package se.lagrummet.rinfo.store.supply;

import java.util.*;

import org.restlet.Context;
import org.restlet.Finder;
import org.restlet.Handler;
import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.FileRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

import se.lagrummet.rinfo.store.depot.DeletedDepotEntryException;
import se.lagrummet.rinfo.store.depot.DepotContent;
import se.lagrummet.rinfo.store.depot.FileDepot;


public class DepotFinder extends Finder {

    private FileDepot fileDepot;

    public DepotFinder() { super(); }
    public DepotFinder(Context context) { super(context); }
    public DepotFinder(Context context, FileDepot fileDepot) {
        this(context);
        this.fileDepot = fileDepot;
    }

    @Override
    public Handler findTarget(Request request, Response response) {
        List<DepotContent> results = null;
        try {
            results = fileDepot.find(
                    request.getResourceRef().getPath().toString());
        } catch (DeletedDepotEntryException e) {
            // TODO: GoneHandler?
        }

        /* TODO: should perhaps wrap "odd" results in GoneHandler,
        SeeOtherHandler? null is "not found".
        */

        if (results==null) {
            // TODO: if (results == null)? Or on an exception? EntryDeletedException?
            //    response.setStatus(Status.CLIENT_ERROR_GONE, "Gone")
            return null;
        }
        // TODO: some kind of result which 303:s (use-case: resource path
        // subsumed by entry above which descibes it ("/ref/fs/sfs"..)..)
        // Also, possible to have "symlink" resources, which at some point in
        // time become owl:sameAs, and thus needs e.g. file://.../name-SYMLINK?

        // perhaps: def resource = new SupplyResource(results)
        Resource resource = new Resource(getContext(), request, response);
        List<Variant> reps = new ArrayList<Variant>();
        for (DepotContent content : results) {
            reps.add(makeRepresentation(content));
        }
        resource.setVariants(reps);
        if (reps.size() == 1) {
            try {
                resource.represent(reps.get(0));
            } catch (ResourceException e) {
                throw new RuntimeException(e); // TODO: what to do?
            }
        }
        resource.setNegotiateContent(true);
        return resource;
    }

    private FileRepresentation makeRepresentation(DepotContent content) {
        FileRepresentation fileRep = new FileRepresentation(
                content.getFile(), MediaType.valueOf(content.getMediaType()));
        //fileRep.setModificationDate(entry.getEntryManifest().getUpdated());
        fileRep.setModificationDate(new Date(content.getFile().lastModified()));
        fileRep.setIdentifier(content.getDepotUriPath());
        if (content.getLang() != null) {
            List languages = new ArrayList<Representation>();
            languages.add(Language.valueOf(content.getLang()));
            fileRep.setLanguages(languages);
        }
        return fileRep;
    }

}
