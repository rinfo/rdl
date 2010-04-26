package se.lagrummet.rinfo.base.rdf;

import java.util.*;

import org.openrdf.model.Literal;
import org.openrdf.model.vocabulary.XMLSchema;


class RDFLiteral {

    Literal literal;

    public RDFLiteral(Literal literal) {
        this.literal = literal;
    }

    public String toString() {
        return literal.stringValue();
    }

    public String getDatatype() {
        return literal.getDatatype().stringValue();
    }

    public String getLang() {
        return literal.getLanguage();
    }

    public Object toNativeValue() {
        org.openrdf.model.URI dt = literal.getDatatype();
        if (dt != null) {
            if (dt.equals(XMLSchema.BOOLEAN))
                return literal.booleanValue();
            else if (dt.equals(XMLSchema.BYTE))
                return literal.byteValue();
            else if (dt.equals(XMLSchema.DOUBLE))
                return literal.doubleValue();
            else if (dt.equals(XMLSchema.FLOAT))
                return literal.floatValue();
            else if (dt.equals(XMLSchema.INT))
                return literal.intValue();
            else if (dt.equals(XMLSchema.LONG))
                return literal.longValue();
            else if (dt.equals(XMLSchema.SHORT))
                return literal.shortValue();
            // TODO: to Date..
            else if (dt.equals(XMLSchema.DATE))
                return literal.calendarValue();
            else if (dt.equals(XMLSchema.DATETIME))
                return literal.calendarValue();
            //return literal;
        }
        return literal.stringValue();
    }

}
