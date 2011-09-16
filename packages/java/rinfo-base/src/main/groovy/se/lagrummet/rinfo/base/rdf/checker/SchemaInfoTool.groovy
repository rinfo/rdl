package se.lagrummet.rinfo.base.rdf.checker

import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.Describer
import se.lagrummet.rinfo.base.rdf.Description

import org.openrdf.repository.Repository

import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.SerializationConfig


class SchemaInfoTool {

    def getSchemaData(repo) {
        def describer = new Describer(repo.getConnection())
        def data = null
        try {
            data = extractCheckerData(describer)
        } finally {
            describer.close()
        }
        return data
    }

    def extractCheckerData(describer) {
        def root = [:]

        def classTypes = new HashSet(
                ["rdfs:Class", "owl:Class"].collect {
                    describer.expandCurie(it)
                })

        def propType = describer.expandCurie("rdf:Property")
        def objectPropType = describer.expandCurie("owl:ObjectProperty")
        def datatypePropType = describer.expandCurie("owl:DatatypeProperty")
        def funcPropType = describer.expandCurie("owl:FunctionalProperty")
        def annotPropType = describer.expandCurie("owl:AnnotationProperty")
        def propTypes = [propType, objectPropType, datatypePropType, funcPropType]

        for (resource in describer.subjects(null, null)) {

            def typeUris = resource.types.collect { it.about }
            if (!typeUris)
                continue
            def (vocab, term) = Describer.splitVocabTerm(resource.about)
            if (vocab == "_:")
                continue
            def rangeUri = resource.getObjectUri("rdfs:range")
            boolean hasLiteralRange = rangeUri == describer.expandCurie("rdfs:Literal")

            def vocabData = root.get(vocab)
            if (!vocabData)
                vocabData = [:]
            if (!vocabData.classes)
                vocabData.classes = [:]
            if (!vocabData.properties)
                vocabData.properties = [:]

            if (typeUris.find { it in classTypes }) {
                def classData = [:]
                vocabData.classes[term] = classData
            } else if (typeUris.find { it in propTypes }) {
                def propData = [:]
                if (!typeUris.contains(annotPropType) &&
                    objectPropType in typeUris && !hasLiteralRange)
                    propData.reference = true
                if (datatypePropType in typeUris) {
                    if (rangeUri == null) {
                        ;
                    }
                    else if (rangeUri.startsWith(Describer.XSD_NS))
                        propData.datatype = Describer.getUriTerm(rangeUri)
                    else if (!hasLiteralRange)
                        propData.datatype = rangeUri
                }
                vocabData.properties[term] = propData
            }

            if (vocabData.classes.size() == 0)
                vocabData.remove('classes')
            if (vocabData.properties.size() == 0)
                vocabData.remove('properties')
            if (vocabData.size() != 0)
                root[vocab] = vocabData
        }

        return root
    }

    void extendSchemaData(config, extensions) {
        for (ext in extensions) {
            extendData(config, ext)
        }
    }

    void extendData(Map data, Map ext) {
        ext.each { k, v ->
            if (k in data) {
                if (data[k] instanceof Map) {
                    extendData(data[k], v)
                } else {
                    data[k] = v
                }
            } else {
                data[k] = v
            }
        }
    }

    static void main(args) {
        def tool = new SchemaInfoTool()
        def repo = RDFUtil.createMemoryRepository()
        def jsonMapper = new ObjectMapper()

        def extJson = []
        for (fpath in args) {
            if (fpath.endsWith(".json"))
                extJson << jsonMapper.readValue(new File(fpath), Map)
            else
                RDFUtil.loadDataFromFile(repo, new File(fpath))
        }

        def data = tool.getSchemaData(repo)
        tool.extendSchemaData(data, extJson)

        jsonMapper.configure(
                SerializationConfig.Feature.INDENT_OUTPUT, true)
        jsonMapper.writeValue(System.out, data)

    }

}
