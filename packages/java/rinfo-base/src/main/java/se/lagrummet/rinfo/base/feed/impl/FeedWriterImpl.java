package se.lagrummet.rinfo.base.feed.impl;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import se.lagrummet.rinfo.base.feed.FeedWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by christian on 2015-05-27.
 */
public class FeedWriterImpl implements FeedWriter.Writer {

    String XMLNS_ROOT = "http://www.w3.org/2005/Atom";
    String XMLNS_LINK_EXTENSIONS_NAME = "le";
    String XMLNS_LINK_EXTENSIONS_URI = "http://purl.org/atompub/link-extensions/1.0";
    String XMLNS_FEED_PAGING_NAME = "fh";
    String XMLNS_FEED_PAGING_URI = "http://purl.org/syndication/history/1.0";
    String XMLNS_ATOM_DELETED_ENTRY_NAME = "at";
    String XMLNS_ATOM_DELETED_ENTRY_URI = "http://purl.org/atompub/tombstones/1.0";

    // xmlns="http://www.w3.org/2005/Atom"
    // xmlns:le="http://purl.org/atompub/link-extensions/1.0"
    // xmlns:fh="http://purl.org/syndication/history/1.0"
    // xml:lang="sv" xmlns:at="http://purl.org/atompub/tombstones/1.0"

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    Document document;
    Element node;

    public FeedWriterImpl() throws ParserConfigurationException {
        this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        this.node = this.document.createElement("feed");
        this.document.appendChild(node);

        setAttributeValue("xmlns", XMLNS_ROOT);
        setAttributeNSValue("xmlns", XMLNS_LINK_EXTENSIONS_NAME, XMLNS_LINK_EXTENSIONS_URI);
        setAttributeNSValue("xmlns", XMLNS_FEED_PAGING_NAME, XMLNS_FEED_PAGING_URI);
        setAttributeNSValue("xmlns", XMLNS_ATOM_DELETED_ENTRY_NAME, XMLNS_ATOM_DELETED_ENTRY_URI);
    }

    private void setAttributeValue(String name, String value) {
        Attr rootXmlNs = document.createAttribute(name);
        node.setAttributeNode(rootXmlNs);
        rootXmlNs.setValue(value);
    }

    private void setAttributeNSValue(String name, String ns, String value) {
        Attr rootXmlNs = document.createAttributeNS(name, ns);
        node.setAttributeNode(rootXmlNs);
        rootXmlNs.setValue(value);
    }

    private FeedWriterImpl(Document document, Element node) {
        if (document==null)
            throw new NullPointerException("document is null!");
        if (node==null)
            throw new NullPointerException("node is null!");
        this.document = document;
        this.node = node;
    }

    @Override
    public FeedWriter.Writer setContent(String content) {
        this.node.setTextContent(content);
        return this;
    }

    @Override
    public FeedWriter.Writer setContent(Date content) {
        if (content!=null)
            this.node.setTextContent(sdf.format(content));
        return this;
    }

    @Override
    public FeedWriter.Writer setAttribute(String name, String value) {
        Attr attribute = document.createAttribute(name);
        attribute.setValue(value);
        this.node.setAttributeNode(attribute);
        return this;
    }

    @Override
    public FeedWriter.Writer setAttribute(String name, Date value) {
        if (value!=null) {
            Attr attribute = document.createAttribute(name);
            attribute.setValue(sdf.format(value));
            this.node.setAttributeNode(attribute);
        }
        return this;
    }

    @Override
    public FeedWriter.Writer createChild(String name) {
        if (name==null)
            throw new NullPointerException("name is null!");
        Element newElement = document.createElement(name);
        FeedWriterImpl taganizer = new FeedWriterImpl(document, newElement);
        node.appendChild(newElement);
        return taganizer;
    }

    @Override
    public FeedWriter.Writer createChildAndSetContent(String tagName, String content) {
        FeedWriter.Writer child = createChild(tagName);
        child.setContent(content);
        return child;
    }

    @Override
    public FeedWriter.Writer createChildAndSetContent(String tagName, Date content) {
        FeedWriter.Writer child = createChild(tagName);
        if (content!=null)
            child.setContent(content);
        return child;
    }

    public void writeTo(OutputStream outputStream) throws TransformerException {
        Source xmlSource = new DOMSource(document);
        Result outputTarget = new StreamResult(outputStream);
        TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
    }
}

/*

<feed xmlns="http://www.w3.org/2005/Atom" xmlns:le="http://purl.org/atompub/link-extensions/1.0" xmlns:fh="http://purl.org/syndication/history/1.0" xml:lang="sv" xmlns:at="http://purl.org/atompub/tombstones/1.0">
    <id>tag:sfs.regeringen.se,2013:rinfo:feed:2014</id>
    <fh:complete/><title>SFS</title>
    <updated>2015-03-25T14:07:14Z</updated>
    <author>
        <name>Regeringskansliet</name>
        <uri>http://www.regeringen.se/</uri>
        <email>rinfo@regeringen.se</email>
    </author>
    <link href="simplefeed.atom" rel="self"/>
    <link href="simplefeed_next.atom" rel="prev-archive"/>
    <at:deleted-entry ref="http://rinfo.lagrummet.se/publ/rf/nja/2013/s_75" when="2014-08-18T14:53:53.221139000Z"/>
    <entry>
        <id>http://rinfo.lagrummet.se/publ/sfs/2014:1/konsolidering/2014-01-21</id>
        <updated>2014-01-21T11:41:14Z</updated>
        <published>2014-01-21T11:41:14Z</published>
        <title>2014:1 Tillkännagivande (2014:1) av uppgift om Riksbankens referensränta</title>
        <summary/>
        <content src="2014_1_konsolidering_senaste.xhtml"
                 type="application/xhtml+xml"
                 le:md5="D841B140ECD65D12572211E7BEC599CC"/>
    </entry>



 */