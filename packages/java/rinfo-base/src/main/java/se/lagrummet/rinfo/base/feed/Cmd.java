package se.lagrummet.rinfo.base.feed;

import se.lagrummet.rinfo.base.feed.exceptions.EntryIdNotFoundException;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedDocumentUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedFeedUrlException;
import se.lagrummet.rinfo.base.feed.impl.CopyFeedImpl;
import se.lagrummet.rinfo.base.feed.type.FeedUrl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by christian on 5/21/15.
 */
public class Cmd {

    public static void main(String[] params) {
        if (params.length==0) {
            System.out.println("Missing params");
            System.exit(-1);
            return;
        }

        try {
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
        } catch (MalformedDocumentUrlException e) {
            e.printStackTrace();
        } catch (EntryIdNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedFeedUrlException e) {
            e.printStackTrace();
        }
    }

}
