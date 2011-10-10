package se.lagrummet.rinfo.service

import org.codehaus.jackson.map.ObjectMapper


class JsonLdSettings {

    String contextPath = "/json-ld/context.json"
    protected String listFramesPath = "/json-ld/list-frames.json"

    Map contextData
    Map listFramesData

    def keywordTerms = ["iri", "type"] as HashSet
    def refTerms = new HashSet()
    def dateTerms = new HashSet()
    def plainStringTerms = new HashSet()

    def boostTermMap = ["identifier": 4.0, "title": 2.0]

    protected def mapper = new ObjectMapper()

    JsonLdSettings() {
        contextData = readJson(contextPath)
        listFramesData = readJson(listFramesPath)
        collectTermsFromFrames()
    }

    void collectTermsFromFrames() {
        def stringCoercionSet = new HashSet(contextData['@coerce']['string'])
        for (termMap in listFramesData.values()) {
            termMap.each { term, value ->
                if (!(value instanceof Map)) {
                    return // to continue
                }
                if (value['@datatype'] == 'date') {
                    dateTerms << term
                } else if (value.containsKey('iri')) {
                    refTerms << term
                } else if (term in stringCoercionSet) {
                    plainStringTerms << term
                }
            }
        }
    }

    protected Map readJson(String dataPath) {
        def inStream = getClass().getResourceAsStream(dataPath)
        try {
            return mapper.readValue(inStream, Map)
        } finally {
            inStream.close()
        }
    }

}
