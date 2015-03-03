package se.lagrummet.rinfo.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.restlet.Context
import org.restlet.data.CharacterSet
import org.restlet.data.MediaType
import org.restlet.data.Status
import org.restlet.Request
import org.restlet.Response
import org.restlet.representation.StringRepresentation
import org.restlet.representation.Representation
import org.restlet.resource.Finder
import org.restlet.resource.Get
import org.restlet.resource.ServerResource
import org.openrdf.repository.Repository

import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.SerializationConfig

import se.lagrummet.rinfo.base.rdf.RDFUtil
import static se.lagrummet.rinfo.base.rdf.jsonld.JSONLDContext.CONTEXT_KEY
import se.lagrummet.rinfo.base.rdf.jsonld.JSONLDSerializer


class DataFinder extends Finder {

    private final Logger logger = LoggerFactory.getLogger(DataFinder.class)

    Repository repo
    JsonLdSettings jsonLdSettings
    String baseUri
    String constructQueryText
    String constructQueryPath
    JSONLDSerializer jsonLdSerializer
    ObjectMapper jsonMapper = new ObjectMapper()

    DataFinder(Context context,
            Repository repo, JsonLdSettings jsonLdSettings,
            String baseUri, String constructQueryPath) {
        super(context)
        this.baseUri = baseUri
        this.repo = repo
        this.jsonLdSettings = jsonLdSettings
        this.constructQueryPath = constructQueryPath
        this.constructQueryText = getClass().getResourceAsStream(
                constructQueryPath).getText("utf-8")

        jsonLdSerializer = jsonLdSettings.createJSONLDSerializer()
        jsonMapper.configure(
                SerializationConfig.Feature.INDENT_OUTPUT, true)
    }

    @Override
    ServerResource find(Request request, Response response) {
        final path = request.attributes["path"] ?: ""
        int dollarAt = path.indexOf('$')
        final requestPath = dollarAt > -1?
                path.substring(0, dollarAt) + "#" + path.substring(dollarAt + 1, path.size()) :
                path
        final resourceUri = baseUri + requestPath

        return new ServerResource() {

            @Get("n3|ttl|txt")
            Representation asN3() {
                //return getRepr(MediaType.APPLICATION_RDF_TURTLE)
                return getRepr(MediaType.TEXT_PLAIN, "application/x-turtle")
            }

            @Get("rdf")
            Representation asRdfXml() {
                return getRepr(MediaType.APPLICATION_RDF_XML)
            }

            @Get("xml")
            Representation asXml() {
                return getRepr(MediaType.TEXT_XML,
                        MediaType.APPLICATION_RDF_XML.toString())
            }

            @Get("json")
            Representation asJSON() {
                return getRepr(MediaType.APPLICATION_JSON)
            }

            def getRepr(mediaType) {
                return getRepr(mediaType, mediaType.toString())
            }

            def getRepr(mediaType, mediaTypeStr) {
                def beforeDate = new Date()
                def itemRepo = getRichRDF(resourceUri)

                logger.info("getRichRDF for resourceUri: '" + resourceUri + "' ("+constructQueryPath+") took " + ((new Date()).getTime()-beforeDate.getTime())/1000 + "s")
                if (itemRepo == null) {
                    setStatus(Status.CLIENT_ERROR_NOT_FOUND)
                    return null
                }

                itemRepo = GraphCleanUtil.filterRepo(itemRepo, repo, "http://purl.org/dc/terms/title", resourceUri)

                def rdfRepr = serializeRDF(itemRepo, resourceUri, mediaTypeStr)
                return new StringRepresentation(rdfRepr, mediaType,
                        null, new CharacterSet("utf-8"))
            }

        }

    }

    /**
     * Create an RDF representation based on the data for the given resource,
     * with added contextual data for relevant incoming and outgoing relations.
     */
    Repository getRichRDF(String resourceUri) {
        def conn = repo.getConnection()
        try {
            return RDFUtil.constructQuery(conn, constructQueryText,
                    ["current": conn.valueFactory.createURI(resourceUri)])
        } finally {
            conn.close()
        }
    }

    String serializeRDF(Repository itemRepo, String resourceUri, String mediaType) {
        if (mediaType == "application/json") {
            def json = jsonLdSerializer.toJSON(itemRepo, resourceUri)
            if (json != null) {
                json = [(CONTEXT_KEY): jsonLdSettings.ldContextPath] + json
            }
            return jsonMapper.writeValueAsString(json)
        }
        def outStream = new ByteArrayOutputStream()
        try {
            RDFUtil.serialize(itemRepo, mediaType, outStream)
            return outStream.toString("UTF-8")
        } finally {
            outStream.close()
        }
    }
}
