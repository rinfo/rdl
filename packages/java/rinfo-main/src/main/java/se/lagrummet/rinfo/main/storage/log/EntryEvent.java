package se.lagrummet.rinfo.main.storage.log;

import org.openrdf.model.Resource;
import org.openrdf.elmo.Entity;
import org.openrdf.elmo.annotations.rdf;


@rdf("http://bblfish.net/work/atom-owl/2006-06-06/#Entry")
public interface EntryEvent extends AtomBase {

    @rdf("http://www.w3.org/2008/09/rx#primarySubject")
    Resource getPrimarySubject(); // TODO: always fails but have to be here!
    void setPrimarySubject(Resource primarySubject);
    // TODO: Needed to get the URI back (via toString); see getPrimarySubject
    @rdf("http://www.w3.org/2008/09/rx#primarySubject")
    Entity getPrimarySubjectObject();

    @rdf("http://purl.org/dc/terms/isPartOf")
    Resource getIsPartOf();
    void setIsPartOf(Resource isPartOf);
    @rdf("http://purl.org/dc/terms/isPartOf")
    Entity getIsPartOfObject();

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
