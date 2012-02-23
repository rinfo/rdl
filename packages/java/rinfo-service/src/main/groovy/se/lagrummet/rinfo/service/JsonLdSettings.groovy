package se.lagrummet.rinfo.service

import groovy.util.logging.Slf4j as Log

import se.lagrummet.rinfo.base.rdf.jsonld.JSONLDContext
import se.lagrummet.rinfo.base.rdf.jsonld.JSONLDSerializer


@Log
class JsonLdSettings {

    static final XSD = JSONLDSerializer.XSD

    JSONLDContext ldContext
    Map listFramesData
    String ldContextPath

    Set keywordTerms
    Set dateTerms = new HashSet()
    Set plainStringTerms = new HashSet()
    Set refTerms = new HashSet()

    Map boostTermMap

    JsonLdSettings(ldContext, listFramesData) {
        this(ldContext, listFramesData, null)
    }

    JsonLdSettings(ldContext, listFramesData, ldContextPath) {
        this.ldContext = ldContext
        this.listFramesData = listFramesData
        this.ldContextPath = ldContextPath
        setupTermSettings()
    }

    protected void setupTermSettings() {
        keywordTerms = ["iri", "type"] as HashSet
        boostTermMap = ["identifier": 100.0, "title": 20.0]

        def xsdString = XSD + "string"
        def xsdDate = XSD + "date"
        def xsdDateTime = XSD + "dateTime"
        def stringCoercionSet = new HashSet()
        def dateCoercionSet = new HashSet()

        for (term in ldContext.terms) {
            def dt = term.datatype
            def key = term.key
            if (dt == xsdString) {
                stringCoercionSet << key
            } else if (dt == xsdDate || dt == xsdDateTime) {
                dateCoercionSet << key
            }
        }
        for (termMap in listFramesData.values()) {
            termMap.each { term, value ->
                if (term in stringCoercionSet) {
                    plainStringTerms << term
                } else if (term in dateCoercionSet) {
                        dateTerms << term
                } else if ((value instanceof Map)) {
                    if (value.containsKey('iri')) {
                        refTerms << term
                    }
                }
            }
        }
    }

    JSONLDSerializer createJSONLDSerializer() {
        return new JSONLDSerializer(ldContext, false, true)
    }

}
