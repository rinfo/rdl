package se.lagrummet.rinfo.base.rdf.jsonld

import org.openrdf.repository.Repository

import se.lagrummet.rinfo.base.rdf.Describer
import se.lagrummet.rinfo.base.rdf.Description
import se.lagrummet.rinfo.base.rdf.RDFLiteral


class JSONLDSerializer {

    static final RDF_TYPE = Describer.RDF_NS + "type"
    static final XSD = Describer.XSD_NS


    JSONLDContext context
    boolean keepUnmapped
    boolean addRevs

    JSONLDSerializer(context, keepUnmapped=false, addRevs=false) {
        this.context = context
        this.keepUnmapped = keepUnmapped
        this.addRevs = addRevs
    }

    Map toJSON(Repository repo, String resourceIri) {
        def describer = new Describer(repo.getConnection())
        def item = null
        try {
            def description = describer.findDescription(resourceIri)
            if (description == null) {
                return
            }
            item = createJSON(description)
            // TODO: context get rev terms
            if (addRevs) {
                def revItems = getRevData(describer, resourceIri)
                if (revItems) {
                    item[context.revKey] = revItems
                }
            }
        } finally {
            describer.close()
        }
        return item
    }

    Map createJSON(Description description, String rootIri=null) {
        def item = [:]
        if (description.about != null && !description.about.startsWith("_:")) {
            item[context.subjectKey] = description.about
        }
        if (rootIri == description.about) {
            return item
        }
        description.propertyValuesMap.each { prop, values ->
            if (prop == RDF_TYPE) {
                def typeTokens = values.collect { toKey(it) }.findAll { it }
                if (typeTokens) {
                    item[context.typeKey] = reduceValues(context.typeKey, typeTokens)
                }
                // NOTE: we don't stop for type here, since you might want
                // *both* a type token and data about type...
            }
            def key = toKey(prop)
            if (!key) {
                return
            }
            def result = values.collect {
                valueToJSON(description.describer, key, it, rootIri ?: description.about)
            }
            boolean asSet = context.keyTermMap[key]?.isSet
            item[key] = asSet? result : reduceValues(key, result)
        }
        return item
    }

    String toKey(String iri) {
        def key = context.toKey(iri)
        return (key != null)? key : keepUnmapped? iri : null
    }

    Object valueToJSON(Describer describer, String key, Object value, String rootIri=null) {
        // TODO: improve coerce mechanics (support @iri...)
        def term = context.keyTermMap[key]
        if (value instanceof RDFLiteral) {
            return toJSONLiteral(value, term?.datatype)
        } else {
            return createJSON(describer.newDescription(value), rootIri)
        }
    }

    Object toJSONLiteral(RDFLiteral value, String coerceDatatype) {
        def dt = value.datatype
        if (dt == null) { // TODO: and coerceDatatype == null
            if (value.lang == null || value.lang == context.lang) {
                return value.toString()
            } else {
                return [(context.LITERAL_KEY): value.toString(),
                        (context.LANG_KEY): value.lang]
            }
        }
        def isXsdVocab = dt.startsWith(XSD)
        def isBool = (dt == XSD + 'boolean')
        // TODO: which number types? Automatic only if lexical meets canonical...
        def isNumber = (!isBool && isXsdVocab && dt.substring(XSD.size()) in
                        ['decimal', 'short', 'int', 'integer', 'float', 'double'])
        if (dt == coerceDatatype) {
            return (isBool || isNumber)? value.toNativeValue() : value.toString()
        } else if (isBool || isNumber) {
            return value.toNativeValue()
        } else {
            return [(context.LITERAL_KEY): value.toString(),
                    (context.DATATYPE_KEY): toKey(dt)]
        }
    }

    Object reduceValues(String key, List values) {
        if (values.size() == 1) {
            return values[0]
        } else {
            return values
        }
    }

    Map getRevData(Describer describer, String resourceIri) {
        def revItems = [:]
        def revTriples = describer.triples(null, null, resourceIri)
        for (triple in revTriples) {
            def key = toKey(triple.property)
            if (!key) {
                continue
            }
            def itemsByKey = null
            if (key in revItems) {
                itemsByKey = revItems[key]
            } else {
                itemsByKey = revItems[key] = []
            }
            itemsByKey << createJSON(describer.newDescription(triple.subject), resourceIri)
        }
        return revItems
    }

}
