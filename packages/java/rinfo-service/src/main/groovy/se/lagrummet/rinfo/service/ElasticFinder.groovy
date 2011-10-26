package se.lagrummet.rinfo.service

import groovy.util.logging.Slf4j as Log

import org.restlet.Context
import static org.restlet.data.CharacterSet.UTF_8
import org.restlet.data.MediaType
import org.restlet.data.Reference
import org.restlet.data.Status
import org.restlet.Request
import org.restlet.Response
import org.restlet.representation.StringRepresentation
import org.restlet.representation.Representation
import org.restlet.resource.Finder
import org.restlet.resource.Get
import org.restlet.resource.ServerResource

import org.elasticsearch.action.search.SearchPhaseExecutionException

import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.SerializationConfig


@Log
class ElasticFinder extends Finder {

    ElasticQuery elasticQuery

    def jsonMapper

    ElasticFinder(Context context, ElasticQuery elasticQuery) {
        super(context)
        this.elasticQuery = elasticQuery
        jsonMapper = new ObjectMapper()
        jsonMapper.configure(
                SerializationConfig.Feature.INDENT_OUTPUT, true)
    }

    @Override
    ServerResource find(Request request, Response response) {
        final String docType = request.attributes["docType"]
        def data = null
        def status = null
        try {
            data = elasticQuery.search(docType, request.resourceRef)
        } catch (Exception e) {
            data = [type: "Error"]
            log.error "Caught exception during query.", e
            if (e.cause instanceof SearchPhaseExecutionException) {
                status = Status.CLIENT_ERROR_BAD_REQUEST
                data.description = e.cause.message
            } else {
                status = Status.SERVER_ERROR_INTERNAL
                data.description = e.message
            }
        }
        return toResource(data, status)
    }

    def toResource(final Map data, final Status status=null) {
        if (data == null)
            return null
        return new ServerResource() {
            @Get("json")
            Representation asJSON() {
                def jsonStr = jsonMapper.writeValueAsString(data)
                def mediaType = MediaType.APPLICATION_JSON
                if (status != null) {
                    setStatus(status)
                }
                return new StringRepresentation(jsonStr, mediaType, null, UTF_8)
            }
        }
    }

}
