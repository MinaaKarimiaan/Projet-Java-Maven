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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Catalogue des memes ASCII disponibles cote client.
 *
 * Mode d'emploi dans le terminal :
 * - Au demarrage du client, l'utilisateur choisit d'abord un avatar (0-9),
 *   puis saisit un pseudo.
 * - Chaque message envoye est automatiquement formate sur deux lignes :
 *   la premiere contient "avatar + pseudo", la seconde contient le texte.
 * - Tapez \memes pour afficher la liste des memes disponibles.
 * - Tapez \meme <nom> pour afficher un apercu d'un meme sans l'envoyer.
 * - Tapez [nom] au debut du message pour inserer automatiquement le meme
 *   avant votre texte.
 * - Exemple : [chat] bonjour a tous
 *
 * Exemple de message envoye :
 * (⌐■_■) Alice
 * Bonjour !
 */
public final class meme {

	private static final Map<String, String> MEMES;

	static {
		Map<String, String> memes = new LinkedHashMap<>();
		memes.put("cafe",
				"    (  )   (   )  )\n"
						+ "       ) (   )  (  (\n"
						+ "       ( )  (    ) )\n"
						+ "       _____________\n"
						+ "      <_____________> ___\n"
						+ "      |             |/ _ \\\n"
						+ "      |               | | |\n"
						+ "      |               |_| |\n"
						+ "   ___|             |\\___/\n"
						+ "  /    \\___________/    \\\n"
						+ "  \\_____________________/");
		memes.put("ordinateur",
				"   _________________\n"
						+ "  /               /|\n"
						+ " /               / |\n"
						+ "/_______________/  |\n"
						+ "|  ___________  |  |\n"
						+ "| |           | |  |\n"
						+ "| |   >_      | |  |\n"
						+ "| |           | |  |\n"
						+ "| |___________| |  |\n"
						+ "|               |  /\n"
						+ "|_______________| /");
		memes.put("chateau",
				"       |>>>\n"
						+ "       |\n"
						+ "   _  _|_  _\n"
						+ "  |;|_|;|_|;|\n"
						+ "  \\\\.    .  /\n"
						+ "   \\\\:  .  /\n"
						+ "    ||:   |\n"
						+ "    ||_   |\n"
						+ "    ||_._ |");
		memes.put("visage",
				"    ___          ___\n"
						+ "   /   \\        /   \\\n"
						+ "  /     \\______/     \\\n"
						+ "  |                  |\n"
						+ "  |   /~\\      /~\\   |\n"
						+ "  |   \\ /      \\ /   |\n"
						+ "  |        /\\        |\n"
						+ "  \\      \\____/      /\n"
						+ "   \\                /\n"
						+ "    \\______________/");
		memes.put("chat",
				"    _._     _,-'\"\"`-._\n"
						+ "   (,-.`._,'(       |\\`-/|\n"
						+ "       `-.-' \\ )-`( , o o)\n"
						+ "             `-    \\`_`\"'-");
		MEMES = Collections.unmodifiableMap(memes);
	}

	private meme() {
	}

	public static String get(String name) {
		if (name == null) {
			return null;
		}
		return MEMES.get(normalize(name));
	}

	public static boolean exists(String name) {
		return get(name) != null;
	}

	public static Set<String> names() {
		return MEMES.keySet();
	}

	public static String previewAll() {
		StringBuilder builder = new StringBuilder("Memes disponibles :\n");
		MEMES.forEach((name, art) -> builder.append("- ").append(name).append('\n'));
		builder.append("Utilisation : [nom] votre message");
		return builder.toString();
	}

	public static String preview(String name) {
		String art = get(name);
		if (art == null) {
			return null;
		}
		return "[" + normalize(name) + "]\n" + art;
	}

	private static String normalize(String name) {
		return name.trim().toLowerCase(Locale.ROOT);
	}
}
