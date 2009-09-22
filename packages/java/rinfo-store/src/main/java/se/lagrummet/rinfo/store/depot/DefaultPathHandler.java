package se.lagrummet.rinfo.store.depot;

import java.net.URLConnection;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.TreeBidiMap;


public class DefaultPathHandler implements PathHandler {

    static final Pattern URI_PATTERN = Pattern.compile(
            "(/([^/]+)\\S*?)(?:/([^/,]+)(?:,([a-z]{2}))?)?");

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

    private TreeBidiMap namedMediaTypes =
            new TreeBidiMap();

    public DefaultPathHandler() {
        setNamedMediaTypes(DEFAULT_NAMED_MEDIA_TYPES);
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
        // FIXME: Knows *very* little! Make configurable..
        // To reconfig: System.setProperty("content.types.user.table",
        // configuredContentTypesPath"), then store the FileNameMap..
        String mtype = URLConnection.getFileNameMap().getContentTypeFor(path);
        // TODO: this is too simple. Unify or only via some fileExtensionUtil..
        if (mtype==null) {
            String[] dotSplit = path.split("\\.");
            try {
                mtype = mediaTypeForHint( dotSplit[dotSplit.length-1] );
            } catch (UnknownMediaTypeException e) {
                ; // pass
            }
        }
        return mtype;
    }

}
