package se.lagrummet.rinfo.service

import groovy.transform.CompileStatic
import groovy.util.logging.Commons as Log
import org.restlet.Context
import org.restlet.data.Parameter

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

import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.SerializationConfig


@CompileStatic
@Log
class ElasticFinder extends Finder {
    static final String[] simpleFields = ['q', 'type', '_stats', '_page', '_pageSize']
    ElasticQuery elasticQuery
    SimpleElasticQuery elasticQuerySimple
    ObjectMapper jsonMapper

    ElasticFinder(Context context, ElasticQuery elasticQuery, SimpleElasticQuery simpleElasticQuery = null) {
        super(context)
        this.elasticQuery = elasticQuery
        this.elasticQuerySimple = simpleElasticQuery
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
            if (isSimpleQuery(request.resourceRef)) {
            //if (false) {
                data = elasticQuerySimple.search(docType, request.resourceRef)
            } else {
                data = elasticQuery.search(docType, request.resourceRef)
            }
        } catch (Exception e) {
            data = [type: "Error"]
            log.error "Caught exception during query.", e
            status = Status.SERVER_ERROR_INTERNAL
            data.description = e.message
        }
        return toResource(data, status)
    }

    ServerResource toResource(final Map data, final Status status=null) {
        if (data == null)
            return null
        return new ServerResource() {
            @Get("json")
            Representation asJSON() {
/*
                println '********************************** DATA *************************************************'
                println data
                println '*****************************************************************************************'
*/
                def jsonStr = jsonMapper.writeValueAsString(data)
/*
                println '---------------------------------- JSON ------------------------------------------------'
                println jsonStr
                println '----------------------------------------------------------------------------------------'
*/
                def mediaType = MediaType.APPLICATION_JSON
                if (status != null) {
                    setStatus(status)
                }
                return new StringRepresentation(jsonStr, mediaType, null, UTF_8)
            }
        }
    }

    boolean isSimpleQuery(Reference ref) {
        def queryNames = ref.getQueryAsForm(UTF_8)
        def returnVal = true
        queryNames.each {
            if (!simpleFields.contains((it as Parameter).getName()))
                returnVal = false
        }
        return returnVal
    }

}
