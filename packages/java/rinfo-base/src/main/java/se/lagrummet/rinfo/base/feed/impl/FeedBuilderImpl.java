package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.CopyFeed;
import se.lagrummet.rinfo.base.feed.Feed;
import se.lagrummet.rinfo.base.feed.type.DocumentUrl;
import se.lagrummet.rinfo.base.feed.type.Md5Sum;

/**
* Created by christian on 5/21/15.
*/
public class FeedBuilderImpl implements CopyFeed.FeedBuilder {
    @Override
    public void addEntry(Feed.EntryId entryId, DocumentUrl documentURL, Md5Sum md5sum, Long length, String type) {
        Feed.Content content = new ContentImpl(Feed.ContentKind.Source, md5sum, length, documentURL, type);
    }
}
