package se.lagrummet.rinfo.main.storage.log;

import javax.xml.datatype.XMLGregorianCalendar;
import org.openrdf.elmo.annotations.rdf;

@rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#ChecksumError")
public interface ChecksumErrorEvent extends ErrorEvent {

    @rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#document")
    String getDocument();
    void setDocument(String document);

    @rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#givenMd5")
    String getGivenMd5();
    void setGivenMd5(String givenMd5);

    @rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#computedMd5")
    String getComputedMd5();
    void setComputedMd5(String computedMd5);

}
