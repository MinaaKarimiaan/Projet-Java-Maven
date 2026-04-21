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

package fr.uga.miashs.dciss.chatservice.server;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import fr.uga.miashs.dciss.chatservice.common.Packet;

public class ServerPacketProcessor implements PacketProcessor {
	private final static Logger LOG = Logger.getLogger(ServerPacketProcessor.class.getName());
	private ServerMsg server;

	public ServerPacketProcessor(ServerMsg s) {
		this.server = s;
	}

	@Override
	public void process(Packet p) {
	    ByteBuffer buf = ByteBuffer.wrap(p.data);
	    byte type = buf.get();

	    if (type == 1) {
	        createGroup(p.srcId, buf);
	    } else if (type == 2) {
	        addMember(p.srcId, buf);
	    } else if (type == 3) {
	        removeMember(p.srcId, buf);
	    } else if (type == 4) {
	        removeGroup(p.srcId, buf);
	    } else {
	        LOG.warning("Server message of type=" + type + " not handled");
	    }
	}

	public void createGroup(int ownerId, ByteBuffer data) {
	    int nb = data.getInt();
	    GroupMsg g = server.createGroup(ownerId);
	    for (int i = 0; i < nb; i++) {
	        g.addMember(server.getUser(data.getInt()));
	    }

	    // Notification au owner
	    UserMsg owner = server.getUser(ownerId);
	    if (owner != null) {
	        String msg = "Groupe cree avec succes. ID = " + g.getId();
	        owner.process(new Packet(0, ownerId, msg.getBytes()));
	    }
	}

	public void addMember(int requesterId, ByteBuffer data) {
	    int groupId = data.getInt();
	    int userId = data.getInt();
	    GroupMsg g = server.getGroup(groupId);
	    UserMsg u = server.getUser(userId);

	    UserMsg requester = server.getUser(requesterId);
	    if (requester == null) return;

	    if (g != null && u != null) {
	        g.addMember(u);
	        // Notification au demandeur
	        String msg = "Utilisateur " + userId + " ajoute au groupe " + groupId;
	        requester.process(new Packet(0, requesterId, msg.getBytes()));
	    } else {
	        // Notification erreur
	        String msg = "Erreur : utilisateur ou groupe introuvable";
	        requester.process(new Packet(0, requesterId, msg.getBytes()));
	    }
	}

	public void removeMember(int requesterId, ByteBuffer data) {
	    int groupId = data.getInt();
	    int userId = data.getInt();
	    GroupMsg g = server.getGroup(groupId);
	    UserMsg u = server.getUser(userId);

	    UserMsg requester = server.getUser(requesterId);
	    if (requester == null) return;

	    if (g != null && u != null) {
	        boolean removed = g.removeMember(u);
	        if (removed) {
	            // Notification au demandeur
	            String msg = "Utilisateur " + userId + " supprime du groupe " + groupId;
	            requester.process(new Packet(0, requesterId, msg.getBytes()));
	        } else {
	            // Erreur : owner ne peut pas être supprimé
	            String msg = "Erreur : impossible de supprimer le owner du groupe";
	            requester.process(new Packet(0, requesterId, msg.getBytes()));
	        }
	    } else {
	        String msg = "Erreur : utilisateur ou groupe introuvable";
	        requester.process(new Packet(0, requesterId, msg.getBytes()));
	    }
	}

	public void removeGroup(int requesterId, ByteBuffer data) {
	    int groupId = data.getInt();
	    GroupMsg g = server.getGroup(groupId);

	    UserMsg requester = server.getUser(requesterId);
	    if (requester == null) return;

	    if (g != null) {
	        server.removeGroup(groupId);
	        // Notification au demandeur
	        String msg = "Groupe " + groupId + " supprime avec succes";
	        requester.process(new Packet(0, requesterId, msg.getBytes()));
	    } else {
	        String msg = "Erreur : groupe introuvable";
	        requester.process(new Packet(0, requesterId, msg.getBytes()));
	    }
	}

}
