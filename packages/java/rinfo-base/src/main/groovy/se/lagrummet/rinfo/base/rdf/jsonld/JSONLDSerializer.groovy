package se.lagrummet.rinfo.base.rdf.jsonld

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.base.rdf.Describer
import se.lagrummet.rinfo.base.rdf.Description
import se.lagrummet.rinfo.base.rdf.RDFLiteral


class JSONLDSerializer {

    static final VOCAB_KEY = "@vocab"
    static final TYPE_KEY = "@type"
    static final SUBJECT_KEY = "@subject"

    protected static RDF_TYPE = Describer.RDF_NS + "type"

    Map contextMap
    def typeKey = TYPE_KEY
    def subjectKey = SUBJECT_KEY
    def iriTermMap = [:]
    def vocab = null
    def addRevs = true

    JSONLDSerializer(contextMap) {
        this.contextMap = contextMap
        contextMap.each { key, value ->
            if (key == VOCAB_KEY)
                this.vocab = value
            //else if (key == "@coerce")
            else if (value == TYPE_KEY)
                typeKey = key
            else if (value == SUBJECT_KEY)
                subjectKey = key
            else
                iriTermMap[value] = key
        }
    }

    def toJSON(Repository repo, String resourceIri) {
        def describer = new Describer(repo.getConnection())
        def item = null
        try {
            def description = describer.findDescription(resourceIri)
            if (description == null)
                return
            item = toJSON(description)
            if (addRevs) {
                def revItems = getRevData(describer, resourceIri)
                if (revItems) {
                    item['@rev'] = revItems
                }
            }
        } finally {
            describer.close()
        }
        return item
    }

    def toJSON(Description description, rootIri=null) {
        def item = [:]
        if (!description.about.startsWith("_:"))
            item[subjectKey] = description.about
            if (rootIri == description.about) {
                return item
            }
        description.propertyValuesMap.each { prop, values ->
            if (prop == RDF_TYPE) {
                def result = values.collect { toKey(it) }
                if (result.size() == 1)
                    result = result[0]
                item[typeKey] = result
            } else {
                def key = toKey(prop)
                def result = values.collect { valueToJSON(description.describer, it, rootIri) }
                if (result.size() == 1)
                    result = result[0]
                item[key] = result
            }
        }
        return item
    }

    def toKey(String prop) {
        def term = iriTermMap[prop]
        if (term)
            return term
        def vocabKeyPair = Describer.splitVocabTerm(prop)
        def vocab = vocabKeyPair[0]
        if (vocab == this.vocab)
            return vocabKeyPair[1]
        else
            return iriTermMap[vocab] + ":" + vocabKeyPair[1]
    }

    def valueToJSON(Describer describer, value, rootIri=null) {
        if (value instanceof RDFLiteral) {
            // TODO: simplify booleans, numbers and coerced
            return (value.datatype == null)? value.toString() : [
                    "@datatype": Describer.splitVocabTerm(value.datatype)[1],
                    "@literal": value.toString()
                ]
        } else {
            return toJSON(describer.findDescription(value), rootIri)
        }
    }

    def getRevData(Describer describer, String resourceIri) {
        def revItems = [:]
        def revTriples = describer.triples(null, null, resourceIri)
        for (triple in revTriples) {
            def key = toKey(triple.property)
            def itemsByKey = null
            if (key in revItems) {
                itemsByKey = revItems[key]
            } else {
                itemsByKey = revItems[key] = []
            }
            itemsByKey << toJSON(describer.findDescription(triple.subject), resourceIri)
        }
        return revItems
    }

}
