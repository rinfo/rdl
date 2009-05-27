package se.lagrummet.rinfo.service.util

import org.restlet.Application
import org.restlet.Context
import org.restlet.Restlet
import org.restlet./*resource.*/Directory

/**
 * Basic application for serving feeds from file.
 */
public class FeedApplication extends Application {

    static ROOT_URI = new File( "src/test/resources/feeds").toURI().toString()

    public FeedApplication(Context parentContext) {
        super(parentContext)
    }

    Restlet createRoot() {
    	return new Directory(context, ROOT_URI)
    }

}
