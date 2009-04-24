package se.lagrummet.rinfo.store.depot;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;


public class UriPathProcessor {

    public static final String CONF_BASE_KEY = "rinfo.depot.uris.";

    static final Pattern URI_PATTERN = Pattern.compile(
            "(/([^/]+)\\S*?)(?:/([^/,]+)(?:,([a-z]{2}))?)?");

    // TODO: Use URLConnection.getFileNameMap?
    // TODO: put namedMediaTypes in rinfo-depot.properties
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

    public void configure(AbstractConfiguration config)
            throws ConfigurationException {
        Configuration namedMediaConf = config.subset(CONF_BASE_KEY+"namedMediaType");
        if (!namedMediaConf.isEmpty()) {
            Map<String, String> configNamedMediaTypes = new HashMap<String, String>();
            for (Iterator iter = namedMediaConf.getKeys(); iter.hasNext();) {
                String key = (String) iter.next();
                configNamedMediaTypes.put(key, config.getString(key));
            }
            setNamedMediaTypes(configNamedMediaTypes);
        }
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
        String lastSegmentNoLang = matcher.group(3);
        lang = matcher.group(4);

        if (namedMediaTypes.containsKey(lastSegmentNoLang)) {
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

}
