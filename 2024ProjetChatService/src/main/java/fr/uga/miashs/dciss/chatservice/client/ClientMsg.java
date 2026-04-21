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
import java.sql.SQLException;
import java.util.*;

import fr.uga.miashs.dciss.chatservice.common.Packet;
import fr.uga.miashs.dciss.chatservice.client.persistence.ClientDatabase;
import fr.uga.miashs.dciss.chatservice.client.persistence.HistoriqueRepository;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/**
 * Manages the connection to a ServerMsg. Method startSession() is used to
 * establish the connection. Then messages can be send by a call to sendPacket.
 * The reception is done asynchronously (internally by the method receiveLoop())
 * and the reception of a message is notified to MessagesListeners. To register
 * a MessageListener, the method addMessageListener has to be called. Session
 * are closed thanks to the method closeSession().
 */
public class ClientMsg {

	private String serverAddress;
	private int serverPort;

	private Socket s;
	private DataOutputStream dos;
	private DataInputStream dis;

	private int identifier;
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

	/**
	 * Method to be called to establish the connection.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
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
				initializeDatabase();
				// start the receive loop
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
		}
	}

	/**
	 * Send a packet to the specified destination (etiher a userId or groupId)
	 * 
	 * @param destId the destinatiion id
	 * @param data   the data to be sent
	 */
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
			// error, connection closed
			closeSession();
		}
		
	}

	/**
	 * Start the receive loop. Has to be called only once.
	 */
	private void receiveLoop() {
		try {
			while (s != null && !s.isClosed()) {

				int sender = dis.readInt();
				int dest = dis.readInt();
				int length = dis.readInt();
				byte[] data = new byte[length];
				dis.readFully(data);
				persistPacket(sender, dest, data, "IN");
				notifyMessageListeners(new Packet(sender, dest, data));

			}
		} catch (IOException e) {
			// error, connection closed
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
		return destId == 0; // tous les paquets vers le serveur sont techniques
	}
	//envoie des paquet de gestion pour le groupe
	
	public void sendAddMember(int groupId, int userId) {
		
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    // creation d'un tableau ou liste pour ranger mes bytes
	    DataOutputStream dos = new DataOutputStream(baos);
	    
	    try {
	        dos.writeByte(3);      // type du paquet = 3 (ajout membre)
	        dos.writeInt(userId);  // l'utilisateur à ajouter
	        byte[] data = baos.toByteArray();// le tableau comprend en byte(3,unserId)
	        
	        sendPacket(0, data); // groupId est négatif
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	public void sendRemoveMember(int groupId, int userId) {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    DataOutputStream dos = new DataOutputStream(baos);
	    
	    try {
	        dos.writeByte(4);       // type du paquet = 4 (suppression membre)
	        dos.writeInt(groupId);  // ID du groupe (négatif)
	        dos.writeInt(userId);   // ID du user à supprimer
	        byte[] data = baos.toByteArray();
	        
	        sendPacket(0, data); // 0 paquet destinatiner au serveur
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	public void sendRemoveGroup(int groupId) {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    DataOutputStream dos = new DataOutputStream(baos);
	    
	    try {
	        dos.writeByte(5);       // type = 5 (suppression groupe)
	        dos.writeInt(groupId);  // ID du groupe (négatif)
	        byte[] data = baos.toByteArray();
	        
	        sendPacket(0, data); // destination = serveur
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	
	//-----------------------------------------------
	//Envoi fichier
	
	public void sendFile(int destId, File file) throws IOException {
	    byte[] fileBytes = Files.readAllBytes(file.toPath());
	    byte[] nameBytes = file.getName().getBytes(StandardCharsets.UTF_8);

	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    DataOutputStream localDos = new DataOutputStream(bos);

	    dos.writeByte(7);                    // type FILE_MSG
	    dos.writeInt(nameBytes.length);      // taille du nom
	    dos.write(nameBytes);                // nom du fichier
	    dos.writeInt(fileBytes.length);      // taille du fichier
	    dos.write(fileBytes);                // contenu brut

	    dos.flush();
	    sendPacket(destId, bos.toByteArray());
	}
	
	//-----------------------------------------------------

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		ClientMsg c = new ClientMsg("localhost", 1666);

		// add a dummy listener that print the content of message as a string
		c.addMessageListener(p -> System.out.println(p.srcId + " says to " + p.destId + ": " + new String(p.data)));
		
		// add a connection listener that exit application when connection closed
		c.addConnectionListener(active ->  {if (!active) System.exit(0);});

		c.startSession();
		System.out.println("Vous êtes : " + c.getIdentifier());

		// Thread.sleep(5000);

		// l'utilisateur avec id 4 crée un grp avec 1 et 3 dedans (et lui meme)
		if (c.getIdentifier() == 4) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);

			// byte 1 : create group on server
			dos.writeByte(1);

			// nb members
			dos.writeInt(2);
			// list members
			dos.writeInt(1);
			dos.writeInt(3);
			dos.flush();

			c.sendPacket(0, bos.toByteArray());

		}
		
		

		Scanner sc = new Scanner(System.in);
		String lu = null;
		while (!"\\quit".equals(lu)) {
			try {
				System.out.println("A qui voulez vous écrire ? ");
				int dest = Integer.parseInt(sc.nextLine());

				System.out.println("Votre message ? ");
				lu = sc.nextLine();
				c.sendPacket(dest, lu.getBytes());
			} catch (InputMismatchException | NumberFormatException e) {
				System.out.println("Mauvais format");
			}

		}
		sc.close();

		/*
		 * int id =1+(c.getIdentifier()-1) % 2; System.out.println("send to "+id);
		 * c.sendPacket(id, "bonjour".getBytes());
		 * 
		 * 
		 * Thread.sleep(10000);
		 */

		c.closeSession();

	}

}
