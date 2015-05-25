package se.lagrummet.rinfo.base.feed.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.lagrummet.rinfo.base.feed.Feed;
import se.lagrummet.rinfo.base.feed.Parser;
import se.lagrummet.rinfo.base.feed.ResourceLocator;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.util.Utils;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by christian on 5/25/15.
 */
public class XmlParserImpl implements Parser {
    String ENTRY_NAME = "entry";
    String LINK_NAME = "link";

    ResourceLocator resourceLocator;

    public XmlParserImpl(ResourceLocator resourceLocator) {
        this.resourceLocator = resourceLocator;
    }

    @Override
    public FeedBuilder parse(ResourceLocator.Resource resource) throws FailedToReadFeedException {
        MyFeedBuilder myFeedBuilder = new MyFeedBuilder();
        resourceLocator.locate(resource, myFeedBuilder);
        if (!myFeedBuilder.waitOk()) {
            throw new FailedToReadFeedException("Failed to locate resource "+myFeedBuilder.getFailure()+" "+myFeedBuilder.getFailureComment());
        }
        return myFeedBuilder;
    }

    private abstract class AbstractReply implements ResourceLocator.Reply {
        private int taskCount = 1;
        private ResourceLocator.Failure failure;
        private String failureComment;

        public ResourceLocator.Failure getFailure() {return failure;}
        public String getFailureComment() {return failureComment;}

        @Override
        public synchronized void ok(ResourceLocator.Data data) {
            try {
                processData(data.asDocument());
                completed();
            } catch (ParseException | MalformedURLException | ParserConfigurationException | SAXException e) {
                failure = ResourceLocator.Failure.Parse;
                failureComment = e.getMessage();
            } catch (IOException e) {
                failure = ResourceLocator.Failure.Unknown;
                failureComment = e.getMessage();
            }
        }

        public synchronized void addTask() {
            taskCount++;
            System.out.println("se.lagrummet.rinfo.base.feed.impl.XmlParserImpl.AbstractReply.addTask taskCount="+taskCount);
        }

        @Override
        public synchronized void failed(ResourceLocator.Failure failure, String comment) {
            this.failure = failure;
            this.failureComment = comment;
            completed();
            System.out.println("se.lagrummet.rinfo.base.feed.impl.XmlParserImpl.AbstractReply.failed failure="+failure+" "+comment);
        }

        protected synchronized void completed() {
            taskCount--;
            if (taskCount ==0)
                this.notifyAll();
            System.out.println("se.lagrummet.rinfo.base.feed.impl.XmlParserImpl.AbstractReply.completed taskCount="+taskCount);
        }

        protected abstract void processData(Document document) throws ParseException, MalformedURLException;

        synchronized boolean waitOk() {
            if (taskCount>0) try {
                wait(60*1000);
            } catch (InterruptedException e) {e.printStackTrace();}
            if (taskCount>0)
                failure = ResourceLocator.Failure.Timeout;
            return failure == null;
        }
    }

    private class MyFeedBuilder extends AbstractReply implements FeedBuilder {

        private List<EntryBuilder> entryBuilders = new ArrayList<>();
        private String id;
        private boolean complete;
        private Date updated;
        private String authorName;
        private String authorUri;
        private String authorEMail;

        @Override public String getId() {return id;}
        @Override public boolean isComplete() {return complete;}
        @Override public Date getUpdated() {return updated;}
        @Override public String getAuthorName() {return authorName;}
        @Override public String getAuthorURI() {return authorUri;}
        @Override public String getAuthorEMail() {return authorEMail;}
        @Override public Iterable<EntryBuilder> getEntries() {return entryBuilders;}

        @Override
        protected void processData(Document document) throws ParseException, MalformedURLException {
            if (document==null)
                throw new NullPointerException("document is null!");
            XmlInterpreter interpreter = new XmlInterpreter(document);
            id = interpreter.getTagValue("id");
            complete = interpreter.tagExists("fh:complete");
            updated = interpreter.getTagValueAsDate("updated");
            authorName = interpreter.getTagValue("authorName");
            authorUri = interpreter.getTagValue("authorUri");
            authorEMail = interpreter.getTagValue("authorEMail");

            for (XmlInterpreter link : interpreter.allNodes(LINK_NAME))
                if (link.hasAttrValue("rel","prev-archive")) {
                    addTask();
                    resourceLocator.locate(new UrlResource(link.getAttrValue("href")), this);
                }

            for (XmlInterpreter entry : interpreter.allNodes(ENTRY_NAME)) {
                MyEntryBuilder myEntryBuilder = new MyEntryBuilder();
                myEntryBuilder.parse(entry);
                entryBuilders.add(myEntryBuilder);
            }
        }
    }

