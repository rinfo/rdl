package se.lagrummet.rinfo.base.feed.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import se.lagrummet.rinfo.base.feed.FeedXmlBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by christian on 2015-05-27.
 */
public class TaganizerImpl implements FeedXmlBuilder.Taganizer {

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    Document document;
    Node node;

    public TaganizerImpl(Document document) {
        this(document, document.getDocumentElement());
    }

    private TaganizerImpl(Document document, Node node) {
        this.document = document;
        this.node = node;
    }

    @Override
    public FeedXmlBuilder.Taganizer setContent(String content) {
        this.node.setTextContent(content);
        return this;
    }

    @Override
    public FeedXmlBuilder.Taganizer setContent(Date content) {
        this.node.setTextContent(sdf.format(content));
        return this;
    }

    @Override
    public FeedXmlBuilder.Taganizer setAttribute(String name, String value) {
        this.node.getAttributes().getNamedItem(name).setTextContent(value);
        return this;
    }

    @Override
    public FeedXmlBuilder.Taganizer setAttribute(String name, Date value) {
        this.node.getAttributes().getNamedItem(name).setTextContent(sdf.format(value));
        return this;
    }

    @Override
    public FeedXmlBuilder.Taganizer createChild(String name) {
        Element newElement = document.createElement("name");
        TaganizerImpl taganizer = new TaganizerImpl(document, newElement);
        node.appendChild(newElement);
        return taganizer;
    }

    @Override
    public FeedXmlBuilder.Taganizer createChildAndSetContent(String tagName, String content) {
        FeedXmlBuilder.Taganizer child = createChild(tagName);
        child.setContent(content);
        return child;
    }

    @Override
    public FeedXmlBuilder.Taganizer createChildAndSetContent(String tagName, Date content) {
        FeedXmlBuilder.Taganizer child = createChild(tagName);
        child.setContent(content);
        return child;
    }

}
