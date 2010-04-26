package se.lagrummet.rinfo.base.rdf;

import java.util.*;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.model.Statement;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.XMLSchema;


public class Describer {

    RepositoryConnection conn;
    ValueFactory vf;

    Map<String, String> prefixes = new HashMap<String, String>();
    boolean addNS = true;
    boolean inferred = false;

    public Describer(RepositoryConnection conn) {
        this.conn = conn;
        this.vf = conn.getValueFactory();
        setPrefix("rdf", RDF.NAMESPACE);
        setPrefix("rdfs", RDFS.NAMESPACE);
        setPrefix("owl", OWL.NAMESPACE);
        setPrefix("xsd", XMLSchema.NAMESPACE);
    }

    public String getPrefix(String prefix) {
        return prefixes.get(prefix);
    }
    public Describer setPrefix(String prefix, String uri) {
        prefixes.put(prefix, uri);
        if (addNS) {
            try {
                conn.setNamespace(prefix, uri);
            } catch (RepositoryException e) {
                throw new DescriptionException(e);
            }
        }
        return this;
    }

    public String expandCurie(String curie) {
        try {
            int i = curie.indexOf(":");
            String pfx = curie.substring(0, i);
            String term = curie.substring(i+1);
            return getPrefix(pfx) + term;
        } catch (Exception e) {
            throw new DescriptionException("Malformed curie: " + curie, e);
        }
    }

    public Description newDescription(String ref) {
        return new Description(this, ref);
    }

    public List<Description> subjectDescriptions(String pCurie, String oUri) {
        List<Description> things = new ArrayList<Description>();
        for (Object ref : subjects(pCurie, oUri)) {
            things.add(newDescription((String) ref));
        }
        return things;
    }

    public List<Description> objectDescriptions(String sUri, String pCurie) {
        List<Description> things = new ArrayList<Description>();
        for (Object ref : objects(sUri, pCurie)) {
            things.add(newDescription((String) ref));
        }
        return things;
    }


    public List<Object> subjects(String pCurie, String oUri) {
        org.openrdf.model.URI p = (pCurie != null)?
                (org.openrdf.model.URI) toRef(expandCurie(pCurie)) : null;
        Value o = (oUri != null)? toRef(oUri) : null;
        try {
            RepositoryResult<Statement> stmts = conn.getStatements(null, p, o, inferred);
            List<Object> values = new ArrayList<Object>();
            while (stmts.hasNext()) {
                values.add(castValue(stmts.next().getSubject()));
            }
            stmts.close();
            return values;
        } catch (RepositoryException e) {
            throw new DescriptionException(e);
        }
    }

    public List<Object> objects(String sUri, String pCurie) {
        Resource s = (sUri != null)? toRef(sUri) : null;
        org.openrdf.model.URI p = (pCurie != null)?
                (org.openrdf.model.URI) toRef(expandCurie(pCurie)) : null;
        try {
            RepositoryResult<Statement> stmts = conn.getStatements(s, p, null, inferred);
            List<Object> values = new ArrayList<Object>();
            while (stmts.hasNext()) {
                values.add(castValue(stmts.next().getObject()));
            }
            stmts.close();
            return values;
        } catch (RepositoryException e) {
            throw new DescriptionException(e);
        }
    }


    Resource curieToRef(String curie) {
        return toRef(expandCurie(curie));
    }

    Resource toRef(String uriStr) {
        if (uriStr.startsWith("_:"))
            return vf.createBNode(uriStr.substring(2));
        return vf.createURI(uriStr);
    }

    BNode blankRef() {
        return vf.createBNode();
    }

    String fromRef(Resource ref) {
        if (ref instanceof BNode)
            return "_:"+((BNode)ref).getID();
        else
            return ref.stringValue();
    }

    Value toLiteral(String value) {
        return vf.createLiteral(value);
    }

    Value toLiteral(String value, String langOrDatatype) {
        if (langOrDatatype.startsWith("@"))
            return vf.createLiteral(value, langOrDatatype.substring(1));
        else
            return vf.createLiteral(value, vf.createURI(expandCurie(langOrDatatype)));
    }

    RDFLiteral fromLiteral(Literal literal) {
        return new RDFLiteral(literal);
    }


    void addRel(String about, String curie, String uri) {
        add(toRef(about), curieToRef(curie), toRef(uri));
    }

    String addBlankRel(String about, String curie) {
        BNode ref = blankRef();
        add(toRef(about), curieToRef(curie), ref);
        return fromRef(ref);
    }

    String addBlankRev(String curie, String about) {
        BNode ref = blankRef();
        add(ref, curieToRef(curie), toRef(about));
        return fromRef(ref);
    }

    void addLiteral(String about, String curie, String value) {
        add(toRef(about), curieToRef(curie), toLiteral(value));
    }

    void addLiteral(String about, String curie, String value, String langOrDatatype) {
        add(toRef(about), curieToRef(curie), toLiteral(value, langOrDatatype));
    }

    void add(Resource s, Value p, Value o) {
        try {
            conn.add(s, (org.openrdf.model.URI)p, o);
        } catch (RepositoryException e) {
            throw new DescriptionException(e);
        }
    }


    Object castValue(Value value) {
        return (value instanceof Literal)?
                fromLiteral((Literal)value) : fromRef((Resource)value);
    }

}
