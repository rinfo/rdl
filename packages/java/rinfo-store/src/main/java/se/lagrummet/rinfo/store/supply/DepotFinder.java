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
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

import se.lagrummet.rinfo.store.depot.DeletedDepotEntryException;
import se.lagrummet.rinfo.store.depot.DepotContent;
import se.lagrummet.rinfo.store.depot.DepotReadException;
import se.lagrummet.rinfo.store.depot.FileDepot;
import se.lagrummet.rinfo.store.depot.LockedDepotEntryException;


public class DepotFinder extends Finder {

    private FileDepot depot;

    public DepotFinder() { super(); }
    public DepotFinder(Context context) { super(context); }
    public DepotFinder(Context context, FileDepot depot) {
        this(context);
        this.depot = depot;
    }

    @Override
    public Handler findTarget(Request request, Response response) {
        List<DepotContent> results = null;
        try {
            // TODO:? E.g in a servlet container, how to handle </webapp/> base?
            // depot may not want such an "instrumental" base segment,
            // in case it should reinterpret non-full url as e.g. a tag uri..?
            // Is resourceRef.relativeRef ok? Only if we *don't* want the public ref!
            // See also resourceRef.baseRef

            String relativePath =
                    request.getResourceRef().getRelativeRef().getPath().toString();
            if (!relativePath.startsWith("/")) {
                relativePath = "/" + relativePath;
            }
            results = depot.find(relativePath);
        } catch (DeletedDepotEntryException e) {
            // TODO: Gone or 404?
            response.setStatus(Status.CLIENT_ERROR_GONE);
        } catch (LockedDepotEntryException e) {
            response.setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
        } catch (DepotReadException e) {
            throw new RuntimeException(e);
        }

        if (results==null) {
            // TODO: if (results == null)? Or on an exception? EntryDeletedException?
            //    response.setStatus(Status.CLIENT_ERROR_GONE, "Gone")
            return null;
        }
        // TODO: some kind of result which 303:s (use-case: resource path
        // subsumed by entry above which descibes it ("/ref/fs/sfs"..)..)
        // E.g.: if(404) findParentEntry, scan ENTRY-INFO/DESCRIBES.urls

        // TODO: Also, possible to have "symlink" resources, which at some
        // point in time become owl:sameAs, and thus needs e.g.
        // file://.../name-SYMLINK?

        // perhaps: new SupplyResource(results)
        Resource resource = new Resource(getContext(), request, response);
        List<Variant> reps = new ArrayList<Variant>();
        for (DepotContent content : results) {
            reps.add(makeRepresentation(content));
        }
        resource.setVariants(reps);
        resource.setNegotiateContent(true);
        /* TODO: 303 (or 307) instead of 200?
                 (For entry paths not ending with "/"? conneg before?)
        if (reps.size() > 1) {
            response.setStatus(Status.REDIRECTION_FOUND);
            //...
        }
        */
        return resource;
    }

    private FileRepresentation makeRepresentation(DepotContent content) {
        FileRepresentation fileRep = new FileRepresentation(
                content.getFile(), MediaType.valueOf(content.getMediaType()));
        //fileRep.setModificationDate(entry.getEntryManifest().getUpdated());
        fileRep.setModificationDate(new Date(content.getFile().lastModified()));
        fileRep.setIdentifier(content.getDepotUriPath());
        if (content.getLang() != null) {
            List<Language> languages = new ArrayList<Language>();
            languages.add(Language.valueOf(content.getLang()));
            fileRep.setLanguages(languages);
        }
        return fileRep;
    }

}
