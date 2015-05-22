package se.lagrummet.rinfo.base.feed.type;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by christian on 5/21/15.
 */
public class DocumentUrl extends CommonUrl {

    public static DocumentUrl create(URL feedUrl) {
        return new DocumentUrl(feedUrl);
    }

    public static DocumentUrl parse(String feedUrl) throws MalformedURLException {
        return new DocumentUrl(new URL(feedUrl));
    }

    public static DocumentUrl parse(URL base, String feedUrl) throws MalformedURLException {
        if (feedUrl.startsWith("http://") || feedUrl.startsWith("https://"))
            return new DocumentUrl(new URL(feedUrl));
        return new DocumentUrl(new URL(base.toString()+"/"+feedUrl));
    }

    private DocumentUrl(URL feedUrl) {
        super(feedUrl);
    }
}
