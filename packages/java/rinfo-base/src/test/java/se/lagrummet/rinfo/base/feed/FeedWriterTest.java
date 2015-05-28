package se.lagrummet.rinfo.base.feed;

import org.junit.Assert;
import org.junit.Test;
import se.lagrummet.rinfo.base.feed.impl.FeedWriterImpl;
import se.lagrummet.rinfo.base.feed.impl.FeedXmlBuilderImpl;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Created by christian on 2015-05-27.
 */
public class FeedWriterTest {
    final String STATIC_XML1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:at=\"http://purl.org/atompub/tombstones/1.0\" xmlns:fh=\"http://purl.org/syndication/history/1.0\" xmlns:le=\"http://purl.org/atompub/link-extensions/1.0\">\n" +
            "    <id>http://myid.org/1.0</id>\n" +
            "    <fh.complete/>\n" +
            "    <title>MyTitle</title>\n" +
            "    <updated>1970-01-04T10:37:54</updated>\n" +
            "    <author>\n" +
            "        <name>MyAuthor</name>\n" +
            "        <uri>MyAuthorURI</uri>\n" +
            "        <email>MyAuthorEmail</email>\n" +
            "    </author>\n" +
            "    <entry>\n" +
            "        <id>987987</id>\n" +
            "        <updated>1970-01-01T01:39:03</updated>\n" +
            "        <published>1970-01-01T01:00:12</published>\n" +
            "        <content length=\"79879\" src=\"/home.pdf\" type=\"application/pdf\"/>\n" +
            "        <link href=\"/home.xhtml\" length=\"79879\" rel=\"alternate\" type=\"application/pdf\"/>\n" +
            "    </entry>\n" +
            "    <title>MyTitel</title>\n" +
            "    <summary>MySummary</summary>\n" +
            "</feed>\n";
    final String STATIC_XML2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:at=\"http://purl.org/atompub/tombstones/1.0\" xmlns:fh=\"http://purl.org/syndication/history/1.0\" xmlns:le=\"http://purl.org/atompub/link-extensions/1.0\">\n" +
            "    <id>9873434</id>\n" +
            "    <date>1970-01-03T18:03:54</date>\n" +
            "    <child childname=\"Hubbe\">\n" +
            "        <version>1.0</version>\n" +
            "        <grandchild>\n" +
            "            <fh:age>100</fh:age>\n" +
            "            <birth>1970-01-03T18:08:02</birth>\n" +
            "        </grandchild>\n" +
            "    </child>\n" +
            "</feed>\n";

//    <link rel="alternate" href="2014_1_grund.xhtml"
//    type="application/rdf+xml" length="1439" le:md5="8EBAE651E1A9C934C17948F3E8ED4C60"/>

    @Test
    public void writeXML() throws ParserConfigurationException, TransformerException {
        FeedWriter feedWriter = new FeedXmlBuilderImpl();
        FeedWriterImpl writer = new FeedWriterImpl();
        Feed feed = new TestFeed();
        feedWriter.write(feed, writer, mock(Report.class));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.writeTo(outputStream);

        //todo fix xmlns
        Assert.assertEquals(STATIC_XML1,new String(outputStream.toByteArray()));
    }

    @Test
    public void buildXML() throws ParserConfigurationException, TransformerException {
        FeedWriterImpl writer = new FeedWriterImpl();

        writer.createChildAndSetContent("id","9873434");
        writer.createChildAndSetContent("date",new Date(234234244));
        FeedWriter.Writer child = writer.createChild("child");
        child.setAttribute("childname","Hubbe");
        child.createChildAndSetContent("version","1.0");
        FeedWriter.Writer grandchild = child.createChild("grandchild");
        grandchild.createChildAndSetContent("fh:age","100");
        grandchild.createChildAndSetContent("birth",new Date(234482394));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.writeTo(outputStream);
        //todo fix xmlns
        Assert.assertEquals(STATIC_XML2, new String(outputStream.toByteArray()));
    }

    private class TestFeed implements Feed {
        @Override public String getId() {return "http://myid.org/1.0";}
        @Override public String getTitle() {return "MyTitle";}
        @Override public Date getUpdated() {return new Date(293874987);}
        @Override public String getAuthorName() {return "MyAuthor";}
        @Override public String getAuthorURI() {return "MyAuthorURI";}
        @Override public String getAuthorEMail() {return "MyAuthorEmail";}

        @Override
        public Iterable<Entry> getEntries() {
            List<Entry> entries = new LinkedList<>();
            entries.add(new TestEntry("987987",new Date(2343987), new Date(12313), "MyTitel", "MySummary")
                    .addContent("http://myhome.se/home.pdf","application/pdf",null,79879l, ContentKind.Source)
                    .addContent("/home.xhtml", "application/pdf", null, 79879l, ContentKind.Alternate)
            );
            return entries;
        }

    }

    private class TestEntry implements Feed.Entry {
        private String id;
        private Date updated;
        private Date published;
        private String title;
        private String summary;
        private List<Feed.Content> contents = new LinkedList<>();

        private TestEntry(String id, Date updated, Date published, String title, String summary) {
            this.id = id;
            this.updated = updated;
            this.published = published;
            this.title = title;
            this.summary = summary;
        }

        @Override public String getId() {return id;}
        @Override public Date getUpdated() {return updated;}
        @Override public Date getPublished() {return published;}
        @Override public String getTitle() {return title;}
        @Override public String getSummary() {return summary;}
        @Override public boolean hasContent() {return true;}

        @Override public Iterable<Feed.Content> getContentList() {return contents;}

        TestEntry addContent(final String url, final String type, final String md5Sum, final Long length, final Feed.ContentKind contentKind) {
            contents.add(new Feed.Content() {
                @Override public Feed.ContentKind getContentKind() {return contentKind;}
                @Override public Md5Sum getMd5Sum() {return Md5Sum.create(md5Sum);}
                @Override public Long getLength() {return length;}
                @Override public DocumentUrl getDocumentUrl() {return DocumentUrl.create(url);}
                @Override public String getType() {return type;}
                @Override public ResourceLocator.Resource asResource() {throw new RuntimeException("Not implemented");}
            });
            return this;
        }
    }
}
