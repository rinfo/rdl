package se.lagrummet.rinfo.main.storage.log;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.Resource;
import org.openrdf.elmo.annotations.rdf;


@rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#DeletedEntry")
public interface DeletedEntryEvent {

    @rdf("http://www.w3.org/2008/09/rx#primarySubject")
    Resource getPrimarySubject();
    void setPrimarySubject(Resource primarySubject);

    @rdf("http://purl.org/dc/terms/isPartOf")
    Resource getIsPartOf();
    void setIsPartOf(Resource isPartOf);

    @rdf("http://purl.org/NET/c4dm/timeline.owl#at")
    XMLGregorianCalendar getAt();
    void setAt(XMLGregorianCalendar at);

    @rdf("http://www.iana.org/assignments/relation/via")
    FeedEvent getViaFeed();
    void setViaFeed(FeedEvent viaFeed);

}
