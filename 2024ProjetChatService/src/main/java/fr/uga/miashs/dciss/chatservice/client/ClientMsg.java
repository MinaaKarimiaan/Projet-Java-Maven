/*
 * Copyright (c) 2024.  Jerome David. Univ. Grenoble Alpes.
 * This file is part of DcissChatService.
 *
 * DcissChatService is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * DcissChatService is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package fr.uga.miashs.dciss.chatservice.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.uga.miashs.dciss.chatservice.client.persistence.ClientDatabase;
import fr.uga.miashs.dciss.chatservice.client.persistence.HistoriqueRepository;
import fr.uga.miashs.dciss.chatservice.client.persistence.MessageRecord;
import fr.uga.miashs.dciss.chatservice.common.Packet;
import fr.uga.miashs.dciss.chatservice.client.persistence.ClientDatabase;

/**
 * Manages the connection to a ServerMsg. Method startSession() is used to
 * establish the connection. Then messages can be send by a call to sendPacket.
 * The reception is done asynchronously (internally by the method receiveLoop())
 * and the reception of a message is notified to MessagesListeners. To register
 * a MessageListener, the method addMessageListener has to be called. Session
 * are closed thanks to the method closeSession().
 */
public class ClientMsg {

	private static final Pattern MEME_PREFIX = Pattern.compile("^\\[([^\\]]+)]\\s*(.*)$", Pattern.DOTALL);

	private String serverAddress;
	private int serverPort;

	private Socket s;
	private DataOutputStream dos;
	private DataInputStream dis;

	private int identifier;
	private String nickname;
	private String avatar;
	private ClientDatabase database;
	private HistoriqueRepository historiqueRepository;

	private List<MessageListener> mListeners;
	private List<ConnectionListener> cListeners;

	/**
	 * Create a client with an existing id, that will connect to the server at the
	 * given address and port
	 * 
	 * @param id      The client id
	 * @param address The server address or hostname
	 * @param port    The port number
	 */
	public ClientMsg(int id, String address, int port) {
		if (id < 0)
			throw new IllegalArgumentException("id must not be less than 0");
		if (port <= 0)
			throw new IllegalArgumentException("Server port must be greater than 0");
		serverAddress = address;
		serverPort = port;
		identifier = id;
		nickname = "User";
		avatar = AvatarCatalog.get(0);
		mListeners = new ArrayList<>();
		cListeners = new ArrayList<>();
	}

	/**
	 * Create a client without id, the server will provide an id during the the
	 * session start
	 * 
	 * @param address The server address or hostname
	 * @param port    The port number
	 */
	public ClientMsg(String address, int port) {
		this(0, address, port);
	}

	/**
	 * Register a MessageListener to the client. It will be notified each time a
	 * message is received.
	 * 
	 * @param l
	 */
	public void addMessageListener(MessageListener l) {
		if (l != null)
			mListeners.add(l);
	}

	protected void notifyMessageListeners(Packet p) {
		mListeners.forEach(x -> x.messageReceived(p));
	}
	
	/**
	 * Register a ConnectionListener to the client. It will be notified if the connection  start or ends.
	 * 
	 * @param l
	 */
	public void addConnectionListener(ConnectionListener l) {
		if (l != null)
			cListeners.add(l);
	}

	protected void notifyConnectionListeners(boolean active) {
		cListeners.forEach(x -> x.connectionEvent(active));
	}

	public int getIdentifier() {
		return identifier;
	}

	public void setProfile(String nickname, int avatarIndex) {
		this.nickname = sanitizeNickname(nickname);
		this.avatar = AvatarCatalog.get(avatarIndex);
	}

	public String getNickname() {
		return nickname;
	}

	public String getAvatar() {
		return avatar;
	}

	public void startSession() throws UnknownHostException {
		if (s == null || s.isClosed()) {
			try {
				s = new Socket(serverAddress, serverPort);
				dos = new DataOutputStream(s.getOutputStream());
				dis = new DataInputStream(s.getInputStream());
				dos.writeInt(identifier);
				dos.flush();
				if (identifier == 0) {
					identifier = dis.readInt();
				}
				// start the receive loop
				initializeDatabase();
				new Thread(() -> receiveLoop()).start();
				notifyConnectionListeners(true);
			} catch (IOException e) {
				e.printStackTrace();
				// error, close session
				closeSession();
			} catch (SQLException e) {
				e.printStackTrace();
				closeSession();
			}
		}
	}

