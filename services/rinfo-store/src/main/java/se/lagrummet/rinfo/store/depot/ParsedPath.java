package se.lagrummet.rinfo.store.depot;

import org.apache.commons.lang.ObjectUtils;


public class ParsedPath {

    private String collection;
    public String getCollection() { return collection; }

    private String depotUriPath;
    public String getDepotUriPath() { return depotUriPath; }

    private String mediaHint;
    public String getMediaHint() { return mediaHint; }

    private String lang;
    public String getLang() { return lang; }

    private boolean asDir;
    public boolean getAsDir() { return asDir; }

    public ParsedPath(String collection, String depotUriPath) {
        this(collection, depotUriPath, false);
    }

    public ParsedPath(String collection, String depotUriPath, boolean asDir) {
        this(collection, depotUriPath, asDir, null, null);
    }

    public ParsedPath(String collection, String depotUriPath, boolean asDir,
            String mediaHint) {
        this(collection, depotUriPath, asDir, mediaHint, null);
    }

    public ParsedPath(String collection, String depotUriPath, boolean asDir,
            String mediaHint,String lang) {
        this.collection = collection;
        this.depotUriPath = depotUriPath;
        this.mediaHint = mediaHint;
        this.lang = lang;
        this.asDir = asDir;
    }

    public boolean equals(Object otherObj) {
        if (!(otherObj instanceof ParsedPath)) {
            return false;
        }
        ParsedPath other = (ParsedPath) otherObj;

        return (
                ObjectUtils.equals(collection, other.collection) &&
                ObjectUtils.equals(depotUriPath, other.depotUriPath) &&
                asDir == other.asDir &&
                ObjectUtils.equals(mediaHint, other.mediaHint) &&
                ObjectUtils.equals(lang, other.lang)
            );
    }

    public String toString() {
        return "ParsedPath("+collection +
                ", "+depotUriPath +
                ", "+asDir +
                ", "+mediaHint +
                ", "+lang+")";
    }

}
