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
