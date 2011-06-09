package se.lagrummet.rinfo.base.checker


class Report {
    def items = []
    void add(ReportItem item) {
        items.add item
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
}


class MissingTypeWarnItem extends WarnItem {
    String uriRef
    MissingTypeWarnItem(uriRef) {
        this.uriRef = uriRef
    }
}

class MalformedURIRefErrorItem extends ErrorItem {
    String uriRef
    def error
    MalformedURIRefErrorItem(uriRef, error) {
        this.uriRef = uriRef
        this.error = error
    }
}


class UnknownTypeWarnItem extends WarnItem {
    String uriRef
    UnknownTypeWarnItem(uriRef) {
        this.uriRef = uriRef
    }
}

class UnknownPropertyWarnItem extends WarnItem {
    String uriRef
    UnknownPropertyWarnItem(uriRef) {
        this.uriRef = uriRef
    }
}

class ExpectedReferenceErrorItem extends ErrorItem {
    def literal
    ExpectedReferenceErrorItem(literal) {
        this.literal = literal
    }
}

class ExpectedLiteralErrorItem extends ErrorItem {
    def uriRef
    ExpectedLiteralErrorItem(uriRef) {
        this.uriRef = uriRef
    }
}

class UnexpectedDatatypeErrorItem extends ErrorItem {
    def literal
    def expected
    UnexpectedDatatypeErrorItem(literal, expected) {
        this.literal = literal
        this.expected = expected
    }
}

class ExpectedLangErrorItem extends ErrorItem {
    def expected
    ExpectedLangErrorItem(expected) {
        this.expected = expected
    }
}

class PatternMismatchErrorItem extends ErrorItem {
    def lexical
    def pattern
    PatternMismatchErrorItem(lexical, pattern) {
        this.lexical = lexical
        this.pattern = pattern
    }
}

class SpuriousWhiteSpaceWarnItem extends WarnItem {
    def literal
    SpuriousWhiteSpaceWarnItem(literal) {
        this.literal = literal
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
}
