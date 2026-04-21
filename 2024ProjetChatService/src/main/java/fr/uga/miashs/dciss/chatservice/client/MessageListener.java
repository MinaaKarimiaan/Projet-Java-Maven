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

import fr.uga.miashs.dciss.chatservice.common.Packet;

public interface MessageListener {

	void messageReceived(Packet p);
	
//------------------------------------
	if (type == 7) { //FILE_MSG
	    int nameLen = dis.readInt();
	    byte[] nameBytes = new byte[nameLen];
	    dis.readFully(nameBytes);
	    String fileName = new String(nameBytes, StandardCharsets.UTF_8);

	    int fileLen = dis.readInt();
	    byte[] fileBytes = new byte[fileLen];
	    dis.readFully(fileBytes);

	    Files.write(Paths.get("received_" + fileName), fileBytes);
	    System.out.println("Fichier reçu : " + fileName);
	}
//---------------------------------------------------

}