	private void initializeDatabase() throws SQLException {
		if (database == null) {
			database = new ClientDatabase("target/client-history-" + identifier);
			database.initSchema();
			historiqueRepository = new HistoriqueRepository(database);
			historiqueRepository.setOnMessageReadListener((destId, msgId) -> {
				try {
					notifyMessageReadToServer(destId, msgId);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	public void sendPacket(int destId, byte[] data) {
		try {
			synchronized (dos) {
				dos.writeInt(destId);
				dos.writeInt(data.length);
				dos.write(data);
				dos.flush();
			}
			persistPacket(identifier, destId, data, "OUT");
		} catch (IOException e) {
			System.err.println("Connexion perdue avec le serveur");
			closeSession();
		}
	}

	public void deleteGroup(int groupId) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);
			dos.writeByte(5);
			dos.writeInt(groupId);
			dos.flush();
			sendPacket(0, bos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void receiveLoop() {
	    try {
	        while (s != null && !s.isClosed()) {
	            // 1. Lecture du paquet depuis le réseau
	            int sender = dis.readInt();
	            int dest = dis.readInt();
	            int length = dis.readInt();
	            byte[] data = new byte[length];
	            dis.readFully(data);

	            // 2. Sauvegarde en base de données
	            try {
	                persistPacket(sender, dest, data, "IN");
	            } catch (Exception e) {
	                System.err.println("Erreur persistance (ignorée): " + e.getMessage());
	            }

	            // 3. Vérifier le type de paquet
	            ByteBuffer buf = ByteBuffer.wrap(data);
	            byte type = buf.get();

	            if (sender == 0) {
	                // 4a. Notification du serveur (ex: groupe créé)
	                System.out.println("Notification serveur : " + new String(data));
	            } else if (type == 7) {
	                // 4b. Réception d'un fichier
	                receiveFile(buf);
	            } else {
	                // 4c. Message normal texte
	                notifyMessageListeners(new Packet(sender, dest, data));
	            }
	        }
	    } catch (IOException e) {
	        // connexion fermée
	    }
	    closeSession();
	}

	public void closeSession() {
		try {
			if (s != null)
				s.close();
		} catch (IOException e) {
		}
		s = null;
		if (database != null) {
			database.close();
			database = null;
			historiqueRepository = null;
		}
		notifyConnectionListeners(false);
	}

	private String buildOutgoingMessage(String input) {
		Matcher matcher = MEME_PREFIX.matcher(input);
		String content = input;
		if (!matcher.matches()) {
			return prependProfileHeader(content);
		}
		String memeName = matcher.group(1).trim();
		String memeContent = meme.get(memeName);
		if (memeContent == null) {
			System.out.println("Meme inconnu : " + memeName + ". Tapez \\memes pour voir la liste.");
			return null;
		}
		String text = matcher.group(2);
		if (text == null || text.isBlank()) {
			content = memeContent;
		} else {
			content = memeContent + System.lineSeparator() + text;
		}
		return prependProfileHeader(content);
	}

	private String prependProfileHeader(String content) {
		return avatar + " " + nickname + System.lineSeparator() + content;
	}

	private static String sanitizeNickname(String rawNickname) {
		if (rawNickname == null) {
			return "User";
		}
		String normalized = rawNickname.replaceAll("\\R+", " ").trim();
		if (normalized.isEmpty()) {
			return "User";
		}
		if (normalized.length() > 24) {
			return normalized.substring(0, 24);
		}
		return normalized;
	}

	private static int promptAvatarSelection(Scanner sc) {
		while (true) {
			System.out.println(AvatarCatalog.previewAll());
			System.out.print("Entrez le numero de l'avatar (0-9) : ");
			String line = sc.nextLine().trim();
			try {
				int index = Integer.parseInt(line);
				if (AvatarCatalog.isValidIndex(index)) {
					return index;
				}
			} catch (NumberFormatException e) {
			}
			System.out.println("Choix invalide. Reessayez.");
		}
	}

	private static String promptNickname(Scanner sc, int identifier) {
		System.out.print("Entrez votre pseudo : ");
		String entered = sanitizeNickname(sc.nextLine());
		if ("User".equals(entered)) {
			return "User-" + identifier;
		}
		return entered;
	}

	private void persistPacket(int srcId, int destId, byte[] data, String direction) {
		if (historiqueRepository == null || isTechnicalPacket(destId, data)) {
			return;
		}
		try {
			historiqueRepository.sauvegarderMessage(srcId, destId, data, direction);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private boolean isTechnicalPacket(int destId, byte[] data) {
		return destId == 0;
	}

	public void sendAddMember(int groupId, int userId) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);

		try {
			dos.writeByte(2);
			dos.writeInt(groupId);
			dos.writeInt(userId);
			byte[] data = baos.toByteArray();
			sendPacket(0, data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRemoveMember(int groupId, int userId) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);

		try {
			dos.writeByte(3);
			dos.writeInt(groupId);
			dos.writeInt(userId);
			byte[] data = baos.toByteArray();
			sendPacket(0, data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRemoveGroup(int groupId) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);

		try {
			dos.writeByte(4);
			dos.writeInt(groupId);
			byte[] data = baos.toByteArray();
			sendPacket(0, data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendCreateGroup(List<Integer> memberIds) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);

		try {
			dos.writeByte(1);
			dos.writeInt(memberIds.size());
			for (int id : memberIds) {
				dos.writeInt(id);
			}
			byte[] data = baos.toByteArray();
			sendPacket(0, data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendFile(int destId, File file) throws IOException {
		byte[] fileBytes = Files.readAllBytes(file.toPath());
		byte[] nameBytes = file.getName().getBytes(StandardCharsets.UTF_8);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream localDos = new DataOutputStream(bos);

		localDos.writeByte(7);
		localDos.writeInt(nameBytes.length);
		localDos.write(nameBytes);
		localDos.writeInt(fileBytes.length);
		localDos.write(fileBytes);
		localDos.flush();
		sendPacket(destId, bos.toByteArray());
	}

	private void receiveFile(ByteBuffer buf) {
	    try {
	        // lire le nom du fichier
	        int nameLength = buf.getInt();
	        byte[] nameBytes = new byte[nameLength];
	        buf.get(nameBytes);
	        String fileName = new String(nameBytes, StandardCharsets.UTF_8);

	        // lire le contenu du fichier
	        int fileLength = buf.getInt();
	        byte[] fileBytes = new byte[fileLength];
	        buf.get(fileBytes);

	        // sauvegarder le fichier
	        File output = new File("received_" + fileName);
	        Files.write(output.toPath(), fileBytes);
	        System.out.println("OK Fichier reçu : " + fileName);

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	private void notifyMessageReadToServer(int destId, long messageId) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		dos.writeByte(8); // type 8: notification de lecture
		dos.writeInt(destId);
		dos.writeLong(messageId);
		dos.flush();
		sendPacket(0, bos.toByteArray());
	}
	
	public List<MessageRecord> getHistoriqueConversation(int targetId, int limit, int offset) throws SQLException {
	    if (historiqueRepository == null) throw new SQLException("Base de données non initialisée");
	    return historiqueRepository.recupererHistoriqueConversation(identifier, targetId, limit, offset);
	}

	public List<MessageRecord> getHistoriqueGroupe(int groupeId, int limit, int offset) throws SQLException {
	    if (historiqueRepository == null) throw new SQLException("Base de données non initialisée");
	    return historiqueRepository.recupererHistoriqueGroupe(groupeId, limit, offset);
	}
	
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		Scanner sc = new Scanner(System.in);
		System.out.print("Entrez votre ID (0 pour nouveau client) : ");
		int id = Integer.parseInt(sc.nextLine());
		ClientMsg c = new ClientMsg(id, "localhost", 1666);

		c.addConnectionListener(active -> {
			if (!active)
				System.exit(0);
		});

		c.startSession();
		c.addMessageListener(p -> {
			System.out.println("\n----------------------------------------");
		    if (p.data.length > 0 && p.data[0] == 7) {
		        c.receiveFile(ByteBuffer.wrap(p.data).position(1)); // skip le byte type
		    } else if (p.data.length > 0 && p.data[0] == 8) {
		        System.out.println("\n[Message lu par " + p.srcId + "]");
		    } else {
		        System.out.println("\n[" + p.srcId + "] " + new String(p.data, StandardCharsets.UTF_8));
		    }
		    System.out.println("----------------------------------------");
		    System.out.println("Entrez une commande ou l'ID du destinataire :");
		});
		
		System.out.println("Vous êtes : " + c.getIdentifier());
		int avatarIndex = promptAvatarSelection(sc);
		String nickname = promptNickname(sc, c.getIdentifier());
		c.setProfile(nickname, avatarIndex);
		System.out.println("Profil actif : " + c.getAvatar() + " " + c.getNickname());
		System.out.println("Commandes : C (creategroup), A (addmember), R (removemember), D (deletegroup), S (sendfile), H (historique), M (memes), \\meme <nom>, Q (quit)");
		System.out.println("Astuce : tapez [nom] au debut du message pour envoyer le meme avant le texte.");

		String lu = null;
		while (!"Q".equals(lu)) {
			try {
				System.out.println("Entrez une commande ou l'ID du destinataire :");
				lu = sc.nextLine();

				if ("M".equals(lu)) {
					System.out.println(meme.previewAll());
					continue;
				}
				if (lu.startsWith("M ")) {
					String memeName = lu.substring("M ".length()).trim();
					String preview = meme.preview(memeName);
					if (preview == null) {
						System.out.println("Meme inconnu : " + memeName);
					} else {
						System.out.println(preview);
					}
					continue;
				}

				if (lu.equalsIgnoreCase("C")) {
					List<Integer> members = new ArrayList<>();
					members.add(2);
					members.add(3);
					c.sendCreateGroup(members);
				} else if (lu.equalsIgnoreCase("A")) {
					c.sendAddMember(-1, 3);
				} else if (lu.equalsIgnoreCase("R")) {
					c.sendRemoveMember(-1, 3);
				} else if (lu.equalsIgnoreCase("D")) {
					c.sendRemoveGroup(-1);
				} else if (lu.equalsIgnoreCase("S")) {
					System.out.println("ID du destinataire ? ");
					int dest = Integer.parseInt(sc.nextLine());
					System.out.println("Chemin du fichier ? ");
					String path = sc.nextLine();
					File file = new File(path);
					if (file.exists()) {
						c.sendFile(dest, file);
						System.out.println("Fichier envoyé !");
					} else {
						System.out.println("Fichier introuvable !");
					}
				} else if (lu.equalsIgnoreCase("H")) {
				    System.out.println("Historique avec qui ? (ID utilisateur ou groupe) ");
				    int targetId = Integer.parseInt(sc.nextLine());
				    System.out.println("Nombre de messages ? (ex: 20) ");
				    int limit = Integer.parseInt(sc.nextLine());
				    
				    try {
				        List<MessageRecord> historique;
				        
				        if (targetId < 0) {
				            // historique d'un groupe
				            historique = c.getHistoriqueGroupe(targetId, limit, 0);
				        } else {
				            // historique avec un utilisateur
				            historique = c.getHistoriqueConversation(targetId, limit, 0);
				        }
				        
				        if (historique.isEmpty()) {
				            System.out.println("Aucun message trouvé !");
				        } else {
				            System.out.println("=== Historique ===");
				            for (MessageRecord msg : historique) {
				                System.out.println(
				                    msg.getHorodatage() + " | " +
				                    msg.getSrcId() + " → " +
				                    msg.getDestId() + " : " +
				                    msg.getContenu()
				                );
				            }
				        }
				    } catch (SQLException e) {
				        System.out.println("Erreur base de données : " + e.getMessage());
				    }
				} else {
					int dest = Integer.parseInt(lu);
					System.out.println("Votre message ? ");
					String message = sc.nextLine();
					if ("M".equals(message)) {
						System.out.println(meme.previewAll());
						continue;
					}
					if (message.startsWith("M ")) {
						String memeName = message.substring("M ".length()).trim();
						String preview = meme.preview(memeName);
						if (preview == null) {
							System.out.println("Meme inconnu : " + memeName);
						} else {
							System.out.println(preview);
						}
						continue;
					}
					String outgoing = c.buildOutgoingMessage(message);
					if (outgoing != null) {
						c.sendPacket(dest, outgoing.getBytes(StandardCharsets.UTF_8));
					}
				} 
			} catch (InputMismatchException | NumberFormatException e) {
				System.out.println("Mauvais format");
			}
		}
		sc.close();
		c.closeSession();
	}
}
