package se.lagrummet.rinfo.base.feed;

import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;

import java.util.Date;

/**
 * Created by christian on 5/25/15.
 */
public interface Parser {

    FeedBuilder parse(ResourceLocator.Resource resource) throws FailedToReadFeedException;

    interface FeedBuilder {
        String getId();
        boolean isComplete();
        Date getUpdated();
        String getAuthorName();
        String getAuthorURI();
        String getAuthorEMail();
        Iterable<EntryBuilder> getEntries();
    }

    interface EntryBuilder {
        String getId();
        String getTitle();
        String getSummary();
        int getBatchIndex();
        Date getUpdated();
        Date getPublished();
        Iterable<EntryContentBuilder> getContents();
    }

    interface EntryContentBuilder {
        String getSource();
        String getType();
        String getMd5SUM();
    }
}
