package se.lagrummet.rinfo.base.feed;

import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;

import java.util.Date;

/**
 * Created by christian on 5/25/15.
 */
public interface Parser {

    FeedBuilder parse(ResourceLocator.Resource resource, Report report) throws FailedToReadFeedException;

    interface FeedBuilder {
        String getId();
        boolean isComplete();
        Date getUpdated();
        String getAuthorName();
        String getAuthorURI();
        String getAuthorEMail();
        Iterable<EntryBuilder> getEntries();

        Feed toFeed();
    }

    interface EntryBuilder {
        String getBaseUrl();
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
        String getLength();

        boolean isFeedOfFeed();


    }
}
