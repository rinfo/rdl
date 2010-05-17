package se.lagrummet.rinfo.store.depot;

import java.io.InputStream;
import java.io.IOException;

import java.net.URLConnection;
import java.net.FileNameMap;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;

import org.apache.commons.configuration.ConfigurationUtils;


public class DefaultPathHandler implements PathHandler {

    static final String DEFAULT_MIME_TYPES_PATH = "mime.types";
    //static final String DEFAULT_NAMED_MEDIA_TYPES_PATH = "named-media-types.properties";

    static final Map<String, String> DEFAULT_NAMED_MEDIA_TYPES =
            new HashMap<String, String>();
    static {
        DEFAULT_NAMED_MEDIA_TYPES.put("atom", "application/atom+xml");
        DEFAULT_NAMED_MEDIA_TYPES.put("feed", "application/atom+xml;type=feed");
        DEFAULT_NAMED_MEDIA_TYPES.put("entry", "application/atom+xml;type=entry");
        DEFAULT_NAMED_MEDIA_TYPES.put("rdf", "application/rdf+xml");
        DEFAULT_NAMED_MEDIA_TYPES.put("html", "text/html");
        DEFAULT_NAMED_MEDIA_TYPES.put("xhtml", "application/xhtml+xml");
        DEFAULT_NAMED_MEDIA_TYPES.put("pdf", "application/pdf");
    };

    private FileNameMap fileNameMap;

    private TreeBidiMap namedMediaTypes = new TreeBidiMap();

    static final Pattern URI_PATTERN = Pattern.compile(
            "(/([^/]+)\\S*?)(?:/([^/,]+)(?:,([a-z]{2}))?)?");

    public DefaultPathHandler() {
        try {
            fileNameMap = createMimeTypesMap(DEFAULT_MIME_TYPES_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // TODO: parse from DEFAULT_NAMED_MEDIA_TYPES_PATH
        setNamedMediaTypes(DEFAULT_NAMED_MEDIA_TYPES);
    }

    public FileNameMap getFileNameMap() {
        return fileNameMap;
    }

    public void setFileNameMap(FileNameMap fileNameMap) {
        this.fileNameMap = fileNameMap;
    }

    public Map<String, String> getNamedMediaTypes() {
        return namedMediaTypes;
    }

    /**
     * Makes a <em>copy</em> of the provided value to create a BidiMap.
     */
    public void setNamedMediaTypes(Map<String, String> namedMediaTypes) {
        this.namedMediaTypes = new TreeBidiMap(namedMediaTypes);
    }

    public ParsedPath parseUriPath(String uriPath) {
        Matcher matcher = URI_PATTERN.matcher(uriPath);
        if (!matcher.matches()) {
            return null;
        }
        String collection;
        String depotUriPath;
        String mediaHint = null;
        String lang = null;
        boolean asDir = false;

        depotUriPath = matcher.group(1);
        collection = matcher.group(2);
        String lastSegmentNoLang = matcher.group(3);
        lang = matcher.group(4);

        if (lastSegmentNoLang != null &&
                namedMediaTypes.containsKey(lastSegmentNoLang)) {
            mediaHint = lastSegmentNoLang;
        } else {
            // last segment was no media name, thus part of uri path
            if (lastSegmentNoLang!=null && !lastSegmentNoLang.equals(""))
                depotUriPath += "/" + lastSegmentNoLang;
        }
        if (depotUriPath.endsWith("/")) {
            asDir = true;
            depotUriPath = depotUriPath.substring(0, depotUriPath.length()-1);
        }
        return new ParsedPath(collection, depotUriPath, asDir, mediaHint, lang);
    }

    public String makeNegotiatedUriPath(String entryUriPath, String mediaType) {
        return makeNegotiatedUriPath(entryUriPath, mediaType, null);
    }

    public String makeNegotiatedUriPath(String entryUriPath, String mediaType,
            String lang) {
        String mediaHint = hintForMediaType(mediaType);
        String uri = entryUriPath+"/"+mediaHint;
        if (lang!=null && !lang.equals("")) {
            uri += ","+lang;
        }
        return uri;
    }

    public String mediaTypeForHint(String mediaHint) {
        String mediaType = (String) namedMediaTypes.get(mediaHint);
        if (mediaType == null) {
            throw new UnknownMediaTypeException(
                    "Found no media type for unknown media hint: "+mediaHint);
        }
        return mediaType;
    }

    public String hintForMediaType(String mediaType) {
        String mediaName = (String) namedMediaTypes.getKey(mediaType);
        if (mediaName == null) {
            throw new UnknownMediaTypeException(
                    "Found no media hint for unknown media type: "+mediaType);
        }
        return mediaName;
    }

    public String computeMediaType(String path) {
        String mtype = fileNameMap.getContentTypeFor(path);
        if (mtype != null) {
            return mtype;
        } else {
            // TODO:? really fall back to namedMediaTypes?
            int dotIndex = path.lastIndexOf('.');
            if (dotIndex == -1)
                return null;
            String ext = path.substring(dotIndex+1);
            try {
                return mediaTypeForHint(ext);
            } catch (UnknownMediaTypeException e) {
                ;
            }
        }
        return null;
    }

    public FileNameMap createMimeTypesMap(String mimeTypesPath) throws IOException {
        MimeTypesMap mimeTypesMap = new MimeTypesMap();
        InputStream ins =
            ConfigurationUtils.locate(mimeTypesPath).openStream();
        try {
            mimeTypesMap.parse(ins);
        } finally {
            ins.close();
        }
        return mimeTypesMap;
    }
}
