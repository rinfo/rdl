package se.lagrummet.rinfo.base.checker

import java.util.regex.Pattern


class PropertyInfo {

    String property
    String datatype
    boolean reference
    String requireLang
    boolean strictWhitespace
    Pattern lexicalPattern
    DateConstraint dateConstraint

    PropertyInfo(String property,
            String datatype,
            Boolean reference,
            String requireLang,
            Boolean strictWhitespace,
            Pattern lexicalPattern,
            DateConstraint dateConstraint) {
        this.property = property
        this.datatype = datatype
        this.reference = reference
        this.requireLang = requireLang
        this.lexicalPattern = lexicalPattern
        this.strictWhitespace = strictWhitespace
        this.dateConstraint = dateConstraint
    }

}
