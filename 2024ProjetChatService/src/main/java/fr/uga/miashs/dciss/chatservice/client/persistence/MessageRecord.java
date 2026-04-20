package fr.uga.miashs.dciss.chatservice.client.persistence;

import java.sql.Timestamp;

public class MessageRecord {

    private final long id;
    private final int srcId;
    private final int destId;
    private final boolean groupe;
    private final String contenu;
    private final Timestamp horodatage;
    private final String direction;

    public MessageRecord(long id, int srcId, int destId, boolean groupe, String contenu, Timestamp horodatage, String direction) {
        this.id = id;
        this.srcId = srcId;
        this.destId = destId;
        this.groupe = groupe;
        this.contenu = contenu;
        this.horodatage = horodatage;
        this.direction = direction;
    }

    public long getId() {
        return id;
    }

    public int getSrcId() {
        return srcId;
    }

    public int getDestId() {
        return destId;
    }

    public boolean isGroupe() {
        return groupe;
    }

    public String getContenu() {
        return contenu;
    }

    public Timestamp getHorodatage() {
        return horodatage;
    }

    public String getDirection() {
        return direction;
    }
}
