package se.lagrummet.rinfo.main.storage.log;

import org.openrdf.model.Resource;
import org.openrdf.elmo.annotations.rdf;


@rdf("http://bblfish.net/work/atom-owl/2006-06-06/#Feed")
public interface FeedEvent extends AtomBase {

    @rdf("http://www.iana.org/assignments/relation/self")
    Resource getSelf();
    void setSelf(Resource self);

    @rdf("http://www.iana.org/assignments/relation/current")
    Object getCurrent();
    void setCurrent(Object current);

}
