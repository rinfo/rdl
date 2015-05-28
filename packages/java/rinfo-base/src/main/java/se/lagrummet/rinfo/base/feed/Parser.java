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
        String getTitle();
        boolean isComplete();
        Date getUpdated();
        String getAuthorName();
        String getAuthorURI();
        String getAuthorEMail();
        Iterable<EntryBuilder> getEntries();
        Iterable<DeletedEntryBuilder> getDeletedEntries();
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
        boolean isAlternate();
    }

    interface DeletedEntryBuilder {
        String getEntryId();
        Date getWhen();
    }
}
