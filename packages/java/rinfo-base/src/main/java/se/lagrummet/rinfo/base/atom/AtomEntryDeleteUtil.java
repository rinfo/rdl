package se.lagrummet.rinfo.base.atom;

import java.util.*;
import java.net.URISyntaxException;

import javax.xml.namespace.QName;

import org.apache.abdera.model.AtomDate;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.ext.sharing.SharingHelper;
import org.apache.abdera.ext.sharing.Sync;


public class AtomEntryDeleteUtil {

    public static final QName ENTRY_EXT_GDATA_DELETED = new QName(
            "http://schemas.google.com/g/2005", "deleted", "gd");

    public static final QName FEED_EXT_TOMBSTONE = new QName(
            "http://purl.org/atompub/tombstones/1.0", "deleted-entry", "at");
    public static final String TOMBSTONE_REF = "ref";
    public static final String TOMBSTONE_WHEN = "when";

    public static Map<IRI, AtomDate> getDeletedMarkers(Feed feed)
            throws URISyntaxException {
        Map<IRI, AtomDate> deletedMap = new HashMap<IRI, AtomDate>();
        List<Element> tombstones = feed.getExtensions(FEED_EXT_TOMBSTONE);
        for (Element elem : tombstones) {
            deletedMap.put(new IRI(elem.getAttributeValue(TOMBSTONE_REF)),
                    new AtomDate(elem.getAttributeValue(TOMBSTONE_WHEN)));
        }
        for (Entry entry : feed.getEntries()) {
            if (isDeleted(entry)) {
                deletedMap.put(entry.getId(),
                        entry.getUpdatedElement().getValue());
            }
        }
        return deletedMap;
    }

    public static boolean isDeleted(Entry entry) {
        if (entry.getExtension(ENTRY_EXT_GDATA_DELETED) != null) {
            return true;
        }
        Sync sync = SharingHelper.getSync(entry);
        if (sync != null) {
            return sync.isDeleted();
        }
        return false;
    }

}
