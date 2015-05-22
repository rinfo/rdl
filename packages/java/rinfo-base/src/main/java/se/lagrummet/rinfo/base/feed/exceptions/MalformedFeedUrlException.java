package se.lagrummet.rinfo.base.feed.exceptions;

import java.net.MalformedURLException;

/**
 * Created by christian on 5/22/15.
 */
public class MalformedFeedUrlException extends Exception {

    public MalformedFeedUrlException(String content, MalformedURLException e) {
        super(content, e);
    }
}
