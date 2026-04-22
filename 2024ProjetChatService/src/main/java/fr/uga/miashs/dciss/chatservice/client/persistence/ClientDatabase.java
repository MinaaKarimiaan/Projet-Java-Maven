package fr.uga.miashs.dciss.chatservice.client.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ClientDatabase {

    private static final String TABLE_ALREADY_EXISTS_SQL_STATE = "X0Y32";

    private final String jdbcUrl;
    private Connection connection;

    public ClientDatabase(String databasePath) {
        this.jdbcUrl = "jdbc:derby:" + databasePath + ";create=true";
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(jdbcUrl);
        }
        return connection;
    }

    public synchronized void initSchema() throws SQLException {
        createTableIfMissing(
                "CREATE TABLE Utilisateurs (" +
                        "id INT PRIMARY KEY, " +
                        "pseudo VARCHAR(50)" +
                        ")");

        createTableIfMissing(
                "CREATE TABLE Groupes (" +
                        "id INT PRIMARY KEY, " +
                        "owner_id INT" +
                        ")");

        createTableIfMissing(
                "CREATE TABLE Groupes_Membres (" +
                        "groupe_id INT NOT NULL, " +
                        "user_id INT NOT NULL, " +
                        "PRIMARY KEY (groupe_id, user_id)" +
                        ")");

        createTableIfMissing(
                "CREATE TABLE Messages_Historique (" +
                        "id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                        "src_id INT NOT NULL, " +
                        "dest_id INT NOT NULL, " +
                        "est_groupe BOOLEAN NOT NULL, " +
                        "contenu VARCHAR(4000) NOT NULL, " +
                        "horodatage TIMESTAMP NOT NULL, " +
                        "direction VARCHAR(10) NOT NULL, " +
                        "lue BOOLEAN DEFAULT FALSE, " +
                        "date_lecture TIMESTAMP" +
                        ")");
    }

    private void createTableIfMissing(String sql) throws SQLException {
        try (Statement st = getConnection().createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            if (!TABLE_ALREADY_EXISTS_SQL_STATE.equals(e.getSQLState())) {
                throw e;
            }
        }
    }

    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }
}
