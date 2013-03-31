package se.lagrummet.rinfo.base.rdf.jsonld

import se.lagrummet.rinfo.base.rdf.Describer


class JSONLDContext {

    static final CONTEXT_KEY = "@context"
    static final VOCAB_KEY = "@vocab"
    static final BASE_KEY = "@base"
    static final ID_KEY = "@id"
    static final TYPE_KEY = "@type"
    static final VALUE_KEY = "@value"
    static final LANG_KEY = "@language"
    static final REV_KEY = "@reverse"
    static final CONTAINER_KEY = "@container"
    static final LIST_KEY = "@list"
    static final SET_KEY = "@set"

    static final KEY_TOKENS = [
            ID_KEY, TYPE_KEY, VALUE_KEY, REV_KEY
        ] as HashSet

    def iriTermMap = [:]
    def keyTermMap = [:]

    def tokenMap = [:]
    def vocab = null
    def base = null
    def lang = null

    JSONLDContext(Object data) {
        KEY_TOKENS.each {
            tokenMap[it] = it
        }
        parseContextData(data)
    }

    Set getTerms() {
        return keyTermMap.values()
    }

    String getIdKey() {
        return tokenMap[ID_KEY]
    }

    String getTypeKey() {
        return tokenMap[TYPE_KEY]
    }

    String getRevKey() {
        return tokenMap[REV_KEY]
    }

    void parseContextData(Object data) {
        def ref = data[CONTEXT_KEY] ?: data
        def contexts = (ref instanceof List)? ref : [ref]
        contexts.each {
            if (it instanceof String) {
                throw new RuntimeException(
                        "External context reference is unsupported.")
            }
            if (VOCAB_KEY in it) {
                addRule(VOCAB_KEY, it[VOCAB_KEY], it)
            }
            it.each { key, value ->
                addRule(key, value, it)
            }
        }
    }

    void addRule(String key, value, contextData) {
        if (key == VOCAB_KEY) {
            vocab = value
        } else if (key == BASE_KEY) {
            base = value
        } else if (key == LANG_KEY) {
            lang = value
        } else if (value in KEY_TOKENS) {
            tokenMap[value] = key
        } else if (value instanceof Map) {
            def iri = resolve(value[ID_KEY] ?: value[REV_KEY])
            def term = new Term(iri, key)
            def container = value[CONTAINER_KEY]
            term.isList = container == LIST_KEY
            term.isSet = container == SET_KEY
            assert !term.isSet || !term.isList
            term.isRev = REV_KEY in value
            term.datatype = resolve(value[TYPE_KEY], contextData)
            addTerm(term)
        } else {
            def iri = resolve(value)
            addTerm(new Term(iri, key))
        }
    }

    void addTerm(Term term) {
        if (term.iri == null) {
            def stored = keyTermMap[term.key]
            if (stored) {
                iriTermMap.remove(stored.iri)
            }
        } else {
            iriTermMap[term.iri] = term
        }
        keyTermMap[term.key] = term
    }

    String resolve(String ref, Map contextData=null) {
        if (ref == null) {
            return null
        }
        def colonAt = ref.indexOf(':')
        if (colonAt > -1) {
            def beforeColon = ref.substring(0, colonAt)
            if (beforeColon =~ /^[a-z][a-z0-9]*$/) { // curie or protocol
                def pfx = keyTermMap[beforeColon] ?: contextData?.get(beforeColon)
                if (pfx) {
                    return pfx.iri + ref.substring(colonAt+1)
                } else {
                    return ref
                }
            }
        } else {
            def term = keyTermMap[ref]
            if (term) {
                return term.iri
            } else {
                return vocab + ref
            }
        }
    }

    String toKey(String iri) {
        // TODO: if different keys per datatype, or if support for @set or @rev,
        // iriTermMap must be a list, and toKey check intended usage!
        def term = iriTermMap[iri]
        if (term) {
            return term.key
        }
        def vocabKeyPair = Describer.splitVocabTerm(iri)
        def vocab = vocabKeyPair[0]
        if (vocab == this.vocab) {
            return vocabKeyPair[1]
        } else {
            def prefix = iriTermMap[vocab]
            if (!prefix) {
                return null
            }
            return prefix.key + ":" + vocabKeyPair[1]
        }
    }

    static class Term {
        String iri
        String key
        boolean isSet
        boolean isList
        boolean isRev
        String datatype = null
        Term(String iri, String key) {
            this.iri = iri
            this.key = key
        }
    }

}
