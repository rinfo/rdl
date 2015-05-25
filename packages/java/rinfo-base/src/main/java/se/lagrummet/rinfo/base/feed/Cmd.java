package se.lagrummet.rinfo.base.feed;

import se.lagrummet.rinfo.base.feed.exceptions.EntryIdNotFoundException;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedDocumentUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedFeedUrlException;
import se.lagrummet.rinfo.base.feed.impl.CopyFeedImpl;
import se.lagrummet.rinfo.base.feed.impl.ResourceLocatorImpl;
import se.lagrummet.rinfo.base.feed.impl.UrlResource;
import se.lagrummet.rinfo.base.feed.impl.XmlParserImpl;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by christian on 5/21/15.
 */
public class Cmd {

    final static String USAGE_TEXT = "Usage:\n" +
            "  Cmd <command> <url>\n" +
            "Commands:\n" +
            "  list\n" +
            "  copy\n" +
            "Example:\n" +
            "  Cmd list http://myfeed.example/index.atom\n";

    public static void main(String[] params) {
        if (params.length==0) {
            System.out.println(USAGE_TEXT);
            System.exit(-1);
            return;
        }

        try {
            FeedUrl feedURL = FeedUrl.parse(params[0]);
            Parser parser = new XmlParserImpl(new ResourceLocatorImpl(feedURL.getUrl()));
            Parser.FeedBuilder parse = parser.parse(new UrlResource(params[0]));
            System.out.println("Parse feed "+params[0]);
            System.out.println("Id: "+parse.getId());
            System.out.println("Author uri: "+parse.getAuthorURI());
            System.out.println("Entries: ");
            for (Parser.EntryBuilder entryBuilder : parse.getEntries()) {
                System.out.println("  EntryId: "+entryBuilder.getId());
            }
        } catch (MalformedURLException | FailedToReadFeedException e) {
            e.printStackTrace();
        }
        System.exit(0);
        /*try {
            FeedUrl feedURL = FeedUrl.parse(params[0]);
            new CopyFeedImpl().copy(feedURL, new File(".").getAbsolutePath());
        } catch (MalformedURLException e) {
            System.out.println("Kunde inte tolka parameter som korrekt url '"+params[0]+"'");
            System.exit(-1);
            return;
        } catch (FailedToReadFeedException e) {
            System.out.println("Kunde inte läsa url '"+params[0]+"'");
            e.printStackTrace();
            System.exit(-1);
            return;
        } catch (MalformedDocumentUrlException | EntryIdNotFoundException | MalformedFeedUrlException e) {
            e.printStackTrace();
        }*/
    }

}
