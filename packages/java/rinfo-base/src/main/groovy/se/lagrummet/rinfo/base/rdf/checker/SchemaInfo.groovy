package se.lagrummet.rinfo.base.rdf.checker

import java.util.regex.Pattern

import org.apache.commons.io.IOUtils

import se.lagrummet.rinfo.base.rdf.Describer
import se.lagrummet.rinfo.base.rdf.Description

import org.openrdf.repository.Repository

import org.codehaus.jackson.map.ObjectMapper


class SchemaInfo {

    def classes = new HashSet<String>()
    def propertyMap = new HashMap<String, PropertyInfo>()

    private mapper = new ObjectMapper()

    void loadConfig(String jsonPath) {
        def inStream = new FileInputStream(new File(jsonPath))
        try {
            loadConfig(inStream)
        } finally {
            inStream.close()
        }
    }

    void loadConfig(InputStream inStream) {
        def config = mapper.readValue(inStream, Map)
        configure(config)
    }

    void configure(Map config) {

        for (vocabEntry in config.entrySet()) {
            def vocab = vocabEntry.key
            def vocabData = vocabEntry.value

            for (propEntry in vocabData.get("properties")?.entrySet()) {
                def uri = vocab + propEntry.key
                def data = propEntry.value

                def datatype = data.get("datatype")
                if (datatype && datatype.indexOf("/") == -1)
                    datatype = Describer.XSD_NS + datatype

                def propInfo = new PropertyInfo(uri,
                        datatype,
                        data.get("reference"),
                        data.get("requireLang"),
                        data.get("strictWhitespace"),
                        makePattern(config, data),
                        makeDateConstraint(config, data))
                propertyMap.put(uri, propInfo)
            }

            for (className in vocabData.get("classes")?.keySet()) {
                classes.add(vocab + className)
            }
        }

    }

    private def makePattern(config, data) {
        def patternDefs = config.get("patterns")
        def pattern = getItem(config, data, "pattern",
                "patterns", "patternRef")
        if (pattern == null)
            return
        return Pattern.compile(pattern)
    }

    private def makeDateConstraint(config, data) {
        def dateConstraintData = getItem(config, data, "dateConstraint",
                "dateConstraints", "dateConstraintRef")
        if (dateConstraintData == null)
            return null
        return new DateConstraint(
                dateConstraintData.get("minYear"),
                dateConstraintData.get("maxDaysFromNow"),
                dateConstraintData.get("maxYearsFromNow"))
    }

    private def getItem(config, data, key, defsKey, refKey) {
        def defs = config.get(defsKey)
        return data.containsKey(key)?
                data.get(key) : (defs != null)?
                defs.get(data.get(refKey)) :
                null
    }

}
