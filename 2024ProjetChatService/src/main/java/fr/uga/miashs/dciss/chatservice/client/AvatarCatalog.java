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

import java.util.List;

public final class AvatarCatalog {

	private static final List<String> AVATARS = List.of(
			"(^_^)",
			"(>_<)",
			"(¬‿¬)",
			"(•‿•)",
			"(╯°□°）╯",
			"(⌐■_■)",
			"(ಥ﹏ಥ)",
			"(ᵔᴥᵔ)",
			"(•̀ᴗ•́)و",
			"(づ｡◕‿‿◕｡)づ");

	private AvatarCatalog() {
	}

	public static boolean isValidIndex(int index) {
		return index >= 0 && index < AVATARS.size();
	}

	public static String get(int index) {
		if (!isValidIndex(index)) {
			throw new IllegalArgumentException("Invalid avatar index: " + index);
		}
		return AVATARS.get(index);
	}

	public static String previewAll() {
		StringBuilder builder = new StringBuilder("Choisissez un avatar :\n");
		for (int i = 0; i < AVATARS.size(); i++) {
			builder.append(i).append(" - ").append(AVATARS.get(i)).append('\n');
		}
		return builder.toString();
	}
}
