package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.Feed;
import se.lagrummet.rinfo.base.feed.FeedBuilder;
import se.lagrummet.rinfo.base.feed.Parser;
import se.lagrummet.rinfo.base.feed.ResourceLocator;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

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

    @Override public String getId() {return id;}
    @Override public Date getUpdated() {return updated;}

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

        @Override public String getId() {return id;}
        @Override public Iterable<Content> getContentList() {return contents;}

        public void build(Parser.EntryBuilder entryBuilder) {
            id = entryBuilder.getId();
            System.out.println("se.lagrummet.rinfo.base.feed.impl.FeedImpl.MyEntry.build id="+id);
            updated = entryBuilder.getUpdated();

            for (Parser.EntryContentBuilder contentBuilder : entryBuilder.getContents()) {
                MyContent content = new MyContent();
                content.build(contentBuilder);
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
                return UrlResource.entry(documentUrl.toString());
            return UrlResource.entry(documentUrl.toString(), length.intValue());
        }

        public void build(Parser.EntryContentBuilder contentBuilder) {
            contentKind = ContentKind.parse(contentBuilder.getType());
            md5Sum = Md5Sum.create(contentBuilder.getMd5SUM());
            try {
                length = contentBuilder.getLength()!=null?Long.parseLong(contentBuilder.getLength()):null;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            documentUrl = contentBuilder.getSource()!=null?DocumentUrl.create(contentBuilder.getSource()):null;
            type = contentBuilder.getType();
        }
    }
}
