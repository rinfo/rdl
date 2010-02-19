package se.lagrummet.rinfo.main.storage.log;

import java.net.URI;
import javax.xml.datatype.XMLGregorianCalendar;
import org.openrdf.elmo.annotations.rdf;

@rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#IdentifyerError")
public interface IdentifyerErrorEvent extends ErrorEvent {

    @rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#givenUri")
    URI getGivenUri();
    void setGivenUri(URI givenUri);

    @rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#computedUri")
    URI getComputedUri();
    void setComputedUri(URI computedUri);

}
