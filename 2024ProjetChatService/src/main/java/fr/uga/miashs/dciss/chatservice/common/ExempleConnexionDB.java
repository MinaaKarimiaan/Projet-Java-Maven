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

package fr.uga.miashs.dciss.chatservice.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class ExempleConnexionDB {

	private static final String URL = "jdbc:derby:target/sample;create=true";
	private static final String CREATE_TABLE_SQL = "CREATE TABLE MsgUser (id INT PRIMARY KEY, nickname VARCHAR(20))";
	private static final String INSERT_SQL = "INSERT INTO MsgUser VALUES (?, ?)";
	private static final String SELECT_SQL = "SELECT id, nickname FROM MsgUser";

	public static void main(String[] args) {
		try (Connection cnx = DriverManager.getConnection(URL)) {
			createTableIfNeeded(cnx);
			insertUser(cnx, 35, "titi");
			listUsers(cnx);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void createTableIfNeeded(Connection cnx) throws SQLException {
		try (Statement statement = cnx.createStatement()) {
			statement.executeUpdate(CREATE_TABLE_SQL);
		} catch (SQLException e) {
			if (!"X0Y32".equals(e.getSQLState())) {
				throw e;
			}
		}
	}

	private static void insertUser(Connection cnx, int id, String nickname) throws SQLException {
		try (PreparedStatement pstmt = cnx.prepareStatement(INSERT_SQL)) {
			pstmt.setInt(1, id);
			pstmt.setString(2, nickname);
			pstmt.executeUpdate();
		}
	}

	private static void listUsers(Connection cnx) throws SQLException {
		try (Statement statement = cnx.createStatement(); ResultSet resultSet = statement.executeQuery(SELECT_SQL)) {
			while (resultSet.next()) {
				System.out.println(resultSet.getInt("id") + " - " + resultSet.getString("nickname"));
			}
		}

	}

}
