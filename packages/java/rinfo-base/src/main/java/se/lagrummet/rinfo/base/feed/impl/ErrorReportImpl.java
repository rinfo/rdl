package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.CopyFeed;
import se.lagrummet.rinfo.base.feed.Feed;
import se.lagrummet.rinfo.base.feed.exceptions.EntryIdNotFoundException;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedDocumentUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedFeedUrlException;
import se.lagrummet.rinfo.base.feed.type.CommonUrl;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import java.net.URI;
import java.net.URL;

/**
* Created by christian on 5/21/15.
*/
public class ErrorReportImpl implements CopyFeed.ErrorReport {

    @Override
    public void failedToReed(Feed feed, Feed.EntryId entryId, CommonUrl commonUrl, FailedToReadFeedException e) {
        System.out.println("se.lagrummet.rinfo.base.feed.CopyFeed.ErrorReportImpl.failedToReed entryId="+entryId+" feed="+feed+" url="+commonUrl);
    }

    @Override
    public void md5SumCheckFailed(Feed.EntryId entryId, DocumentUrl documentURL, Md5Sum md5sum, Md5Sum md5SumOfDownloadedDocument) {
        System.out.println("se.lagrummet.rinfo.base.feed.impl.ErrorReportImpl.md5SumCheckFailed");
    }

    @Override
    public void failedToDownloadDocument(Feed.EntryId entryId, DocumentUrl documentURL, Md5Sum md5sum, Exception e) {
        System.out.println("se.lagrummet.rinfo.base.feed.impl.ErrorReportImpl.failedToDownloadDocument");
    }

    @Override
    public void mailformedDocumentUrl(Feed feed, Feed.EntryId entryId, FeedUrl feedUrl, MalformedDocumentUrlException e) {
        System.out.println("se.lagrummet.rinfo.base.feed.impl.ErrorReportImpl.mailformedDocumentUrl");
    }

    @Override
    public void entryIdNotFound(Feed feed, Feed.EntryId entryId, FeedUrl feedUrl, EntryIdNotFoundException e) {
        System.out.println("se.lagrummet.rinfo.base.feed.impl.ErrorReportImpl.entryIdNotFound");
    }

    @Override
    public void malformedFeedUrl(Feed feed, Feed.EntryId entryId, FeedUrl feedUrl, MalformedFeedUrlException e) {
        System.out.println("se.lagrummet.rinfo.base.feed.impl.ErrorReportImpl.malformedFeedUrl");
    }
}
