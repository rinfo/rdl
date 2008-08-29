package se.lagrummet.rinfo.store.depot;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// TODO: Make this configurable/extensible

public class UriPathProcessor {

    static final Pattern URI_PATTERN = Pattern.compile(
            "(/([^/]+)\\S+?)(?:/([^/,]+)(?:,([a-z]{2}))?)?");

    // TODO: Use URLConnection.getFileNameMap?
    static Map<String, String> DEFAULT_NAMED_MEDIA_TYPES =
            new HashMap<String, String>() {{
        put("atom", "application/atom+xml");
        put("feed", "application/atom+xml;type=feed");
        put("entry", "application/atom+xml;type=entry");
        put("rdf", "application/rdf+xml");
        put("html", "text/html");
        put("xhtml", "application/xhtml+xml");
        put("pdf", "application/pdf");
    }};

    private Map<String, String> namedMediaTypes;
    public Map<String, String> getNamedMediaTypes() {
        return namedMediaTypes;
    }

    private Map<String, String> mediaTypeHints;

    public UriPathProcessor() {
        setNamedMediaTypes(DEFAULT_NAMED_MEDIA_TYPES);
    }

    public void setNamedMediaTypes(Map<String, String> namedMediaTypes) {
        this.namedMediaTypes = Collections.unmodifiableMap(namedMediaTypes);
        mediaTypeHints = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : namedMediaTypes.entrySet()) {
            mediaTypeHints.put(entry.getValue(), entry.getKey());
        }
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
        String lastSansLang = matcher.group(3);
        lang = matcher.group(4);

        if (namedMediaTypes.containsKey(lastSansLang)) {
            mediaHint = lastSansLang;
        } else {
            // re-combine
            if (lastSansLang!=null && !lastSansLang.equals(""))
                depotUriPath += "/" + lastSansLang;
        }
        if (depotUriPath.endsWith("/")) {
            asDir = true;
            depotUriPath = depotUriPath.substring(0, depotUriPath.length()-1);
        }
        return new ParsedPath(collection, depotUriPath, asDir, mediaHint, lang);
    }

    public String mediaTypeForHint(String mediaHint) {
        String mediaType = namedMediaTypes.get(mediaHint);
        if (mediaType == null) {
            throw new UnknownMediaTypeException(
                    "Found no media type for unknown media hint: "+mediaHint);
        }
        return mediaType;
    }

    public String hintForMediaType(String mediaType) {
        String mediaHint = mediaTypeHints.get(mediaType);
        if (mediaHint == null) {
            throw new UnknownMediaTypeException(
                    "Found no media hint for unknown media type: "+mediaType);
        }
        return mediaHint;
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

}

