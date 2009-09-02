package se.lagrummet.rinfo.main.storage.log;

import java.net.URI;
import javax.xml.datatype.XMLGregorianCalendar;
import org.openrdf.elmo.annotations.rdf;

@rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#Error")
public interface ErrorEvent {

    @rdf("http://www.iana.org/assignments/relation/via")
    EntryEvent getViaEntry();
    void setViaEntry(EntryEvent viaEntry);

    @rdf("http://purl.org/NET/c4dm/timeline.owl#at")
    XMLGregorianCalendar getAt();
    void setAt(XMLGregorianCalendar at);

    @rdf("http://www.w3.org/1999/02/22-rdf-syntax-ns#value")
    String getValue();
    void setValue(String value);

    /*
    @rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#expectedUri")
    URI getExpectedUri();
    void setExpectedUri(URI expectedUri);

    @rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#computedUri")
    URI getComputedUri();
    void setComputedUri(URI computedUri);
    */
}

