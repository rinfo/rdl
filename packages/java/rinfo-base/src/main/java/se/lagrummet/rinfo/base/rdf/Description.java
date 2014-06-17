package se.lagrummet.rinfo.base.rdf;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


public class Description {

    private Describer describer;
    private String about;

    public Description(Describer describer, String about) {
        this.describer = describer;
        this.about = about;
    }

    public String getAbout() {
        return about;
    }


    public Describer getDescriber() {
        return describer;
    }

    public String expandCurie(String curie) {
        return describer.expandCurie(curie);
    }


    public RDFLiteral getLiteral(String curie) {
        Set<Object> values = getObjectValues(curie);
        for (Object value : values) return (RDFLiteral) value;
        return null;
    }

    // TODO:? rename to getLexical?
    public String getString(String curie) {
        RDFLiteral literal = getLiteral(curie);
        return (literal != null)? literal.toString() : null;
    }

    public String getLexical(String curie, ReverseSlug reverseSlug) {
        String string = null;
        try {
            Set<Object> objectValues = getObjectValues(curie);
            for (Object value : objectValues) {
                if (value instanceof RDFLiteral)  {
                    string = value.toString();
                    break;
                }
                if (value instanceof String) {
                    string = reverseSlug.lookup((String) value, getRel(curie));
                    break;
                }
            }
            if (string!=null && !string.equals(""))
                return string;
        } catch (Exception ignore) {}
        return null;
    }

    public interface ReverseSlug {
        String lookup(String url, Description rel);
    }

    // TODO:? rename to get?
    public Object getNative(String curie) {
        RDFLiteral literal = getLiteral(curie);
        return (literal != null)? literal.toNativeValue() : null;
    }

    // TODO: consolidate with describer.objectValues and make getValues/getLiterals/getUris...
    public Set<Object> getObjectValues(String curie) {
        return describer.objectValues(about, curie);
    }

    public Set<Triple> getTriples() {
        return describer.triples(about);
    }

    public Map<String, List<Object>> getPropertyValuesMap() {
        Map<String, List<Object>> propsValues = new HashMap<String, List<Object>>();
        for (Triple triple : getTriples()) {
            List<Object> values = propsValues.get(triple.getProperty());
            if (values == null) {
                values = new ArrayList<Object>();
                propsValues.put(triple.getProperty(), values);
            }
            values.add(triple.getObject());
        }
        return propsValues;
    }

    // TODO:? merge with getLexical?
    public String getObjectUri(String curie) {
        Description rel = getRel(curie);
        return (rel != null)? rel.getAbout() : null;
    }


    public Description getRel(String curie) {
        for (Description it : getRels(curie))
          return it;
        return null;
    }

    public Set<Description> getRels(String curie) {
        return describer.objects(about, curie);
    }

    public Description getRev(String curie) {
        for (Description it : getRevs(curie))
          return it;
        return null;
    }

    public Set<Description> getRevs(String curie) {
        return describer.subjects(curie, about);
    }


    public Description getType() {
        for (Description it : getTypes())
          return it;
        return null;
    }

    public Set<Description> getTypes() {
        return describer.objects(about, "rdf:type");
    }


    public void addLiteral(String curie, Object value) {
        describer.addLiteral(about, curie, value);
    }
    public void addLiteral(String curie, String value, String langOrDatatype) {
        describer.addLiteral(about, curie, value, langOrDatatype);
    }
    //public void addObject(String curie, Object value) {
    //    RDFLiteral.fromNativeValue(value);
    //}

    public Description addRel(String curie) {
        return describer.newDescription(describer.addBlankRel(about, curie));
    }
    public Description addRel(String curie, String uri) {
        describer.addRel(about, curie, uri);
        return describer.newDescription(uri);
    }

    public Description addRev(String curie) {
        return describer.newDescription(describer.addBlankRev(curie, about));
    }
    public Description addRev(String curie, String uri) {
        describer.addRel(uri, curie, about);
        return describer.newDescription(uri);
    }

    public void remove(String curie) {
        describer.remove(about, curie);
    }

    public void addType(String typeCurie) {
        describer.addRel(about, "rdf:type", describer.expandCurie(typeCurie));
    }

}
