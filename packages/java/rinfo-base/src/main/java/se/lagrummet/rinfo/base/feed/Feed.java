package se.lagrummet.rinfo.base.feed;

import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import java.util.Date;

/**
 * Created by christian on 5/21/15.
 */
public interface Feed {



    enum ContentKind { Source, Alternate, FeedOfFeed, Unknown;

        public static ContentKind parse(String type, boolean alternate) {
            if (type==null)
                return Unknown;
            if (type.equalsIgnoreCase("application/xhtml+xml")
                    || type.equalsIgnoreCase("application/pdf")
                    || type.equalsIgnoreCase("application/rdf+xml")
                    || type.equalsIgnoreCase("text/html")
                    )
                return alternate? ContentKind.Alternate : Feed.ContentKind.Source;
            if (type.equalsIgnoreCase("application/atom+xml;type=feed"))
                return Feed.ContentKind.FeedOfFeed;
            return Feed.ContentKind.Unknown;
        }

    }

    Iterable<Entry> getEntries();
    String getId();
    String getTitle();
    Date getUpdated();
    String getAuthorName();
    String getAuthorURI();
    String getAuthorEMail();

    interface Entry {
        String getId();
        Iterable<Content> getContentList();
        Date getUpdated();
        Date getPublished();
        String getTitle();
        String getSummary();
        boolean hasContent();
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
