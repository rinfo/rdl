package se.lagrummet.rinfo.base.feed;

import se.lagrummet.rinfo.base.feed.exceptions.EntryIdNotFoundException;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedDocumentUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedFeedUrlException;
import se.lagrummet.rinfo.base.feed.impl.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
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
        if (params.length!=2) {
            System.out.println(USAGE_TEXT);
            System.exit(-1);
            return;
        }

        try {
            String command = params[0];
            String url = params[1];
            if (command.equalsIgnoreCase("list"))
                list(url);
            else if (command.equalsIgnoreCase("copy"))
                copy(url);
            else
                System.out.println("Unknown command '"+command+"'");
        } catch (ParserConfigurationException | TransformerException | IOException | FailedToReadFeedException | MalformedDocumentUrlException | EntryIdNotFoundException | MalformedFeedUrlException e) {
            e.printStackTrace();
            System.exit(-1);
            return;
        }
        System.exit(0);
    }

    private static void copy(String url) throws IOException, FailedToReadFeedException, EntryIdNotFoundException, MalformedFeedUrlException, MalformedDocumentUrlException, ParserConfigurationException, TransformerException {
        Report report = new ReportImpl();
        ResourceLocatorImpl resourceLocator = new ResourceLocatorImpl(new URL(url), report);
        Parser parser = new XmlParserImpl(resourceLocator);
        CopyFeed copyFeed = new CopyFeedImpl(resourceLocator, parser);
        copyFeed.copy(UrlResource.startFeed(url), ".", report);
        report.print();
    }

    private static void list(String url) throws MalformedURLException, FailedToReadFeedException {
        Report report = new ReportImpl();
        Parser parser = new XmlParserImpl(new ResourceLocatorImpl(new URL(url), report));
        Parser.FeedBuilder parse = parser.parse(UrlResource.startFeed(url), report);

        System.out.println("*** Parse feed "+url+" ***");
        System.out.println("Id: "+parse.getId());
        System.out.println("Author uri: "+parse.getAuthorURI());
        System.out.println("Entries: ");
        int count = 0;
        for (Parser.EntryBuilder entryBuilder : parse.getEntries()) {
            count++;
            System.out.println("  EntryId: "+entryBuilder.getId());
        }
        System.out.println("Found "+count+" entries in feed");

        report.print();
    }

}
