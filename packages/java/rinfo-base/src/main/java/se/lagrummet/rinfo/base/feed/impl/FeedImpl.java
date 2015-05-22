package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.CopyFeed;
import se.lagrummet.rinfo.base.feed.Feed;
import se.lagrummet.rinfo.base.feed.exceptions.EntryIdNotFoundException;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedDocumentUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedFeedUrlException;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;

import java.net.MalformedURLException;

/**
 * Created by christian on 5/22/15.
 */
public class FeedImpl implements Feed {
    private FeedUrl feedURL;

    public FeedImpl(FeedUrl feedURL) {
        this.feedURL = feedURL;
    }

    @Override
    public DocumentUrl createDocumentUrl(String relativeUrl) throws MalformedURLException {
        return feedURL.createDocumentUrl(relativeUrl);
    }

    @Override
    public void read(CopyFeed.FeedReader feedReader, CopyFeed.DiscoveredEntryCollector discoveredEntryCollector) throws FailedToReadFeedException, MalformedFeedUrlException, EntryIdNotFoundException, MalformedDocumentUrlException {
        feedReader.read(feedURL, discoveredEntryCollector);
    }
}
