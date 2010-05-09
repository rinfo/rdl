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
    String pathSep = "/"
    String extensionSep = "."

    public EntryPathMapper(PathHandler pathHandler) {
        this.pathHandler = pathHandler
    }

    public String getEnclosureSlug(Link atomLink) {
        return getEnclosureSlug(atomLink, (Entry)atomLink.getParentElement())
    }

    public String getEnclosureSlug(Link atomLink, Entry atomEntry) {
        URI entryUri = atomEntry.getId().toURI()
        String formatOfResource = atomLink.getAttributeValue(DCT_IS_FORMAT_OF)
        if (formatOfResource != null) {
            // TODO: is this hardcoded hash-part=>segment acceptable?
            def entryContainerUri = new URI(entryUri.toString()+pathSep)
            String resourcePath = formatOfResource.replace("#", pathSep)
            String suffix = extensionSep + pathHandler.hintForMediaType(
                    atomLink.getMimeType().toString())
            return entryContainerUri.resolve(resourcePath).getPath() + suffix
        } else {
            def enclosureUri = atomLink.resolvedHref.toURI()
            def slug = computeEnclosureSlug(entryUri, enclosureUri)
            if (slug == null) {
                def contentElem = atomEntry.getContentElement()
                def contentPath = (contentElem.getSrc() != null)?
                        contentElem.getResolvedSrc().toURI().getPath() : null
                slug = inferEnclosureSlug(entryUri.getPath(),
                        contentPath, enclosureUri.getPath())
            }
            if (slug == null) {
                throw new EntryPathMapperException(entryUri, enclosureUri)
            }
            return slug
        }
    }

    public String computeEnclosureSlug(URI entryUri, URI enclosureUri) {
        String entryIdBase = entryUri.getPath()
        String enclPath = enclosureUri.getPath()
        if (!enclPath.startsWith(entryIdBase)) {
            return null
        }
        return enclPath
    }

    public String inferEnclosureSlug(
            String basePath, String contentPath, String enclosurePath) {
        if (contentPath == null) contentPath = basePath
        def commonHrefBase = findCommonBase(enclosurePath, contentPath)
        if (commonHrefBase == null || commonHrefBase.equals(pathSep)) {
            return null // must share some base under root
        }
        def contentTrail = substringAfter(contentPath, commonHrefBase)
        if (contentTrail.indexOf(pathSep) > -1) {
            return null // base must be all but content "file"
        }
        def enclosureTrail = substringAfter(enclosurePath, commonHrefBase)
        if (enclosureTrail.equals("") || enclosureTrail.startsWith(pathSep)) {
            return null // enclosureTrail must be relative and non-empty
        }
        return basePath + pathSep + enclosureTrail
    }

    String findCommonBase(href1, href2) {
        def sb = new StringBuffer()
        int i = 0
        // NOTE: must make sure base is all up to a pathSep!
        int lastPathSep = 0
        while (i < href1.length() && i < href2.length() &&
                href1.charAt(i) == href2.charAt(i)) {
            i++
            if (href1.charAt(i) == pathSep) {
                lastPathSep = i
            }
        }
        return href1.substring(0, lastPathSep+1)
    }

    String substringAfter(string, base) {
        if (string.startsWith(base)) {
            return string.substring(base.length())
        }
        return string
    }

}

public class EntryPathMapperException extends RuntimeException {
    public EntryPathMapperException(URI entryUri, URI enclosureUri) {
        super("Cannot compute enclosure slug from  <"+enclosureUri +
                "> within entry <"+entryUri+">.")
    }
}

