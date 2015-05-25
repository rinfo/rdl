package se.lagrummet.rinfo.base.feed;

import se.lagrummet.rinfo.base.feed.exceptions.EntryIdNotFoundException;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedDocumentUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedFeedUrlException;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import javax.activation.MimeType;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Created by christian on 5/21/15.
 */
public interface Feed {

    String ENTRY_NAME = "entry";
    String LINK_NAME = "link";

    DocumentUrl createDocumentUrl(String relativeUrl) throws MalformedURLException;

    enum ContentKind { Source, FeedOfFeed, Unknown }

    void read(CopyFeed.FeedReader feedReader, CopyFeed.DiscoveredEntryCollector discoveredEntryCollector) throws FailedToReadFeedException, MalformedFeedUrlException, EntryIdNotFoundException, MalformedDocumentUrlException;

    interface Entry {
        EntryId getId();
        List<Content> getContentList();
    }

    interface EntryId {
    }

    interface Content {
        ContentKind getContentKind();
        Md5Sum getMd5Sum();
        Long getLength();
        DocumentUrl getDocumentUrl();
        String getType();
    }
}
