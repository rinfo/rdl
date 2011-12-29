package se.lagrummet.rinfo.main.storage

public class StorageCredentials {

    private CollectorSource source;
    private boolean admin;

    public StorageCredentials(source, admin) {
        this.admin = admin;
        this.source = source;
    }

    public boolean isAdmin() { return admin; }

    public CollectorSource getSource() { return source; }

}
