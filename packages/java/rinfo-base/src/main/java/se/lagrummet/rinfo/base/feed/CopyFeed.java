package se.lagrummet.rinfo.base.feed;

import org.w3c.dom.Node;
import se.lagrummet.rinfo.base.feed.exceptions.EntryIdNotFoundException;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedDocumentUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedFeedUrlException;
import se.lagrummet.rinfo.base.feed.type.CommonUrl;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;

/**
 * Created by christian on 5/21/15.
 */
public interface CopyFeed  {

    void copy(FeedUrl feedURL, final String targetPath) throws FailedToReadFeedException, EntryIdNotFoundException, MalformedFeedUrlException, MalformedDocumentUrlException;

    interface FeedReader {
        void read(FeedUrl feedUrl, DiscoveredEntryCollector discoveredEntryCollector) throws FailedToReadFeedException, MalformedDocumentUrlException, EntryIdNotFoundException, MalformedFeedUrlException;
    }

    interface FeedBuilder {
        void addEntry(Feed.EntryId entryId, DocumentUrl documentURL, Md5Sum md5sum, Long length, String type);
    }

    interface DiscoveredEntryCollector {
        void feedOfFeed(Feed.EntryId entryId, FeedUrl feedUrl);
        void document(Feed.EntryId entryId, DocumentUrl documentURL, Md5Sum md5sum, Long length, String type);
        void prevFeed(FeedUrl feedUrl);
    }

    interface FeedEntryParser {
         void parseEntries(FeedUrl feed, Node nodeEntry, DiscoveredEntryCollector discoveredEntryCollector) throws EntryIdNotFoundException, MalformedFeedUrlException, MalformedDocumentUrlException;
         void parseLinks(FeedUrl feedUrl, Node link, DiscoveredEntryCollector discoveredEntryCollector) throws MalformedFeedUrlException;
    }

    interface EntryDocumentDownloader {
        void downloadEntryAndVerifyMd5Sum(EntryDocumentDownloaderReply reply, Feed feed, Feed.EntryId entryId, DocumentUrl documentUrl, Md5Sum md5Sum, String targetPath);
    }

    interface FileNameCreator {
        String createFileName(Feed feed, Feed.EntryId getEntryId, DocumentUrl documentUrl);
    }

    interface EntryDocumentDownloaderReply  {
        void completed();
        void md5SumCheckFailed(Md5Sum md5SumOfDownloadedDocument);
        void failed(Exception e);
    }

    interface ErrorReport {
        void failedToReed(Feed feed, Feed.EntryId entryId, CommonUrl commonUrl, FailedToReadFeedException e);
        void md5SumCheckFailed(Feed.EntryId entryId, DocumentUrl documentURL, Md5Sum md5sum, Md5Sum md5SumOfDownloadedDocument);
        void failedToDownloadDocument(Feed.EntryId entryId, DocumentUrl documentURL, Md5Sum md5sum, Exception e);

        void mailformedDocumentUrl(Feed feed, Feed.EntryId entryId, FeedUrl feedUrl, MalformedDocumentUrlException e);

        void entryIdNotFound(Feed feed, Feed.EntryId entryId, FeedUrl feedUrl, EntryIdNotFoundException e);

        void malformedFeedUrl(Feed feed, Feed.EntryId entryId, FeedUrl feedUrl, MalformedFeedUrlException e);
    }


}
