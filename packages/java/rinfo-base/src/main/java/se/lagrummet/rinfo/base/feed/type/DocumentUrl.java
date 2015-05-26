package se.lagrummet.rinfo.base.feed.type;

import se.lagrummet.rinfo.base.feed.util.Utils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by christian on 5/21/15.
 */
public class DocumentUrl extends CommonUrl {

    public static DocumentUrl create(String feedUrl) {
        if (feedUrl==null)
            throw new NullPointerException("feedUrl is null!");
        return new DocumentUrl(feedUrl);
    }

    public static DocumentUrl parse(URL base, String feedUrl) throws MalformedURLException {
        return new DocumentUrl(Utils.parse(base, feedUrl).toString());
    }

    private DocumentUrl(String feedUrl) {
        super(feedUrl);
    }
}
