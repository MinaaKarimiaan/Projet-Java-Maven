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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import fr.uga.miashs.dciss.chatservice.common.Packet;

public interface MessageListener {

	byte FILE_MSG = 7;

	void messageReceived(Packet p);

	default boolean isFilePacket(Packet p) {
		return p != null && p.data != null && p.data.length > 0 && p.data[0] == FILE_MSG;
	}

	default ReceivedFile parseFilePacket(Packet p) throws IOException {
		if (!isFilePacket(p)) {
			throw new IOException("Packet is not a file packet");
		}
		try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(p.data))) {
			dis.readByte();
			int nameLen = dis.readInt();
			if (nameLen < 0) {
				throw new IOException("Invalid file name length");
			}
			byte[] nameBytes = new byte[nameLen];
			dis.readFully(nameBytes);
			String fileName = new String(nameBytes, StandardCharsets.UTF_8);

			int fileLen = dis.readInt();
			if (fileLen < 0) {
				throw new IOException("Invalid file content length");
			}
			byte[] fileBytes = new byte[fileLen];
			dis.readFully(fileBytes);
			return new ReceivedFile(fileName, fileBytes);
		}
	}

	default Path saveFilePacket(Packet p) throws IOException {
		return saveFilePacket(p, Paths.get("target", "received-files"));
	}

	default Path saveFilePacket(Packet p, Path directory) throws IOException {
		ReceivedFile received = parseFilePacket(p);
		Files.createDirectories(directory);
		Path output = directory.resolve(received.fileName()).normalize();
		if (!output.startsWith(directory.normalize())) {
			throw new IOException("Refusing to write outside download directory");
		}
		Files.write(output, received.content());
		return output;
	}

	final class ReceivedFile {
		private final String fileName;
		private final byte[] content;

		public ReceivedFile(String fileName, byte[] content) {
			this.fileName = fileName;
			this.content = content;
		}

		public String fileName() {
			return fileName;
		}

		public byte[] content() {
			return content;
		}
	}
}
