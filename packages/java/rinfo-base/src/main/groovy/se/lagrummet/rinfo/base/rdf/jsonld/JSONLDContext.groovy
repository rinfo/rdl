package se.lagrummet.rinfo.base.rdf.jsonld

import se.lagrummet.rinfo.base.rdf.Describer


class JSONLDContext {

    static final CONTEXT_KEY = "@context"
    static final VOCAB_KEY = "@vocab"
    static final BASE_KEY = "@base"
    static final LANG_KEY = "@language"
    static final IRI_KEY = "@iri"
    static final SUBJECT_KEY = "@subject" // DEPRECATED?
    static final TYPE_KEY = "@type"
    static final LITERAL_KEY = "@literal"
    static final DATATYPE_KEY = "@datatype"
    static final REV_KEY = "@rev" // EXPERIMENTAL

    static final TOKENS = [
            SUBJECT_KEY, IRI_KEY, TYPE_KEY, LITERAL_KEY, DATATYPE_KEY, REV_KEY
        ] as HashSet

    static final LIST_COERCE = "@list"
    static final SET_COERCE = "@set" // EXPERIMENTAL
    static final REV_COERCE = REV_KEY // EXPERIMENTAL
    static final COERCE_KEY = "@coerce"

    def iriTermMap = [:]
    def keyTermMap = [:]

    def tokenMap = [:]
    def vocab = null
    def base = null
    def lang = null

    JSONLDContext(Object data) {
        TOKENS.each {
            tokenMap[it] = it
        }
        parseContextData(data)
    }

    Set getTerms() {
        return keyTermMap.values()
    }

    String getSubjectKey() {
        return tokenMap[SUBJECT_KEY]
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
                        "External context reference unsupported.") // TODO
            }
            Map<String, List<String>> coerceMap = [:]
            addRule(VOCAB_KEY, it[VOCAB_KEY], coerceMap)
            it.each { key, value ->
                addRule(key, value, coerceMap)
            }
            completeCoercions(coerceMap)
        }
    }

    void completeCoercions(Map coerceMap) {
        coerceMap.each { key, obj ->
            def rules = (obj instanceof List)? obj : [obj]
            def term = keyTermMap[key]
            if (term == null) {
                term = new Term(vocab + key, key)
                addTerm(term)
            }
            for (rule in rules) {
                if (rule == LIST_COERCE) {
                    term.isList = true
                    assert term.isSet == false
                } else if (rule == SET_COERCE) {
                    term.isSet = true
                    assert term.isList == false
                } else if (rule == REV_COERCE) {
                    term.isRev = true
                } else {
                    term.datatype = resolve(rule)
                }
            }
        }
    }

    void addRule(String key, value, coerceMap) {
        if (key == VOCAB_KEY) {
            vocab = value
        } else if (key == BASE_KEY) {
            base = value
        } else if (key == LANG_KEY) {
            lang = value
        } else if (key == COERCE_KEY) {
            coerceMap.putAll(value)
        } else if (value in TOKENS) {
            tokenMap[value] = key
        } else {
            if (value instanceof Map) {
                def entry = value.entrySet().toList()[0]
                def iri = resolve(entry.key)
                def coercion = entry.value
                addTerm(new Term(iri, key))
                coerceMap[key] = coercion
            } else {
                def iri = resolve(value)
                addTerm(new Term(iri, key))
            }
        }
    }

    void addTerm(Term term) {
        iriTermMap[term.iri] = term
        keyTermMap[term.key] = term
    }

    String resolve(String ref) {
        def colonAt = ref.indexOf(':')
        if (colonAt > -1) {
            def beforeColon = ref.substring(0, colonAt)
            if (beforeColon =~ /^[a-z][a-z0-9]*$/) { // curie or protocol
                def pfx = keyTermMap[beforeColon]
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
