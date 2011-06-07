package se.lagrummet.rinfo.base.checker

import java.util.regex.Pattern

import se.lagrummet.rinfo.base.rdf.Describer
import se.lagrummet.rinfo.base.rdf.Description
import se.lagrummet.rinfo.base.rdf.RDFLiteral
import se.lagrummet.rinfo.base.rdf.RDFUtil
import se.lagrummet.rinfo.base.rdf.Triple

import org.openrdf.repository.Repository


class RDFChecker {

    int smallTextSize = 140

    protected static RDF_TYPE = Describer.RDF_NS + "type"

    def schemaInfo = new SchemaInfo()

    Report check(repo, mainUri=null) {
        def report = new Report()
        def describer = new Describer(repo.getConnection())
        try {
            boolean hasType = false
            // TODO: check all triples, not only for mainUri?
            for (triple in describer.triples(mainUri)) {
                if (triple.property == RDF_TYPE &&
                        triple.subject == mainUri) {
                    hasType = true
                }
                new TripleCheck(triple, report).run()
            }
            if (mainUri && !hasType) {
                report.add new MissingTypeWarnItem(mainUri)
            }
        } finally {
            describer.close()
        }
        return report
    }


    class TripleCheck {

        Triple triple
        Report report

        TripleCheck(triple, report) {
            this.triple = triple
            this.report = report
        }

        void run() {
            checkReference(triple.subject, null)
            if (triple.property == RDF_TYPE) {
                checkType(triple.object)
            } else {
                def propInfo = schemaInfo.propertyMap.get(triple.property)
                if (propInfo == null) {
                    report.add new UnknownPropertyWarnItem(triple.property)
                } else
                checkValue(triple.object, propInfo)
            }
        }

        void checkValue(Object v, propInfo) {
            if (v instanceof RDFLiteral) {
                checkLiteral(v, propInfo)
            } else {
                checkReference(v, propInfo)
            }
        }

        void checkLiteral(RDFLiteral literal, propInfo) {
            try {
                literal.toNativeValue()
            } catch (e) {
                report.add new DatatypeErrorItem(literal, e)
            }
            if (propInfo == null)
                return
            if (propInfo.reference) {
                report.add new ExpectedReferenceErrorItem(literal)
            }
            if (propInfo.datatype && propInfo.datatype != literal.datatype) {
                report.add new UnexpectedDatatypeErrorItem(literal, propInfo.datatype)
            }
            if (propInfo.requireLang && propInfo.requireLang != literal.lang) {
                // TODO: only if *no* o for same s and p has expected lang
                report.add new ExpectedLangErrorItem(literal, propInfo.requireLang)
            }
            if (propInfo.strictWhitespace &&
                !hasStrictWhitespace(literal.toString())) {
                report.add new SpuriousWhiteSpaceWarnItem(literal)
            }
            checkLexicalPattern(propInfo.lexicalPattern, literal.toString())
        }

        void checkReference(String uriRef, propInfo) {
            try {
                new URI(uriRef)
            } catch (e) {
                report.add new MalformedURIRefErrorItem(uriRef, e)
            }
            if (propInfo == null)
                return
            if (!propInfo.reference) {
                report.add new ExpectedLiteralErrorItem(uriRef)
            }
            checkLexicalPattern(propInfo.lexicalPattern, uriRef)
        }

        void checkLexicalPattern(Pattern pattern, String lexical) {
            if (pattern == null)
                return
            if (!pattern.matcher(lexical).matches()) {
                report.add new PatternMismatchErrorItem(lexical, pattern.toString())
            }
        }

        void checkType(String uriRef) {
            if (!schemaInfo.classes.contains(uriRef)) {
                report.add new UnknownTypeWarnItem(uriRef)
            }
        }

        boolean hasStrictWhitespace(String lexical) {
            boolean isLongText = (lexical =~ /\n/) || lexical.size() > smallTextSize
            boolean isNormalized = !(lexical =~ /^\s/ || lexical =~ /\s$/)
            return isLongText || isNormalized
        }

    }

}
