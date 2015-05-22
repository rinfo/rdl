package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.Feed;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

/**
 * Created by christian on 5/22/15.
 */
public class ContentImpl implements Feed.Content {

    private Feed.ContentKind contentKind;
    private Md5Sum md5Sum;
    private Long length;
    private DocumentUrl documentUrl;
    private String type;

    @Override public Feed.ContentKind getContentKind() {return contentKind;}
    @Override public Md5Sum getMd5Sum() {return md5Sum;}
    @Override public Long getLength() {return length;}
    @Override public DocumentUrl getDocumentUrl() {return documentUrl;}
    @Override public String getType() {return type;}

    public ContentImpl(Feed.ContentKind contentKind, Md5Sum md5Sum, Long length, DocumentUrl documentUrl, String type) {
        this.contentKind = contentKind;
        this.md5Sum = md5Sum;
        this.length = length;
        this.documentUrl = documentUrl;
        this.type = type;
    }
}
