package se.lagrummet.rinfo.base.feed;

import java.util.Date;

/**
 * Created by christian on 2015-05-27.
 */
public interface FeedXmlBuilder {
    void write(Feed feed, Taganizer taganizer);

    interface Taganizer {
        FeedXmlBuilder.Taganizer setContent(String content);
        FeedXmlBuilder.Taganizer setContent(Date content);
        FeedXmlBuilder.Taganizer setAttribute(String name, String value);
        FeedXmlBuilder.Taganizer setAttribute(String name, Date value);
        Taganizer createChild(String name);

        Taganizer createChildAndSetContent(String tagName, String content);
        Taganizer createChildAndSetContent(String tagName, Date content);
    }

}
