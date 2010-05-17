package se.lagrummet.rinfo.store.depot;

import java.io.*;
import java.util.*;

import java.net.FileNameMap;


public class MimeTypesMap implements FileNameMap {

    private Map<String, String> extensionMimeTypeMap = new HashMap<String, String>();

    public Map getExtensionMimeTypeMap() { return extensionMimeTypeMap; }

    /**
     * Parses mime.types formatted input, as given in the <a
     * href="http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types"
     * >mime.types</a> file maintained by Roy Fielding for the Apache httpd
     * project.
     */
    public void parse(InputStream mimeTypesInputStream) throws IOException {
        BufferedReader br = new BufferedReader(
            new InputStreamReader(mimeTypesInputStream, "ISO-8859-1"));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length > 1) {
                String mimeType = parts[0];
                for (int i = 1; i < parts.length; i++) {
                    String ext = parts[i];
                    extensionMimeTypeMap.put(ext, mimeType);
                }
            }
        }
    }

    public String getContentTypeFor(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex == -1)
            return null;
        String ext = path.substring(dotIndex+1);
        return extensionMimeTypeMap.get(ext);
    }

}
