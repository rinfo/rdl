package se.lagrummet.rinfo.base.feed;

import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import java.util.Date;

/**
 * Created by christian on 5/21/15.
 */
public interface Feed {

    enum ContentKind { Source, FeedOfFeed, Unknown;

        public static ContentKind parse(String type) {
            if (type==null)
                return Unknown;
            if (type.equalsIgnoreCase("application/xhtml+xml")
                    || type.equalsIgnoreCase("application/pdf")
                    || type.equalsIgnoreCase("application/rdf+xml")
                    || type.equalsIgnoreCase("text/html")
                    )
                return Feed.ContentKind.Source;
            if (type.equalsIgnoreCase("application/atom+xml;type=feed"))
                return Feed.ContentKind.FeedOfFeed;
            return Feed.ContentKind.Unknown;
        }

    }

    Iterable<Entry> getEntries();
    String getId();
    Date getUpdated();

    interface Entry {
        String getId();
        Iterable<Content> getContentList();
    }

    interface Content {
        ContentKind getContentKind();
        Md5Sum getMd5Sum();
        Long getLength();
        DocumentUrl getDocumentUrl();
        String getType();

        ResourceLocator.Resource asResource();
    }

}
