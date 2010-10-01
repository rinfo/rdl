package se.lagrummet.rinfo.base.rdf;

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

    public String getValue(String curie) {
        RDFLiteral literal = getLiteral(curie);
        return (literal != null)? literal.toString() : null;
    }

    public Object getNative(String curie) {
        RDFLiteral literal = getLiteral(curie);
        return (literal != null)? literal.toNativeValue() : null;
    }

    public RDFLiteral getLiteral(String curie) {
        List<Object> values = getObjectValues(curie);
        return (values.size() != 0)? (RDFLiteral) values.get(0) : null;
    }

    // TODO: consolidate with describer.objectValues and make getValues/getLiterals/getUris...
    public List<Object> getObjectValues(String curie) {
        return describer.objectValues(about, curie);
    }

    public String getUri(String curie) {
        Description rel = getRel(curie);
        return (rel != null)? rel.getAbout() : null;
    }

    public Description getRel(String curie) {
        List<Description> descriptions = getRels(curie);
        return (descriptions.size() > 0)? descriptions.get(0) : null;
    }
    public List<Description> getRels(String curie) {
        return describer.objects(about, curie);
    }
    //public Description getRel(Description desc) {
    //}
    //public List<Description> getRels(Description desc) {
    //}

    public Description getRev(String curie) {
        List<Description> descriptions = getRevs(curie);
        return (descriptions.size() > 0)? descriptions.get(0) : null;
    }
    public List<Description> getRevs(String curie) {
        return describer.subjects(curie, about);
    }
    //List<Description> getRev(Description desc) {
    //}
    //List<Description> getRevs(Description desc) {
    //}

    public Description getType() {
        List<Description> types = getTypes();
        return (Description) ((types.size() > 0)? types.get(0) : null);
    }
    public List<Description> getTypes() {
        return describer.objects(about, "rdf:type");
    }


    public void addValue(String curie, Object value) {
        describer.addLiteral(about, curie, value);
    }
    public void addValue(String curie, String value, String langOrDatatype) {
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
