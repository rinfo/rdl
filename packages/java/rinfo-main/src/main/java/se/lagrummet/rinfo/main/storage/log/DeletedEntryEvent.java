package se.lagrummet.rinfo.main.storage.log;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.Resource;
import org.openrdf.elmo.annotations.rdf;


@rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#DeletedEntry")
public interface DeletedEntryEvent {

    @rdf("http://rdfs.org/sioc/ns#about")
    Resource getAbout();
    void setAbout(Resource about);

    @rdf("http://rdfs.org/sioc/ns#has_space")
    Resource getSpace();
    void setSpace(Resource space);

    @rdf("http://purl.org/NET/c4dm/timeline.owl#at")
    XMLGregorianCalendar getAt();
    void setAt(XMLGregorianCalendar at);

    @rdf("http://www.iana.org/assignments/relation/via")
    FeedEvent getViaFeed();
    void setViaFeed(FeedEvent viaFeed);

}
