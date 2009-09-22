package se.lagrummet.rinfo.store.depot;

import java.util.Map;


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

    ParsedPath parseUriPath(String uriPath);

    String makeNegotiatedUriPath(String entryUriPath, String mediaType);

    String makeNegotiatedUriPath(String entryUriPath, String mediaType,
            String lang);

    String mediaTypeForHint(String mediaHint);

    String hintForMediaType(String mediaType);

    String computeMediaType(String path);

}
