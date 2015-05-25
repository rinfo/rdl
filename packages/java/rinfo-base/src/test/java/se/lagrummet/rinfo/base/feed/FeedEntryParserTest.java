package se.lagrummet.rinfo.base.feed;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.lagrummet.rinfo.base.feed.exceptions.EntryIdNotFoundException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedDocumentUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedFeedUrlException;
import se.lagrummet.rinfo.base.feed.impl.EntryIdImpl;
import se.lagrummet.rinfo.base.feed.impl.FeedEntryParserImpl;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * Created by christian on 5/22/15.
 *
 * Test of processing entries in feed
 */
public class FeedEntryParserTest {
    final String RESOURCES_PATH = "packages/java/rinfo-base/src/test/resources/";
    final String FEEDS_PATH = RESOURCES_PATH+"feeds/";
    final String SIMPLE_FEED = FEEDS_PATH+"simplefeed.atom";

    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    CopyFeed.FeedEntryParser feedEntryParser;
    FeedUrl feedUrl;
    Document document;
    NodeList entries;
    CopyFeed.DiscoveredEntryCollector discoveredEntryCollector;

    @Before
    public void init() {
        try {
            feedEntryParser = new FeedEntryParserImpl();
            feedUrl = FeedUrl.parse("Http://my.se");
            document = dbf.newDocumentBuilder().parse(new File(SIMPLE_FEED));
            entries = document.getDocumentElement().getElementsByTagName(Feed.ENTRY_NAME);
            discoveredEntryCollector = mock(CopyFeed.DiscoveredEntryCollector.class);
        } catch (Exception ignore) {ignore.printStackTrace();}
    }

    @Test
    public void countParsedEntries() {
        assertEquals(3, entries.getLength());
    }

    @Test
    public void parseEntryWithXHtmlContent() throws ParserConfigurationException, IOException, SAXException, MalformedDocumentUrlException, EntryIdNotFoundException, MalformedFeedUrlException {
        feedEntryParser.parseEntries(feedUrl, entries.item(0), discoveredEntryCollector);
        verify(discoveredEntryCollector).document(
                new EntryIdImpl("http://rinfo.lagrummet.se/publ/sfs/2014:1/konsolidering/2014-01-21"),
                DocumentUrl.parse("http://my.se/2014_1_konsolidering_senaste.xhtml"),
                Md5Sum.create("D841B140ECD65D12572211E7BEC599CC"),
                null,
                "application/xhtml+xml");

        //verify(discoveredEntryCollector1, never()).document(any(Feed.EntryId.class), any(DocumentUrl.class), any(Md5Sum.class));
    }

    @Test
    public void parseEntryWithXHtmlContentWithOtherData() throws ParserConfigurationException, IOException, SAXException, MalformedDocumentUrlException, EntryIdNotFoundException, MalformedFeedUrlException {
        feedEntryParser.parseEntries(feedUrl, entries.item(1), discoveredEntryCollector);
        verify(discoveredEntryCollector, times(1)).document(
                new EntryIdImpl("http://rinfo.lagrummet.se/publ/sfs/2014:2/konsolidering/2014-02-15"),
                DocumentUrl.parse("http://my.se/2014_2_konsolidering_senaste.xhtml"),
                Md5Sum.create("00B6B1477062C819E79114318BCC7843"),
                null,
                "application/xhtml+xml");

        //verify(discoveredEntryCollector1, never()).document(any(Feed.EntryId.class), any(DocumentUrl.class), any(Md5Sum.class));
    }

    @Test
    public void parseEntryWithPdfContentAndXHtmlRel() throws ParserConfigurationException, IOException, SAXException, MalformedDocumentUrlException, EntryIdNotFoundException, MalformedFeedUrlException {
        feedEntryParser.parseEntries(feedUrl, entries.item(2), discoveredEntryCollector);
        verify(discoveredEntryCollector, times(1)).document(
                new EntryIdImpl("http://rinfo.lagrummet.se/publ/sfs/2014:1"),
                DocumentUrl.parse("http://rkrattsdb.gov.se/SFSdoc/14/140001.pdf"),
                null,
                null,
                "application/pdf");
        verify(discoveredEntryCollector, times(1)).document(
                new EntryIdImpl("http://rinfo.lagrummet.se/publ/sfs/2014:1"),
                DocumentUrl.parse("http://my.se/2014_1_grund.xhtml"),
                Md5Sum.create("8EBAE651E1A9C934C17948F3E8ED4C60"),
                new Long(1439),
                "application/rdf+xml");
    }
}


