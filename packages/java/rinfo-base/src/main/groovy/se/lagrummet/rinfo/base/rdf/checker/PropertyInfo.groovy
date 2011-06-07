package se.lagrummet.rinfo.base.checker

import java.util.regex.Pattern


class PropertyInfo {

    String property
    String datatype
    boolean reference
    String requireLang
    Pattern lexicalPattern
    boolean strictWhitespace

    PropertyInfo(String property,
            String datatype, Boolean reference, String requireLang,
            Pattern lexicalPattern, Boolean strictWhitespace) {
        this.property = property
        this.datatype = datatype
        this.reference = reference
        this.requireLang = requireLang
        this.lexicalPattern = lexicalPattern
        this.strictWhitespace = strictWhitespace
    }

}
