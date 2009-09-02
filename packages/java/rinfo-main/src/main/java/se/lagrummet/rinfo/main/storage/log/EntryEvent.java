package se.lagrummet.rinfo.main.storage.log;

import org.openrdf.model.Resource;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.rdf;


@rdf("http://bblfish.net/work/atom-owl/2006-06-06/#Entry")
public interface EntryEvent extends AtomBase {

    @rdf("http://rdfs.org/sioc/ns#about")
    Resource getAbout(); // TODO: always fails but have to be here!
    void setAbout(Resource about);
    // TODO: Needed to get the URI back (via toString); see getAbout
    @rdf("http://rdfs.org/sioc/ns#about")
    Entity getAboutObject();

    //sioc:has_host <http://rinfo.lagrummet.se/>;

    @rdf("http://rdfs.org/sioc/ns#has_space")
    Resource getSpace();
    void setSpace(Resource space);
    @rdf("http://rdfs.org/sioc/ns#has_space")
    Entity getSpaceObject();

    @rdf("http://bblfish.net/work/atom-owl/2006-06-06/#source")
    FeedEvent getSource();
    void setSource(FeedEvent source);

    @rdf("http://bblfish.net/work/atom-owl/2006-06-06/#source")
    Resource getSourceRef();
    void setSourceRef(Resource source);

    @rdf("http://www.iana.org/assignments/relation/via")
    EntryEvent getViaEntry();
    void setViaEntry(EntryEvent viaEntry);

}
