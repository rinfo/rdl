package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.*;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;
import se.lagrummet.rinfo.base.feed.util.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by christian on 5/22/15.
 */
public class FeedImpl implements Feed, FeedBuilder {

    List<Entry> entries = new LinkedList<>();
    private String id;
    private Date updated;
    private String title;
    private String authorName;
    private String authorURI;
    private String authorEMail;

    @Override public String getId() {return id;}
    @Override public Date getUpdated() {return updated;}
    @Override public String getTitle() {return title;}
    @Override public String getAuthorName() {return authorName;}
    @Override public String getAuthorURI() {return authorURI;}
    @Override public String getAuthorEMail() {return authorEMail;}

    @Override public Iterable<Entry> getEntries() {return entries;}

    public FeedImpl() {

    }

    /******************************************************/
    /* ************** FeedBuilder *********************** */
    /******************************************************/

    @Override
    public Feed build(Parser.FeedBuilder feedBuilder) {
        id = feedBuilder.getId();
        updated = feedBuilder.getUpdated();
        title = feedBuilder.getTitle();
        authorName = feedBuilder.getAuthorName();
        authorURI = feedBuilder.getAuthorURI();
        authorEMail = feedBuilder.getAuthorEMail();
        System.out.println("se.lagrummet.rinfo.base.feed.impl.FeedImpl.build id="+id);
        for (Parser.EntryBuilder entryBuilder : feedBuilder.getEntries()) {
            MyEntry entry = new MyEntry();
            entry.build(entryBuilder);
            entries.add(entry);
        }
        return this;
    }

    private class MyEntry implements Entry {
        private String id;
        private Date updated;
        private List<Content> contents = new LinkedList<>();
        private String baseUrl;
        private String summary;
        private String title;
        private Date published;

        @Override public String getId() {return id;}
        @Override public Iterable<Content> getContentList() {return contents;}
        @Override public Date getUpdated() {return updated;}
        @Override public String getSummary() {return summary;}
        @Override public String getTitle() {return title;}
        @Override public Date getPublished() {return published;}

        @Override
        public boolean hasContent() {
            return !contents.isEmpty();
        }

        @Override
        public boolean containsOnlyFeedOfFeed() {
            return contents.size() == 1 && contents.get(0).getContentKind() == ContentKind.FeedOfFeed;

        }

        public void build(Parser.EntryBuilder entryBuilder) {
            id = entryBuilder.getId();
            baseUrl = entryBuilder.getBaseUrl();
            updated = entryBuilder.getUpdated();
            summary = entryBuilder.getSummary();
            title = entryBuilder.getTitle();
            published = entryBuilder.getPublished();

            for (Parser.EntryContentBuilder contentBuilder : entryBuilder.getContents()) {
                MyContent content = new MyContent();
                content.build(baseUrl, contentBuilder);
                contents.add(content);
            }
        }
    }

    private class MyContent implements Content {
        private ContentKind contentKind;
        private Md5Sum md5Sum;
        private Long length;
        private DocumentUrl documentUrl;
        private String type;
        public MyContent() {}

        @Override public ContentKind getContentKind() {return contentKind;}
        @Override public Md5Sum getMd5Sum() {return md5Sum;}
        @Override public Long getLength() {return length;}
        @Override public DocumentUrl getDocumentUrl() {return documentUrl;}
        @Override public String getType() {return type;}

        @Override
        public ResourceLocator.Resource asResource() {
            if (documentUrl==null)
                throw new NullPointerException("documentUrl is null");
            if (length==null)
                return UrlResource.entry(documentUrl.toString(), md5Sum);
            return UrlResource.entry(documentUrl.toString(), md5Sum, length.intValue());
        }

        public void build(String baseUrl, Parser.EntryContentBuilder contentBuilder) {
            contentKind = ContentKind.parse(contentBuilder.getType(), contentBuilder.isAlternate());
            md5Sum = Md5Sum.create(contentBuilder.getMd5SUM());
            try {
                length = contentBuilder.getLength()!=null?Long.parseLong(contentBuilder.getLength()):null;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (contentBuilder.getSource()!=null) {
                //System.out.println("Found content "+contentBuilder.getSource()+" baseUrl="+baseUrl);
                if (baseUrl!=null) {
                    try {
                        URL url = new URL(baseUrl);
                        documentUrl = DocumentUrl.create(Utils.parse(url, contentBuilder.getSource()).toString());
                    } catch (MalformedURLException e) {
                        //todo handle this different?
                        documentUrl = DocumentUrl.create(contentBuilder.getSource());
                    }
                } else
                    documentUrl = DocumentUrl.create(contentBuilder.getSource());
            } else
                documentUrl = null;
            type = contentBuilder.getType();
        }
    }
}
