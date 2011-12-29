package se.lagrummet.rinfo.base.rdf.checker


class OldReport {
    def items = []
    void add(ReportItem item) {
        items.add item
    }
    boolean isOk() {
        return items.size() == 0
    }
}


// TODO:IMPROVE: store triple for most of these (or bundle in triple + reports)

class ReportItem {
}


class WarnItem extends ReportItem {
}


class ErrorItem extends ReportItem {
}


class DatatypeErrorItem extends ErrorItem {
    def literal
    def error
    DatatypeErrorItem(literal, error) {
        this.literal = literal
        this.error = error
    }
    String toString() {
        return "DatatypeErrorItem: " +
            " literal = " + literal + ";"
            " error = " + error + ";"
    }
}


class MissingTypeWarnItem extends WarnItem {
    String uriRef
    MissingTypeWarnItem(uriRef) {
        this.uriRef = uriRef
    }
    String toString() {
        return "MissingTypeWarnItem: " +
            " uriRef = " + uriRef + ";"
    }
}

class MalformedURIRefErrorItem extends ErrorItem {
    String uriRef
    def error
    MalformedURIRefErrorItem(uriRef, error) {
        this.uriRef = uriRef
        this.error = error
    }
    String toString() {
        return "MalformedURIRefErrorItem: " +
            " uriRef = " + uriRef + ";"
            " error = " + error + ";"
    }
}


class UnknownTypeWarnItem extends WarnItem {
    String uriRef
    UnknownTypeWarnItem(uriRef) {
        this.uriRef = uriRef
    }
    String toString() {
        return "UnknownTypeWarnItem: " +
            " uriRef = " + uriRef + ";"
    }
}

class UnknownPropertyWarnItem extends WarnItem {
    String uriRef
    UnknownPropertyWarnItem(uriRef) {
        this.uriRef = uriRef
    }
    String toString() {
        return "UnknownPropertyWarnItem: " +
            " uriRef = " + uriRef + ";"
    }
}

class ExpectedReferenceErrorItem extends ErrorItem {
    def literal
    ExpectedReferenceErrorItem(literal) {
        this.literal = literal
    }
    String toString() {
        return "ExpectedReferenceErrorItem: " +
            " literal = " + literal + ";"
    }
}

class ExpectedLiteralErrorItem extends ErrorItem {
    def uriRef
    ExpectedLiteralErrorItem(uriRef) {
        this.uriRef = uriRef
    }
    String toString() {
        return "ExpectedLiteralErrorItem: " +
            " uriRef = " + uriRef + ";"
    }
}

class UnexpectedDatatypeErrorItem extends ErrorItem {
    def literal
    def expected
    UnexpectedDatatypeErrorItem(literal, expected) {
        this.literal = literal
        this.expected = expected
    }
    String toString() {
        return "UnexpectedDatatypeErrorItem: " +
            " literal = " + literal + ";"
            " expected = " + expected + ";"
    }
}

class ExpectedLangErrorItem extends ErrorItem {
    def expected
    ExpectedLangErrorItem(expected) {
        this.expected = expected
    }
    String toString() {
        return "ExpectedLangErrorItem: " +
            " expected = " + expected + ";"
    }
}

class PatternMismatchErrorItem extends ErrorItem {
    def lexical
    def pattern
    PatternMismatchErrorItem(lexical, pattern) {
        this.lexical = lexical
        this.pattern = pattern
    }
    String toString() {
        return "PatternMismatchErrorItem: " +
            " lexical = " + lexical + ";"
            " pattern = " + pattern + ";"
    }
}

class SpuriousWhiteSpaceWarnItem extends WarnItem {
    def literal
    SpuriousWhiteSpaceWarnItem(literal) {
        this.literal = literal
    }
    String toString() {
        return "SpuriousWhiteSpaceWarnItem: " +
            " literal = " + literal + ";"
    }
}

class DateConstraintWarnItem extends WarnItem {
    def constraint
    def literal
    def now
    DateConstraintWarnItem(constraint, literal, now) {
        this.constraint = constraint
        this.literal = literal
        this.now = now
    }
    String toString() {
        return "DateConstraintWarnItem: " +
            " constraint = " + constraint + ";"
            " literal = " + literal + ";"
            " now = " + now + ";"
    }
}
