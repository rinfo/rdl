package se.lagrummet.rinfo.base.feed;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.impl.ReportImpl;
import se.lagrummet.rinfo.base.feed.impl.XmlParserImpl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static org.mockito.Mockito.mock;

/**
 * Created by christian on 5/22/15.
 *
 * Tests reading of feed skeleton. Not processing entries.
 */
public class XmlParserTest {
    final String RESOURCES_PATH = "packages/java/rinfo-base/src/test/resources/";
    final String FEEDS_PATH = RESOURCES_PATH+"feeds/";
    final String SIMPLE_FEED = FEEDS_PATH+"simplefeed.atom";
    final String SIMPLE_NEXT_FEED = FEEDS_PATH+"simplefeed_next.atom";

    final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    @Test
    public void parseAtomFeed() throws FailedToReadFeedException {
        final ResourceLocator.Resource simple_feed_atom = mock(ResourceLocator.Resource.class);

        ResourceLocator resourceLocator = new MyResourceLocator(simple_feed_atom);

        XmlParserImpl xmlParser = new XmlParserImpl(resourceLocator);

        Parser.FeedBuilder feedBuilder = xmlParser.parse(simple_feed_atom, new ReportImpl());

        Assert.assertEquals("tag:sfs.regeringen.se,2013:rinfo:feed:2014", feedBuilder.getId());
        Iterator<Parser.EntryBuilder> iterator = feedBuilder.getEntries().iterator();
        Assert.assertEquals("http://rinfo.lagrummet.se/publ/dom/mmd/p8048-12/2013-01-10", iterator.next().getId());
        Assert.assertEquals("http://rinfo.lagrummet.se/publ/rf/mod/2013:5", iterator.next().getId());
        Assert.assertEquals("http://rinfo.lagrummet.se/publ/dom/hd/oe4581-12/2013-02-18", iterator.next().getId());
        Assert.assertEquals("http://rinfo.lagrummet.se/publ/rf/nja/2013/s_75", iterator.next().getId());
        Assert.assertEquals("http://rinfo.lagrummet.se/publ/sfs/2014:1/konsolidering/2014-01-21", iterator.next().getId());
        Assert.assertEquals("http://rinfo.lagrummet.se/publ/sfs/2014:2/konsolidering/2014-02-15", iterator.next().getId());
        Assert.assertEquals("http://rinfo.lagrummet.se/publ/sfs/2014:1", iterator.next().getId());
        Assert.assertFalse(iterator.hasNext());
    }

    private class MyResourceLocator implements ResourceLocator {
        private final Resource simple_feed_atom;

        public MyResourceLocator(Resource simple_feed_atom) {
            this.simple_feed_atom = simple_feed_atom;
        }

        @Override
        public void locate(Resource resource, Reply reply) {
            if (resource== simple_feed_atom) {
                reply.ok(new MyData(SIMPLE_FEED, resource));
                return ;
            }
            reply.ok(new MyData(SIMPLE_NEXT_FEED, resource));
        }

        private class MyData implements Data {
            private final String filename;
            private Resource resource;

            public MyData(String filename, Resource resource) {
                this.filename = filename;
                this.resource = resource;
            }

            @Override public Resource getResource() {return resource;}
            @Override public Md5Sum getMd5Sum() {return null;}

            @Override public InputStream asInputStream() {
                try {
                    return new FileInputStream(filename);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("Internal error",e);
                }
            }

            @Override
            public Document asDocument() throws ParserConfigurationException, IOException, SAXException {
                return dbf.newDocumentBuilder().parse(asInputStream());
            }
        }
    }
}
