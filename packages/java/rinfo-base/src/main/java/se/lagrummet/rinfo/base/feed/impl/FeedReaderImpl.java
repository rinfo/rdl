package se.lagrummet.rinfo.base.feed.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import se.lagrummet.rinfo.base.feed.CopyFeed;
import se.lagrummet.rinfo.base.feed.Feed;
import se.lagrummet.rinfo.base.feed.exceptions.EntryIdNotFoundException;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedDocumentUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedFeedUrlException;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;

import java.util.ArrayList;
import java.util.List;

/**
* Created by christian on 5/21/15.
*/
public class FeedReaderImpl implements CopyFeed.FeedReader {
    CopyFeed.FeedEntryParser parser;

    public FeedReaderImpl(CopyFeed.FeedEntryParser parser) {
        this.parser = parser;
    }

    @Override
    public void read(FeedUrl feedUrl, CopyFeed.DiscoveredEntryCollector discoveredEntryCollector) throws FailedToReadFeedException, MalformedDocumentUrlException, EntryIdNotFoundException, MalformedFeedUrlException {
        Document document = feedUrl.getDocument();
        Node root = document.getDocumentElement();

        List<Node> linkNodes = allNodes(root, Feed.LINK_NAME);
        for (Node entry : linkNodes) {
            parser.parseLinks(feedUrl, entry, discoveredEntryCollector);
        }

        List<Node> entryNodes = allNodes(root, Feed.ENTRY_NAME);
        for (Node entry : entryNodes) {
            parser.parseEntries(feedUrl, entry, discoveredEntryCollector);
        }
    }

    private List<Node> allNodes(Node root, String entryName) {
        List<Node> result = new ArrayList<>();
        NodeList entries = root.getChildNodes();
        for (int i = 0; i < entries.getLength(); i++) {
            Node entry = entries.item(i);
            if (entry.getNodeName().equals(entryName))
                result.add(entry);
        }
        return result;
    }

}
