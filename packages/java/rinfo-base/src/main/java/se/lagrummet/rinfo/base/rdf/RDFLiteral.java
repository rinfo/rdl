package se.lagrummet.rinfo.base.rdf;

import java.util.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.Literal;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
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
        org.openrdf.model.URI datatype = literal.getDatatype();
        return (datatype != null)? datatype.stringValue() : null;
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
            else if (dt.equals(XMLSchema.DATE))
                return toDate(literal.calendarValue());
            else if (dt.equals(XMLSchema.DATETIME))
                return toDate(literal.calendarValue());
        }
        return literal.stringValue();
    }

    Date toDate(XMLGregorianCalendar xmlGCal) {
        return xmlGCal.toGregorianCalendar().getTime();
    }

    public static GregorianCalendar toGCal(Date time, String timeZone) {
        GregorianCalendar grCal = new GregorianCalendar(
                TimeZone.getTimeZone(timeZone));
        grCal.setTime(time);
        return grCal;
    }

    public static RDFLiteral parseValue(Object value) {
        return new RDFLiteral(toRDFApiLiteral(value));
    }

    public static Literal toRDFApiLiteral(Object value) {
        return toRDFApiLiteral(new ValueFactoryImpl(), value);
    }

    static Literal toRDFApiLiteral(ValueFactory vf, Object value) {
        if (value instanceof Boolean) {
            return vf.createLiteral((Boolean) value);
        } else if (value instanceof Byte) {
            return vf.createLiteral((Byte) value);
        } else if (value instanceof Double) {
            return vf.createLiteral((Double) value);
        } else if (value instanceof Float) {
            return vf.createLiteral((Float) value);
        } else if (value instanceof Integer) {
            return vf.createLiteral((Integer) value);
        } else if (value instanceof Long) {
            return vf.createLiteral((Long) value);
        } else if (value instanceof Short) {
            return vf.createLiteral((Short) value);
        } else {
            if (value instanceof Date) {
                GregorianCalendar gregCal = new GregorianCalendar(
                        TimeZone.getTimeZone("GMT"));
                gregCal.setTime((Date) value);
                value = gregCal;
            }
            if (value instanceof GregorianCalendar) {
                try {
                    value = DatatypeFactory.newInstance().newXMLGregorianCalendar(
                            (GregorianCalendar) value);
                } catch (DatatypeConfigurationException e) {
                    throw new DescriptionException(e);
                }
            }
            if (value instanceof XMLGregorianCalendar) {
                return vf.createLiteral((XMLGregorianCalendar) value);
            }
        }
        return vf.createLiteral(value.toString());
    }

}
