package se.lagrummet.rinfo.main.storage.log;

import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;
import org.openrdf.elmo.annotations.rdf;

@rdf("http://rinfo.lagrummet.se/ns/2008/10/collector#Collect")
public interface CollectEvent {

    @rdf("http://purl.org/NET/c4dm/timeline.owl#start")
    XMLGregorianCalendar getStart();
    void setStart(XMLGregorianCalendar start);

    @rdf("http://purl.org/NET/c4dm/timeline.owl#end")
    XMLGregorianCalendar getEnd();
    void setEnd(XMLGregorianCalendar end);

    @rdf("http://www.iana.org/assignments/relation/via")
    Set<FeedEvent> getViaFeeds();
    void setViaFeeds(Set<FeedEvent> viaFeed);
}
