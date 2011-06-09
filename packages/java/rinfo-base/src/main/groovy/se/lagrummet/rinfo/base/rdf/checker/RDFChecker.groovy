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

    Report check(repo, mainUri) {
        def report = new Report()
        def describer = new Describer(repo.getConnection())
        try {
            // TODO: check all/"descending" resources?
            def desc = describer.findDescription(mainUri)
            new ResourceCheck(desc, report).run()
            if (mainUri != null && desc.getType() == null) {
                report.add new MissingTypeWarnItem(mainUri)
            }
        } finally {
            describer.close()
        }
        return report
    }


    class ResourceCheck {

        Description desc
        Report report

        ResourceCheck(desc, report) {
            this.desc = desc
            this.report = report
        }

        void run() {
            checkReference(desc.about, null)
            for (entry in desc.getPropertyValuesMap().entrySet()) {
                checkValues(entry.key, entry.value)
            }
        }

        void checkValues(property, values) {
            if (property == RDF_TYPE) {
                for (value in values) {
                    checkType(value)
                }
                return
            }
            def propInfo = schemaInfo.propertyMap.get(property)
            if (propInfo == null) {
                report.add new UnknownPropertyWarnItem(property)
            } else {
                checkMultiple(propInfo, values)
            }
            for (value in values) {
                checkValue(value, propInfo)
            }
        }

        void checkMultiple(propInfo, values) {
            if (propInfo.requireLang) {
                checkRequiredLang(propInfo, values)
            }
        }

        void checkRequiredLang(propInfo, values) {
            for (literal in values) {
                if (literal.lang == propInfo.requireLang) {
                    return
                }
            }
            report.add new ExpectedLangErrorItem(propInfo.requireLang)
        }

        void checkValue(Object value, propInfo) {
            if (value instanceof RDFLiteral) {
                checkLiteral(value, propInfo)
            } else {
                checkReference(value, propInfo)
            }
        }

        void checkLiteral(RDFLiteral literal, propInfo) {
            try {
                literal.toNativeValue()
            } catch (e) {
                report.add new DatatypeErrorItem(literal, e)
                return
            }
            if (propInfo == null)
                return
            if (propInfo.reference) {
                report.add new ExpectedReferenceErrorItem(literal)
                return
            }
            if (propInfo.datatype && propInfo.datatype != literal.datatype) {
                report.add new UnexpectedDatatypeErrorItem(literal, propInfo.datatype)
                return
            }
            if (propInfo.dateConstraint) {
                def now = RDFLiteral.toGCal(new Date(), "UTC")
                if (!propInfo.dateConstraint.verify(literal.toGCal(), now)) {
                    report.add new DateConstraintWarnItem(
                            propInfo.dateConstraint, literal, now)
                }
                return
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
