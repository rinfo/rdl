package se.lagrummet.rinfo.base.feed;

import se.lagrummet.rinfo.base.feed.exceptions.EntryIdNotFoundException;
import se.lagrummet.rinfo.base.feed.exceptions.FailedToReadFeedException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedDocumentUrlException;
import se.lagrummet.rinfo.base.feed.exceptions.MalformedFeedUrlException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

/**
 * Created by christian on 5/21/15.
 */
public interface CopyFeed  {

    Feed copy(ResourceLocator.Resource resource, final String targetPath, Report report) throws FailedToReadFeedException, EntryIdNotFoundException, MalformedFeedUrlException, MalformedDocumentUrlException, IOException, ParserConfigurationException, TransformerException;

}
