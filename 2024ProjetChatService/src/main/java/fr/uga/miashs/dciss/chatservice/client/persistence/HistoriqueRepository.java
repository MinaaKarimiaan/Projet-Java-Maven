package fr.uga.miashs.dciss.chatservice.client.persistence;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class HistoriqueRepository {

    private final ClientDatabase database;

    public HistoriqueRepository(ClientDatabase database) {
        this.database = database;
    }

    public void sauvegarderMessage(int srcId, int destId, byte[] data, String direction) throws SQLException {
    	if (data == null || data.length == 0) return;
        byte type = data[0];
        if (type == 7) return; // ne pas sauvegarder les fichiers comme du texte
        String contenu = new String(data, StandardCharsets.UTF_8);
        boolean estGroupe = destId < 0;
        sauvegarderMessage(srcId, destId, contenu, estGroupe, direction);
    }

    public void sauvegarderMessage(int srcId, int destId, String contenu, boolean estGroupe, String direction)
            throws SQLException {
        Connection cnx = database.getConnection();
        String sql = "INSERT INTO Messages_Historique (src_id, dest_id, est_groupe, contenu, horodatage, direction) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = cnx.prepareStatement(sql)) {
            pstmt.setInt(1, srcId);
            pstmt.setInt(2, destId);
            pstmt.setBoolean(3, estGroupe);
            pstmt.setString(4, contenu);
            pstmt.setTimestamp(5, Timestamp.from(Instant.now()));
            pstmt.setString(6, direction);
            pstmt.executeUpdate();
        }
    }

    public List<MessageRecord> recupererHistoriqueConversation(int userA, int userB, int limit, int offset)
            throws SQLException {
        Connection cnx = database.getConnection();
        String sql = "SELECT id, src_id, dest_id, est_groupe, contenu, horodatage, direction " +
                "FROM Messages_Historique " +
                "WHERE est_groupe = FALSE AND ((src_id = ? AND dest_id = ?) OR (src_id = ? AND dest_id = ?)) " +
                "ORDER BY horodatage DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (PreparedStatement pstmt = cnx.prepareStatement(sql)) {
            pstmt.setInt(1, userA);
            pstmt.setInt(2, userB);
            pstmt.setInt(3, userB);
            pstmt.setInt(4, userA);
            pstmt.setInt(5, Math.max(offset, 0));
            pstmt.setInt(6, Math.max(limit, 1));
            try (ResultSet rs = pstmt.executeQuery()) {
                return mapMessages(rs);
            }
        }
    }

    public List<MessageRecord> recupererHistoriqueGroupe(int groupeId, int limit, int offset) throws SQLException {
        Connection cnx = database.getConnection();
        String sql = "SELECT id, src_id, dest_id, est_groupe, contenu, horodatage, direction " +
                "FROM Messages_Historique " +
                "WHERE est_groupe = TRUE AND dest_id = ? " +
                "ORDER BY horodatage DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (PreparedStatement pstmt = cnx.prepareStatement(sql)) {
            pstmt.setInt(1, groupeId);
            pstmt.setInt(2, Math.max(offset, 0));
            pstmt.setInt(3, Math.max(limit, 1));
            try (ResultSet rs = pstmt.executeQuery()) {
                return mapMessages(rs);
            }
        }
    }

    public List<MessageRecord> recupererHistorique(int userIdA, Integer userIdB, Integer groupeId, int limit, int offset)
            throws SQLException {
        if (groupeId != null && groupeId != 0) {
            return recupererHistoriqueGroupe(groupeId, limit, offset);
        } else if (userIdB != null && userIdB != 0) {
            return recupererHistoriqueConversation(userIdA, userIdB, limit, offset);
        }
        return new ArrayList<>();
    }

    private List<MessageRecord> mapMessages(ResultSet rs) throws SQLException {
        List<MessageRecord> messages = new ArrayList<>();
        while (rs.next()) {
            messages.add(new MessageRecord(
                    rs.getLong("id"),
                    rs.getInt("src_id"),
                    rs.getInt("dest_id"),
                    rs.getBoolean("est_groupe"),
                    rs.getString("contenu"),
                    rs.getTimestamp("horodatage"),
                    rs.getString("direction")));
        }
        return messages;
    }
}
package fr.uga.miashs.dciss.chatservice.client.persistence;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HistoriqueRepository {

    private final ClientDatabase database;
    private Consumer<Long> onMessageRead;

    public HistoriqueRepository(ClientDatabase database) {
        this.database = database;
    }

    public void setOnMessageReadListener(Consumer<Long> listener) {
        this.onMessageRead = listener;
    }

    public void sauvegarderMessage(int srcId, int destId, byte[] data, String direction) throws SQLException {
        if (data == null || data.length == 0) return;
        byte type = data[0];
        if (type == 7) return;
        String contenu = new String(data, StandardCharsets.UTF_8);
        boolean estGroupe = destId < 0;
        sauvegarderMessage(srcId, destId, contenu, estGroupe, direction);
    }

    public void sauvegarderMessage(int srcId, int destId, String contenu, boolean estGroupe, String direction)
            throws SQLException {
        Connection cnx = database.getConnection();
        String sql = "INSERT INTO Messages_Historique (src_id, dest_id, est_groupe, contenu, horodatage, direction) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = cnx.prepareStatement(sql)) {
            pstmt.setInt(1, srcId);
            pstmt.setInt(2, destId);
            pstmt.setBoolean(3, estGroupe);
            pstmt.setString(4, contenu);
            pstmt.setTimestamp(5, Timestamp.from(Instant.now()));
            pstmt.setString(6, direction);
            pstmt.executeUpdate();
        }
    }

    public List<MessageRecord> recupererHistoriqueConversation(int userA, int userB, int limit, int offset)
            throws SQLException {
        Connection cnx = database.getConnection();
        String sql = "SELECT id, src_id, dest_id, est_groupe, contenu, horodatage, direction " +
                "FROM Messages_Historique " +
                "WHERE est_groupe = FALSE AND ((src_id = ? AND dest_id = ?) OR (src_id = ? AND dest_id = ?)) " +
                "ORDER BY horodatage DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (PreparedStatement pstmt = cnx.prepareStatement(sql)) {
            pstmt.setInt(1, userA);
            pstmt.setInt(2, userB);
            pstmt.setInt(3, userB);
            pstmt.setInt(4, userA);
            pstmt.setInt(5, Math.max(offset, 0));
            pstmt.setInt(6, Math.max(limit, 1));
            try (ResultSet rs = pstmt.executeQuery()) {
                return mapMessages(rs);
            }
        }
    }

    public List<MessageRecord> recupererHistoriqueGroupe(int groupeId, int limit, int offset) throws SQLException {
        Connection cnx = database.getConnection();
        String sql = "SELECT id, src_id, dest_id, est_groupe, contenu, horodatage, direction " +
                "FROM Messages_Historique " +
                "WHERE est_groupe = TRUE AND dest_id = ? " +
                "ORDER BY horodatage DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        try (PreparedStatement pstmt = cnx.prepareStatement(sql)) {
            pstmt.setInt(1, groupeId);
            pstmt.setInt(2, Math.max(offset, 0));
            pstmt.setInt(3, Math.max(limit, 1));
            try (ResultSet rs = pstmt.executeQuery()) {
                return mapMessages(rs);
            }
        }
    }

    public List<MessageRecord> recupererHistorique(int userIdA, Integer userIdB, Integer groupeId, int limit, int offset)
            throws SQLException {
        if (groupeId != null && groupeId != 0) {
            return recupererHistoriqueGroupe(groupeId, limit, offset);
        } else if (userIdB != null && userIdB != 0) {
            return recupererHistoriqueConversation(userIdA, userIdB, limit, offset);
        }
        return new ArrayList<>();
    }

    public void marquerMessageCommeServiceLu(long messageId) throws SQLException {
        Connection cnx = database.getConnection();
        String sql = "UPDATE Messages_Historique SET lue = TRUE, date_lecture = ? WHERE id = ?";
        try (PreparedStatement pstmt = cnx.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.from(Instant.now()));
            pstmt.setLong(2, messageId);
            pstmt.executeUpdate();
            if (onMessageRead != null) {
                onMessageRead.accept(messageId);
            }
        }
    }

    public void marquerMessagesCommeLus(int userIdA, int userIdB) throws SQLException {
        Connection cnx = database.getConnection();
        String sql = "UPDATE Messages_Historique SET lue = TRUE, date_lecture = ? WHERE lue = FALSE AND src_id = ? AND dest_id = ? AND est_groupe = FALSE";
        try (PreparedStatement pstmt = cnx.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.from(Instant.now()));
            pstmt.setInt(2, userIdA);
            pstmt.setInt(3, userIdB);
            pstmt.executeUpdate();
        }
    }

    public boolean estMessageNonLu(long messageId) throws SQLException {
        Connection cnx = database.getConnection();
        String sql = "SELECT lue FROM Messages_Historique WHERE id = ?";
        try (PreparedStatement pstmt = cnx.prepareStatement(sql)) {
            pstmt.setLong(1, messageId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return !rs.getBoolean("lue");
                }
            }
        }
        return false;
    }

    private List<MessageRecord> mapMessages(ResultSet rs) throws SQLException {
        List<MessageRecord> messages = new ArrayList<>();
        while (rs.next()) {
            messages.add(new MessageRecord(
                    rs.getLong("id"),
                    rs.getInt("src_id"),
                    rs.getInt("dest_id"),
                    rs.getBoolean("est_groupe"),
                    rs.getString("contenu"),
                    rs.getTimestamp("horodatage"),
                    rs.getString("direction")));
        }
        return messages;
    }
}
