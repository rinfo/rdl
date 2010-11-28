package se.lagrummet.rinfo.base.rdf;

public class Triple {

    private Describer describer;
    private String subject;
    private String property;
    private Object object;

    public Triple(Describer describer,
            String subject, String property, Object object) {
        this.describer = describer;
        this.subject = subject;
        this.property = property;
        this.object = object;
    }

    public Describer getDescriber() {
        return describer;
    }

    public String getSubject() {
        return subject;
    }

    public String getProperty() {
        return property;
    }

    public Object getObject() {
        return object;
    }

}
