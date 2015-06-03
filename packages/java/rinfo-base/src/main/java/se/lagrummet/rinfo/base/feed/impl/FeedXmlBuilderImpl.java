package se.lagrummet.rinfo.base.feed.impl;

import se.lagrummet.rinfo.base.feed.Feed;
import se.lagrummet.rinfo.base.feed.FeedWriter;
import se.lagrummet.rinfo.base.feed.Report;

/**
 * Created by christian on 2015-05-27.
 */
public class FeedXmlBuilderImpl implements FeedWriter {
    @Override
    public void write(Feed feed, Writer writer, Report report) {
        writer.createChildAndSetContent("id", feed.getId());
        writer.createChildAndSetContent("fh.complete", "");
        writer.createChildAndSetContent("title", feed.getTitle());
        writer.createChildAndSetContent("updated", feed.getUpdated());
        Writer author = writer.createChild("author");
          author.createChildAndSetContent("name", feed.getAuthorName());
          author.createChildAndSetContent("uri", feed.getAuthorURI());
          author.createChildAndSetContent("email", feed.getAuthorEMail());

        for (Feed.Entry entry : feed.getEntries()) {
            if (entry.hasContent() && !entry.containsOnlyFeedOfFeed()) {
                Writer entryTag = writer.createChild("entry");
                entryTag.createChildAndSetContent("id", entry.getId());
                entryTag.createChildAndSetContent("updated", entry.getUpdated());
                entryTag.createChildAndSetContent("published", entry.getPublished());
                entryTag.createChildAndSetContent("title", entry.getTitle());
                entryTag.createChildAndSetContent("summary", entry.getSummary());

                for (Feed.Content content : entry.getContentList()) {
                    switch (content.getContentKind()) {
                        case Source: {
                            Writer contentTag = entryTag.createChild("content");
                            contentTag.setAttribute("src", content.getDocumentUrl().getName());
                            contentTag.setAttribute("type", content.getType());
                            if (content.getMd5Sum()!=null)
                                contentTag.setAttribute("le:md5", content.getMd5Sum().toString());
                            if (content.getLength()!=null)
                                contentTag.setAttribute("length", content.getLength().toString());
                            break;
                        }
                        case Alternate: {
                            Writer contentTag = entryTag.createChild("link");
                            contentTag.setAttribute("rel", "alternate");
                            contentTag.setAttribute("href", content.getDocumentUrl().getName());
                            contentTag.setAttribute("type", content.getType());
                            if (content.getMd5Sum()!=null)
                                contentTag.setAttribute("le:md5", content.getMd5Sum().toString());
                            if (content.getLength()!=null)
                                contentTag.setAttribute("length", content.getLength().toString());
                            break;
                        }
                        default:
                            report.getReportItem(Report.Group.Entry, entry.getId()).warning("Unknown content %1", content.getDocumentUrl());
                    }
                }
            }
        }
    } 
    








/*
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

* */







    /*    <id>tag:sfs.regeringen.se,2013:rinfo:feed:2014</id>
    <fh:complete/><title>SFS</title>
    <updated>2015-03-25T14:07:14Z</updated>
    <author>
        <name>Regeringskansliet</name>
        <uri>http://www.regeringen.se/</uri>
        <email>rinfo@regeringen.se</email>
    </author>
    <link href="simplefeed.atom" rel="self"/>
    <link href="simplefeed_next.atom" rel="prev-archive"/>
*/
}
