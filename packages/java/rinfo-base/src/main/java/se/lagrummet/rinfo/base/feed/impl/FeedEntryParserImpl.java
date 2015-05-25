package se.lagrummet.rinfo.base.feed.impl;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import se.lagrummet.rinfo.base.feed.CopyFeed;
import se.lagrummet.rinfo.base.feed.Feed;
import se.lagrummet.rinfo.base.feed.exceptions.EntryIdNotFoundException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedDocumentUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedFeedUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.SevereInternalException;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

import java.net.MalformedURLException;

/**
* Created by christian on 5/21/15.
*/
public class FeedEntryParserImpl implements CopyFeed.FeedEntryParser {
    final String ENTRY_ID_NAME = "id";
    final String ENTRY_CONTENT_NAME = "content";
    final String ENTRY_LINK_NAME = "link";
    final String ENTRY_CONTENT_TYPE_NAME = "type";
    final String ENTRY_CONTENT_SRC_NAME = "src";
    final String ENTRY_CONTENT_HREF_NAME = "href";
    final String ENTRY_CONTENT_MD5_NAME = "le:md5";
    //final String ENTRY_CONTENT_MD5_NAME = "http://purl.org/atompub/link-extensions/1.0:md5";
    final String ENTRY_CONTENT_LENGTH_NAME = "length";
    final String LINK_REL_PREV_ARCHIVE = "prev-archive";

    @Override
    public void parseLinks(FeedUrl feedUrl, Node link, CopyFeed.DiscoveredEntryCollector discoveredEntryCollector) throws MalformedFeedUrlException {
        Node linkRelPrevArchive = link.getAttributes().getNamedItem(LINK_REL_PREV_ARCHIVE);
        if (linkRelPrevArchive!=null) {
            String textContent = linkRelPrevArchive.getTextContent();
            try {
                discoveredEntryCollector.prevFeed(FeedUrl.parse(textContent));
            } catch (MalformedURLException e) {
                throw new MalformedFeedUrlException(textContent, e);
            }
        }
    }

    @Override
    public void parseEntries(FeedUrl feed, Node nodeEntry, CopyFeed.DiscoveredEntryCollector discoveredEntryCollector) throws EntryIdNotFoundException, MalformedFeedUrlException, MalformedDocumentUrlException {
        NodeEntryProcessor nodeEntryProcessor = new NodeEntryProcessor(nodeEntry);

        Feed.EntryId entryId = nodeEntryProcessor.getId();

        NodeContentProcessor contentProcessor = nodeEntryProcessor.getContent();
        Feed.ContentKind contentKind = contentProcessor.getContentKind();

        processKind(feed, discoveredEntryCollector, entryId, contentProcessor, contentKind);

        NodeContentProcessor linkProcessor = nodeEntryProcessor.getLink();
        if (linkProcessor!=null) {
            Feed.ContentKind alternateKind = linkProcessor.getContentKind();
            processKind(feed, discoveredEntryCollector, entryId, linkProcessor, alternateKind);
        }
    }

    private void processKind(FeedUrl feed, CopyFeed.DiscoveredEntryCollector discoveredEntryCollector, Feed.EntryId entryId, NodeContentProcessor contentProcessor, Feed.ContentKind contentKind) throws MalformedFeedUrlException, MalformedDocumentUrlException {
        System.out.println("se.lagrummet.rinfo.base.feed.impl.FeedEntryParserImpl.processKind ENTER "+feed);
        switch (contentKind) {
            case FeedOfFeed:
                FeedUrl feedUrl = contentProcessor.getFeedUrl();
                discoveredEntryCollector.feedOfFeed(entryId, feedUrl);
                break;
            case Source:
                Md5Sum md5Sum = contentProcessor.getMd5Sum();
                Long length = contentProcessor.getLength();
                DocumentUrl documentUrl = contentProcessor.getDocumentUrl(feed);
                String type = contentProcessor.getType();
                discoveredEntryCollector.document(entryId, documentUrl, md5Sum, length, type);
                break;
            case Unknown:
                //todo warn
                System.out.println("se.lagrummet.rinfo.base.feed.impl.FeedEntryParserImpl.parse UNKNOWN");
                break;
            default:
                throw new SevereInternalException("Unknonw content kind " + contentKind);
        }
    }


    class AbstractNodeProcessor {
         protected Node root;
         protected NodeList childNodes;

