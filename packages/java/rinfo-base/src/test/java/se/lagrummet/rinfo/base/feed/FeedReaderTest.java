package se.lagrummet.rinfo.base.feed;

import org.junit.Test;
import static org.mockito.Mockito.*;

import org.mockito.Matchers;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import se.lagrummet.rinfo.base.feed.exceptions.EntryIdNotFoundException;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedDocumentUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedFeedUrlException;
import se.lagrummet.rinfo.base.feed.impl.FeedReaderImpl;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Created by christian on 5/22/15.
 *
 * Tests reading of feed skeleton. Not processing entries.
 */
public class FeedReaderTest {
    final String RESOURCES_PATH = "packages/java/rinfo-base/src/test/resources/";
    final String FEEDS_PATH = RESOURCES_PATH+"feeds/";
    final String SIMPLE_FEED = FEEDS_PATH+"simplefeed.atom";

    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    @Test
    public void readFeed() throws FailedToReadFeedException, ParserConfigurationException, IOException, SAXException, MalformedFeedUrlException, EntryIdNotFoundException, MalformedDocumentUrlException {
        CopyFeed.FeedEntryParser feedEntryParser = mock(CopyFeed.FeedEntryParser.class);
        CopyFeed.DiscoveredEntryCollector discoveredEntryCollector = mock(CopyFeed.DiscoveredEntryCollector.class);
        CopyFeed.FeedReader feedReader = new FeedReaderImpl(feedEntryParser);
        FeedUrl feedUrl = mock(FeedUrl.class);

        when(feedUrl.getDocument()).thenReturn(dbf.newDocumentBuilder().parse(new File(SIMPLE_FEED)));

        feedReader.read(feedUrl, discoveredEntryCollector);

        //verify(feedEntryParser,times(2)).parse(isA(Node.class), discoveredEntryCollector);
    }
}
