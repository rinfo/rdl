package se.lagrummet.rinfo.main.storage

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.base.rdf.RDFUtil

import se.lagrummet.rinfo.store.depot.DepotContent
import se.lagrummet.rinfo.store.depot.DepotEntry


class EntryRdfReader {

    static List RDF_MIME_TYPES = [
        "application/rdf+xml",
        // "application/xhtml+xml" TODO: scan for RDFa
    ]

    static Repository readRdf(DepotEntry depotEntry) {
        def rdfContent = getRdfContent(depotEntry.findContents())
        if (rdfContent == null) {
            throw new MissingRdfContentException("Found no RDF in <${depotEntry.id}>.")
        }
        def repo = rdfContentToRepository(rdfContent)
    }

    static DepotContent getRdfContent(List<DepotContent> contents) {
        def rdfContent = null
        for (DepotContent content : contents) {
            if (isRdfContent(content)) {
                rdfContent = content
                break
            }
        }
        return rdfContent
    }

    static boolean isRdfContent(DepotContent content) {
        return RDF_MIME_TYPES.contains(content.mediaType)
    }

    static Repository rdfContentToRepository(DepotContent content) {
        def repo = RDFUtil.createMemoryRepository()
        // TODO:IMPROVE: pass logical depot as baseURI?
        RDFUtil.loadDataFromFile(repo, content.file, content.mediaType)
        return repo
    }
}
