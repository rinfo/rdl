package se.lagrummet.rinfo.store.supply

import org.restlet.Finder
import org.restlet.Handler
import org.restlet.data.Language
import org.restlet.data.MediaType
import org.restlet.data.Request
import org.restlet.data.Response
import org.restlet.data.Status
import org.restlet.resource.FileRepresentation
import org.restlet.resource.Representation
import org.restlet.resource.Resource

import se.lagrummet.rinfo.store.depot.FileDepot
import se.lagrummet.rinfo.store.depot.DepotContent


class DepotFinder extends Finder {

    FileDepot fileDepot

    DepotFinder() { super() }
    DepotFinder(context) { super(context) }

    @Override
    Handler findTarget(Request request, Response response) {
        def results = fileDepot.find(request.resourceRef.path as String)

        /* TODO: should perhaps wrap "odd" results in GoneHandler,
        SeeOtherHandler? null is "not found".
        */

        if (!results) {
            // TODO: if (results == null)? Or on an exception? EntryDeletedException?
            //    response.setStatus(Status.CLIENT_ERROR_GONE, "Gone")
            return null
        }
        // TODO: some kind of result which 303:s (use-case: resource path
        // subsumed by entry above which descibes it ("/ref/fs/sfs"..)..)
        // Also, possible to have "symlink" resources, which at some point in
        // time become owl:sameAs, and thus needs e.g. file://.../name-SYMLINK?

        // perhaps: def resource = new SupplyResource(results)
        def resource = new Resource(context, request, response)
        def reps = results.collect { makeRepresentation(it) }
        resource.variants = reps
        if (reps.size() == 1) {
            resource.represent(reps[0])
        }
        resource.negotiateContent = true
        return resource
    }

    private def makeRepresentation(DepotContent content) {
        def fileRep = new FileRepresentation(
                content.file, MediaType.valueOf(content.mediaType))
        //fileRep.modificationDate = entry.entryManifest.updated
        fileRep.modificationDate = new Date(content.file.lastModified())
        fileRep.setIdentifier(content.depotUriPath)
        if (content.lang) {
            fileRep.languages = [Language.valueOf(content.lang)]
        }
        return fileRep
    }

}