         AbstractNodeProcessor(Node root) {
             this.root = root;
             childNodes = root.getChildNodes();

         }

         protected Node getFirstNodeByName(String nodename) {
             for (int i = 0; i < childNodes.getLength(); i++) {
                 Node node = childNodes.item(i);
                 if (node.getNodeName().equalsIgnoreCase(nodename)) {
                     return node;
                 }
             }
             return null;
         }

         protected Node getFirstAttributeByName(String attrName) {
             return root.getAttributes().getNamedItem(attrName);
         }
     }
    
     class NodeEntryProcessor extends AbstractNodeProcessor {

         NodeEntryProcessor(Node root) {
            super(root);
        }


        Feed.EntryId getId() throws EntryIdNotFoundException {
            Node node = getFirstNodeByName(ENTRY_ID_NAME);
            if (node==null)
                throw new EntryIdNotFoundException();
            return new EntryIdImpl(node.getTextContent());
        }

        NodeContentProcessor getContent() {
            Node node = getFirstNodeByName(ENTRY_CONTENT_NAME);
            if (node==null)
                return null;
            return new NodeContentProcessor(node);
        }


         NodeContentProcessor getLink() {
             Node node = getFirstNodeByName(ENTRY_LINK_NAME);
             if (node==null)
                 return null;
             return new NodeContentProcessor(node);
         }

     }

     class NodeContentProcessor extends AbstractNodeProcessor {
         NodeContentProcessor(Node root) {
             super(root);
         }

         Feed.ContentKind getContentKind() {
             Node node = getFirstAttributeByName(ENTRY_CONTENT_TYPE_NAME);
             if (node.getTextContent().equalsIgnoreCase("application/xhtml+xml")
                     || node.getTextContent().equalsIgnoreCase("application/pdf")
                     || node.getTextContent().equalsIgnoreCase("application/rdf+xml")
                     || node.getTextContent().equalsIgnoreCase("text/html")
                     )
                 return Feed.ContentKind.Source;
             if (node.getTextContent().equalsIgnoreCase("application/atom+xml;type=feed"))
                 return Feed.ContentKind.FeedOfFeed;
             return Feed.ContentKind.Unknown;
         }


         public FeedUrl getFeedUrl() throws MalformedFeedUrlException {
             Node node = getFirstAttributeByName(ENTRY_CONTENT_SRC_NAME);
             try {
                 return FeedUrl.parse(node.getTextContent());
             } catch (MalformedURLException e) {
                 throw new MalformedFeedUrlException(node.getTextContent(), e);
             }
         }

         public Md5Sum getMd5Sum() {
             Node node = getFirstAttributeByName(ENTRY_CONTENT_MD5_NAME);
             if (node==null)
                return null;
             return Md5Sum.create(node.getTextContent());
         }

         public Long getLength() {
             Node node = getFirstAttributeByName(ENTRY_CONTENT_LENGTH_NAME);
             if (node==null)
                 return null;
             return Long.parseLong(node.getTextContent());
         }

         public DocumentUrl getDocumentUrl(FeedUrl feed) throws MalformedDocumentUrlException {
             Node node = getFirstAttributeByName(ENTRY_CONTENT_SRC_NAME);
             if (node==null) {
                 node = getFirstAttributeByName(ENTRY_CONTENT_HREF_NAME);
                 if (node==null)
                    return null;
             }
             try {
                 return feed.createDocumentUrl(node.getTextContent());
             } catch (MalformedURLException e) {
                 throw new MalformedDocumentUrlException(node.getTextContent(), e);
             }
         }

         public String getType() {
             Node node = getFirstAttributeByName(ENTRY_CONTENT_TYPE_NAME);
             return node.getTextContent();
         }
     }

}

/*
    <entry>
        <id>http://rinfo.lagrummet.se/publ/sfs/2014:1</id>
        <updated>2014-04-02T09:12:18Z</updated>
        <published>2014-04-02T09:12:18Z</published>
        <title>2014:1 Tillkännagivande (2014:1) av uppgift om Riksbankens referensränta</title>
        <summary/>
        <link rel="alternate" href="2014_1_grund.xhtml"
              type="application/rdf+xml" length="1439" le:md5="8EBAE651E1A9C934C17948F3E8ED4C60"/>
        <content src="http://rkrattsdb.gov.se/SFSdoc/14/140001.pdf" type="application/pdf"/>
    </entry>
*/