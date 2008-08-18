package se.lagrummet.rinfo.store.depot;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.codec.digest.DigestUtils;


public class DepotContent {

    private File file;
    public File getFile() { return file; }

    private String depotUriPath;
    public String getDepotUriPath() { return depotUriPath; }

    private String mediaType;
    public String getMediaType() { return mediaType; }

    private String lang;
    public String getLang() { return lang; }

    public DepotContent(File file, String depotUriPath, String mediaType) {
        this(file, depotUriPath, mediaType, null);
    }

    public DepotContent(File file, String depotUriPath, String mediaType,
            String lang) {
        this.file = file;
        this.depotUriPath = depotUriPath;
        this.mediaType = mediaType;
        this.lang = lang;
    }

    public String getMd5Hex() throws IOException {
        return DigestUtils.md5Hex(FileUtils.readFileToByteArray(file));
    }

    public String toString() {
        return "DepotContent(file:"+file.toString() +
                ", depotUriPath:"+depotUriPath +
                ", mediaType:"+mediaType +
                ", lang:"+lang+")";
    }

}
