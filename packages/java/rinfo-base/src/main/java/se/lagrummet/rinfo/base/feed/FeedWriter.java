package se.lagrummet.rinfo.base.feed;

import java.util.Date;

/**
 * Created by christian on 2015-05-27.
 */
public interface FeedWriter {

    void write(Feed feed, Writer writer);

    interface Writer {
        Writer setContent(String content);
        Writer setContent(Date content);
        Writer setAttribute(String name, String value);
        Writer setAttribute(String name, Date value);
        Writer createChild(String name);

        Writer createChildAndSetContent(String tagName, String content);
        Writer createChildAndSetContent(String tagName, Date content);
    }

}
