package se.lagrummet.rinfo.service

import org.restlet.Context
import org.restlet.data.CharacterSet
import org.restlet.data.Language
import org.restlet.data.MediaType
import org.restlet.Request
import org.restlet.Response
import org.restlet.representation.StringRepresentation
import org.restlet.representation.Representation
import org.restlet.resource.Finder
import org.restlet.resource.Get
import org.restlet.resource.ServerResource
import org.restlet.routing.Router

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.base.rdf.RDFUtil


class DataFinder extends Finder {

    def baseUrl
    def repo

    DataFinder(Context context, String baseUrl, Repository repo) {
        super(context)
        this.baseUrl = baseUrl
        this.repo = repo
    }

    @Override
    ServerResource find(Request request, Response response) {
        final path = request.attributes["path"]

        return new ServerResource() {

            @Get("n3|txt")
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
                return new StringRepresentation(
                        getFullRDF(path, mediaTypeStr), mediaType)
            }

        }

    }

    def getFullRDF(String path, String mediaType) {
        def url = new URL(baseUrl + path)
        def itemRepo = RDFUtil.createMemoryRepository()
        RDFUtil.loadDataFromURL(itemRepo, url, "application/rdf+xml")
        def outStream = new ByteArrayOutputStream()
        try {
            RDFUtil.serialize(itemRepo, mediaType, outStream)
        } finally {
            outStream.close()
        }
        return outStream.toString()
    }

}
