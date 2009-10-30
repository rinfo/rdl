package se.lagrummet.rinfo.main.storage

import javax.xml.namespace.QName

import org.apache.abdera.model.Entry
import org.apache.abdera.model.Link
import org.apache.abdera.i18n.iri.IRI

import se.lagrummet.rinfo.store.depot.PathHandler


public class EntryPathMapper {

    public static final QName DCT_IS_FORMAT_OF = new QName(
            "http://purl.org/dc/terms/",
            "isFormatOf", "dct");

    PathHandler pathHandler

    public EntryPathMapper(PathHandler pathHandler) {
        this.pathHandler = pathHandler
    }

    // TODO:? put these in a separate util class?

    public String getEnclosureSlug(Link atomLink) {
        return getEnclosureSlug(atomLink, (Entry)atomLink.getParentElement())
    }

    public String getEnclosureSlug(Link atomLink, Entry atomEntry) {
        return getEnclosureSlug(atomLink, atomEntry.getId())
    }

    public String getEnclosureSlug(Link atomLink, IRI entryId) {
        String formatOfResource = atomLink.getAttributeValue(DCT_IS_FORMAT_OF)
        if (formatOfResource != null) {
            // TODO: is this hash-part=>segment acceptable?
            String resourcePath = formatOfResource.replace("#", "/")
            String suffix = "." + pathHandler.
                    hintForMediaType(atomLink.getMimeType().toString())
            return new URI(entryId.toString()+"/").resolve(formatOfResource).getPath() + suffix
        } else {
            return computeEnclosureSlug(entryId.toURI(),
                    atomLink.resolvedHref.toURI())
        }
    }

    public String computeEnclosureSlug(URI entryUri, URI enclosureUri) {
        String entryIdBase = entryUri.getPath()
        String enclPath = enclosureUri.getPath()
        if (!enclPath.startsWith(entryIdBase)) {
            // TODO: fail with what?
            throw new RuntimeException("Entry <"+entryUri +
                    "> references <${enclosureUri}> out of its domain.")
        }
        return enclPath
    }

}