    private class MyEntryBuilder implements EntryBuilder {
        private List<EntryContentBuilder> contents = new ArrayList<>();

        private String id;
        private String title;
        private String summary;
        private int batchIndex;
        private Date updated;
        private Date published;

        @Override public String getId() {return id;}
        @Override public String getTitle() {return title;}
        @Override public String getSummary() {return summary;}
        @Override public int getBatchIndex() {return batchIndex;}
        @Override public Date getUpdated() {return updated;}
        @Override public Date getPublished() {return published;}
        @Override public Iterable<EntryContentBuilder> getContents() {return contents;}

        public void parse(XmlInterpreter entry) throws ParseException {
            id = entry.getTagValue("id");
            title = entry.getTagValue("title");
            summary = entry.getTagValue("summary");
            batchIndex = 0;
            updated = entry.getTagValueAsDate("updated");
            published = entry.getTagValueAsDate("published");

            for (XmlInterpreter content : entry.allNodes("content")) {
                MyEntryContentBuilder myEntryContentBuilder = new MyEntryContentBuilder();
                myEntryContentBuilder.parseContents(content);
                contents.add(myEntryContentBuilder);
            }
            for (XmlInterpreter content : entry.allNodes("link")) {
                if (content.hasAttrValue("rel","alternate")) {
                    MyEntryContentBuilder myEntryContentBuilder = new MyEntryContentBuilder();
                    myEntryContentBuilder.parseLink(content);
                    contents.add(myEntryContentBuilder);
                }
            }
        }
    }

    private class MyEntryContentBuilder implements EntryContentBuilder {
        private String md5Sum;
        private String type;
        private String source;

        @Override public String getMd5SUM() {return md5Sum;}
        @Override public String getType() {return type;}
        @Override public String getSource() {return source;}

        public void parseContents(XmlInterpreter content) {
            md5Sum = content.getAttrValue("le:md5");
            type = content.getAttrValue("type");
            source = content.getAttrValue("src");
        }

        public void parseLink(XmlInterpreter content) {
            md5Sum = content.getAttrValue("le:md5");
            type = content.getAttrValue("type");
            source = content.getAttrValue("href");
        }
    }

    private static class XmlInterpreter {
        Node root;

        private XmlInterpreter(Document document) {
            this.root = document.getDocumentElement();
        }

        private XmlInterpreter(Node node) {
            this.root = node;
        }

        private List<XmlInterpreter> allNodes(String entryName) {
            List<XmlInterpreter> result = new ArrayList<>();
            NodeList entries = root.getChildNodes();
            for (int i = 0; i < entries.getLength(); i++) {
                Node entry = entries.item(i);
                if (entry.getNodeName().equals(entryName))
                    result.add(new XmlInterpreter(entry));
            }
            return result;
        }

        public String getAttrValue(String name) {
            try {
                return root.getAttributes().getNamedItem(name).getTextContent();
            } catch (NullPointerException ignore) {
                return null;
            }
        }

        public boolean hasAttrValue(String name, String value) {
            try {
                return root.getAttributes().getNamedItem(name).getTextContent().equalsIgnoreCase(value);
            } catch (NullPointerException ignore) {
                return false;
            }
        }

        public String getTagAttrValue(String tag, String attr) {
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equalsIgnoreCase(tag)) {
                    Node namedItem = node.getAttributes().getNamedItem(attr);
                    if (namedItem==null)
                        return null;
                    return namedItem.getTextContent();
                }
            }
            return null;
        }

        public String getTagValue(String name) {
            if (root.getNodeName().equalsIgnoreCase(name))
                return root.getTextContent();
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equalsIgnoreCase(name))
                    return node.getTextContent();
            }
            return null;
        }

        public boolean tagExists(String name) {
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equalsIgnoreCase(name))
                    return true;
            }
            return false;
        }

        public Date getTagValueAsDate(String value) throws ParseException {
            return Utils.parseXMLDateTime(getTagValue(value));
        }
    }

}
