package se.lagrummet.rinfo.service

import org.restlet.Context
import org.restlet.Restlet
import org.restlet.data.CharacterSet
import org.restlet.data.MediaType
import org.restlet.data.Method
import org.restlet.data.Request
import org.restlet.data.Response


class RDFStoreLoaderRestlet extends Restlet {

    static final ALLOWED = new HashSet([Method.GET])
    private FileDepot depot
    private SesameLoader rdfStoreLoader

    public RDFStoreLoaderRestlet(Context context, FileDepot depot, SesameLoader rdfStoreLoader) {
        super(context)
        this.depot = depot
        this.rdfStoreLoader = rdfStoreLoader
    }

    @Override
    public void handle(Request request, Response response) {
        String feedUrl = request.getResourceRef().
                getQueryAsForm(CharacterSet.UTF_8).getFirstValue("feed")
        response.setAllowedMethods(ALLOWED)
        if (feedUrl == null) {
            response.setEntity("No feed parameter.", MediaType.TEXT_PLAIN)
            return
        }
        // FIXME: run via concurrent!
        rdfStoreLoader.readFeed([new URL(feedUrl)])
        response.setEntity("Scheduled collect of <${feedUrl}>.", MediaType.TEXT_PLAIN)
    }

}
