package se.lagrummet.rinfo.service

import org.restlet.Context
import org.restlet.data.CharacterSet
import org.restlet.data.Language
import org.restlet.data.MediaType
import org.restlet.data.Status
import org.restlet.Request
import org.restlet.Response
import org.restlet.representation.StringRepresentation
import org.restlet.representation.Representation
import org.restlet.resource.Finder
import org.restlet.resource.Get
import org.restlet.resource.ServerResource
import org.restlet.routing.Router

import org.openrdf.repository.Repository
import static org.openrdf.query.QueryLanguage.SPARQL
import org.openrdf.repository.util.RDFInserter

import se.lagrummet.rinfo.base.rdf.RDFUtil


class DataFinder extends Finder {

    def baseUri
    def repo

    DataFinder(Context context, Repository repo, String baseUri) {
        super(context)
        this.baseUri = baseUri
        this.repo = repo
    }

    @Override
    ServerResource find(Request request, Response response) {
        final path = request.attributes["path"]

        return new ServerResource() {

            @Get("n3|ttl|txt")
            Representation asN3() {
                //return getRepr(MediaType.TEXT_RDF_N3)
                return getRepr(MediaType.TEXT_PLAIN, "application/x-turtle")
            }

            @Get("rdf|xml")
            Representation asRdfXml() {
                //return getRepr(path, MediaType.APPLICATION_RDF_XML)
                return getRepr(MediaType.TEXT_XML,
                        MediaType.APPLICATION_RDF_XML.toString())
            }

            def getRepr(mediaType) {
                return getRepr(mediaType, mediaType.toString())
            }

            def getRepr(mediaType, mediaTypeStr) {
                def rdfRepr = getFullRDF(path, mediaTypeStr)
                if (rdfRepr == null) {
                    setStatus(Status.CLIENT_ERROR_NOT_FOUND)
                    return null
                }
                return new StringRepresentation(rdfRepr, mediaType,
                        null, new CharacterSet("utf-8"))
            }

        }

    }

    def getFullRDF(String path, String mediaType) {
        def itemRepo = RDFUtil.createMemoryRepository()
        def itemConn = itemRepo.getConnection()
        boolean empty = true
        try {
            def conn = repo.getConnection()
            try {
                def currentUri = conn.valueFactory.createURI(baseUri + path)
                def graphQuery = conn.prepareGraphQuery(SPARQL,
                        constructRelRevDataSparql)
                graphQuery.setBinding("current", currentUri)
                graphQuery.evaluate(new RDFInserter(itemConn))
            } finally {
                conn.close()
            }
            empty = itemConn.size() == 0 // itemConn.isEmpty()
        } finally {
            itemConn.close()
        }

        if (empty)
            return null

        def outStream = new ByteArrayOutputStream()
        try {
            RDFUtil.serialize(itemRepo, mediaType, outStream)
        } finally {
            outStream.close()
        }
        return outStream.toString()
    }

    String getConstructRelRevDataSparql() {
        return getClass().getResourceAsStream(
                "/construct_relrev_data.rq").getText("utf-8")
    }

}
