package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.CopyFeed;
import se.lagrummet.rinfo.base.feed.Feed;
import se.lagrummet.rinfo.base.feed.exceptions.*;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.Semaphore;

/**
 * Created by christian on 5/21/15.
 */
public class CopyFeedImpl implements CopyFeed, CopyFeed.FileNameCreator {

    ErrorReport errorReport = new ErrorReportImpl();
    FeedReader feedReader = new FeedReaderImpl(new FeedEntryParserImpl());
    FeedBuilder feedBuilder = new FeedBuilderImpl();
    EntryDocumentDownloader documentDownloader = new EntryDocumentDownloaderImpl(this);

    public CopyFeedImpl() {
    }

    public void copy(FeedUrl feedURL, final String targetPath) throws FailedToReadFeedException, EntryIdNotFoundException, MalformedFeedUrlException, MalformedDocumentUrlException {
        final Feed feed = new FeedImpl(feedURL);

        final WaitCounter waitCounter = new WaitCounter();

        feed.read(feedReader, new DiscoveredEntryCollector() {
            @Override
            public void feedOfFeed(Feed.EntryId entryId, FeedUrl feedUrl) {
                try {
                    feedReader.read(feedUrl, this);
                } catch (FailedToReadFeedException e) {
                    errorReport.failedToReed(feed, entryId, feedUrl, e);
                } catch (MalformedDocumentUrlException e) {
                    errorReport.mailformedDocumentUrl(feed, entryId, feedUrl, e);
                } catch (EntryIdNotFoundException e) {
                    errorReport.entryIdNotFound(feed, entryId, feedUrl, e);
                } catch (MalformedFeedUrlException e) {
                    errorReport.malformedFeedUrl(feed, entryId, feedUrl, e);
                }
            }

            @Override
            public void document(final Feed.EntryId entryId, final DocumentUrl documentURL, final Md5Sum md5sum, final Long length, final String type) {
                waitCounter.start();
                documentDownloader.downloadEntryAndVerifyMd5Sum(new EntryDocumentDownloaderReply() {
                    @Override
                    public void completed() {
                        feedBuilder.addEntry(entryId, documentURL, md5sum, length, type);
                        waitCounter.end();
                    }

                    @Override
                    public void md5SumCheckFailed(Md5Sum md5SumOfDownloadedDocument) {
                        errorReport.md5SumCheckFailed(entryId, documentURL, md5sum, md5SumOfDownloadedDocument);
                        waitCounter.end();
                    }

                    @Override
                    public void failed(Exception e) {
                        errorReport.failedToDownloadDocument(entryId, documentURL, md5sum, e);
                        waitCounter.end();
                    }
                }, feed, entryId, documentURL, md5sum, targetPath);
            }

            @Override
            public void prevFeed(FeedUrl feedUrl) {
                try {
                    feedReader.read(feedUrl, this);
                } catch (FailedToReadFeedException e) {
                    errorReport.failedToReed(feed, null, feedUrl, e);
                } catch (MalformedDocumentUrlException e) {
                    errorReport.mailformedDocumentUrl(feed, null, feedUrl, e);
                } catch (EntryIdNotFoundException e) {
                    errorReport.entryIdNotFound(feed, null, feedUrl, e);
                } catch (MalformedFeedUrlException e) {
                    errorReport.malformedFeedUrl(feed, null, feedUrl, e);
                }
            }
        });

        try {
            waitCounter.wait(1000 * 60 * 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String createFileName(Feed feed, Feed.EntryId getEntryId, DocumentUrl documentUrl) {
        return documentUrl.getName();
    }

    private class WaitCounter {
        int count = 0;

        synchronized void start() { count++;}
        synchronized void end() {
            count--;
            if (count==0)
                this.notifyAll();
        }
    }
}
