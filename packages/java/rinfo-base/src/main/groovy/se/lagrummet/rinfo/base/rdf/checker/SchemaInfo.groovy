package se.lagrummet.rinfo.base.checker

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

    void configure(Map config) {
        def patterns = new HashMap<String, Pattern>()

        for (entry in config.get("patterns")?.entrySet()) {
            patterns.put(entry.key, Pattern.compile(entry.value))
        }

        def prefixes = config.get("prefixes")?.clone() ?: [:]
        prefixes.put("rdf", Describer.RDF_NS)
        prefixes.put("rdfs", Describer.RDFS_NS)
        prefixes.put("owl", Describer.OWL_NS)
        prefixes.put("xsd", Describer.XSD_NS)

        for (curieDef in config.get("properties")?.entrySet()) {
            def uri = resolve(prefixes, curieDef.key)
            def data = curieDef.value
            def propInfo = new PropertyInfo(uri,
                    resolve(prefixes, data.get("datatype")),
                    data.get("reference"),
                    data.get("requireLang"),
                    patterns.get(data.get("usePattern")),
                    data.get("strictWhitespace"))
            propertyMap.put(uri, propInfo)
        }

        for (curie in config.get("classes")?.keySet()) {
            classes.add(resolve(prefixes, curie))
        }

    }

    def resolve(Map prefixes, String curie) {
        if (curie == null)
            return null
        int ic = curie.indexOf(":")
        if (ic == -1)
            return prefixes.get("") + curie
        def pfx = curie.substring(0, ic)
        def local = curie.substring(ic + 1)
        return prefixes.get(pfx) + local
    }

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

}
