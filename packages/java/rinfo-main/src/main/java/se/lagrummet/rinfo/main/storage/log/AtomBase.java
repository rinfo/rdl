package se.lagrummet.rinfo.main.storage.log;

import java.net.URI;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.elmo.annotations.rdf;


public interface AtomBase {

    @rdf("http://bblfish.net/work/atom-owl/2006-06-06/#id")
    URI getId();
    void setId(URI id);

    @rdf("http://bblfish.net/work/atom-owl/2006-06-06/#updated")
    XMLGregorianCalendar getUpdated();
    void setUpdated(XMLGregorianCalendar updated);

    @rdf("http://bblfish.net/work/atom-owl/2006-06-06/#published")
    XMLGregorianCalendar getPublished();
    void setPublished(XMLGregorianCalendar published);

}
