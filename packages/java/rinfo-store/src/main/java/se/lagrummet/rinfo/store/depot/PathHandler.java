package se.lagrummet.rinfo.store.depot;

import java.util.Map;

import java.net.FileNameMap;


public interface PathHandler {

    /**
     * These names are tokens used to denote the media type in the path of a
     * resource, commonly as names or suffixes.
     */
    Map<String, String> getNamedMediaTypes();

    /**
     * This may make a <em>copy</em> of the provided value to create internal
     * bidirectional map(s).
     */
    void setNamedMediaTypes(Map<String, String> namedMediaTypes);

    /**
     * A companion file name map used to compute a media type from the
     * (extension of) a resource name (or path). Used by computeMediaType to
     * provide full read support for getting media types from names. In
     * comparison, namedMediaTypes are intended for specific control of
     * canonical names (and are not explictly tied to the extension of a
     * resource name).
     */
    FileNameMap getFileNameMap();

    void setFileNameMap(FileNameMap fileNameMap);

    ParsedPath parseUriPath(String uriPath);

    String makeNegotiatedUriPath(String entryUriPath, String mediaType);

    String makeNegotiatedUriPath(String entryUriPath, String mediaType,
            String lang);

    String mediaTypeForHint(String mediaHint);

    String hintForMediaType(String mediaType);

    String computeMediaType(String path);

}
