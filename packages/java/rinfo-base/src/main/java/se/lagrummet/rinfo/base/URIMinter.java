package se.lagrummet.rinfo.base;

import java.util.*;
import java.io.*;

import org.apache.commons.io.IOUtils;

import se.lagrummet.rinfo.base.rdf.RDFUtil;
import se.lagrummet.rinfo.base.rdf.Describer;
import se.lagrummet.rinfo.base.rdf.Description;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.rio.RDFFormat;

import net.sf.json.JSONSerializer;


public class URIMinter {

    CoinScheme scheme;
    public static final String BASE_CHAR_MAP_PATH =
            "/uriminter/unicodebasechars-scandinavian.json";

    public URIMinter(Repository repo, String schemeUri) {
        try {
            RepositoryConnection conn = repo.getConnection();
            try {
                Description desc = newDescriber(conn).newDescription(schemeUri);
                scheme = new CoinScheme(desc, loadBaseCharMap(BASE_CHAR_MAP_PATH));
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                conn.close();
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> loadBaseCharMap(String jsonPath) throws IOException {
        String encoding = "UTF-8";
        InputStream charMapInStream =
                getClass().getResourceAsStream(jsonPath);
        Map<String, String> baseCharMap = null;
        try {
            baseCharMap = (Map) JSONSerializer.toJSON(
                    IOUtils.toString(charMapInStream, encoding));
        } finally {
            charMapInStream.close();
        }
        return baseCharMap;
    }

    public Describer newDescriber(RepositoryConnection conn) {
        Describer describer = new Describer(conn).
            setPrefix("coin", "http://purl.org/court/def/2009/coin#").
            // for using prop URIs as CURIEs
            setPrefix("http", "http:").
            setPrefix("https", "https:").
            setPrefix("tag", "tag:").
            setPrefix("urn", "urn:");
        return describer;
    }

    public String computeUri(Repository docRepo) throws Exception {
        for (List<String> uris : computeUris(docRepo).values()) {
            for (String uri : uris) {
                return uri;
            }
        }
        return null;
    }

    public Map<String, List<String>> computeUris(Repository docRepo)
            throws Exception {
        RepositoryConnection conn = docRepo.getConnection();
        try {
            Map<String, List<String>> resultMap =
                    new HashMap<String, List<String>>();
            Describer describer = newDescriber(conn);
            for (Description desc : describer.subjectDescriptions(null, null)) {
                List<String> uris = scheme.coinUris(desc);
                if (uris != null) {
                    resultMap.put(desc.getAbout(), uris);
                }
            }
            return resultMap;
        } finally {
            conn.close();
        }
    }


    static class CoinScheme {

        List<CoinTemplate> templates = new ArrayList<CoinTemplate>();
        Map<String, Map<String, String>> slugMappings =
                new HashMap<String, Map<String, String>>();

        Map<String, String> baseCharMap;

        String base;
        String fragmentSeparator;
        boolean lowerCasedTranslation;
        boolean baseCharTranslation;
        String spaceRepl = "_";

        CoinScheme(Description desc, Map<String, String> baseCharMap) {
            this.baseCharMap = baseCharMap;
            base = desc.getValue("coin:base");
            fragmentSeparator = desc.getValue("coin:fragmentSeparator");
            for (Description tdesc : desc.getRels("coin:template")) {
                templates.add(new CoinTemplate(this, tdesc));
            }
            Description slugTransl = desc.getRel("coin:slugTranslation");
            for (Description type : slugTransl.getTypes()) {
                if (type.getAbout().equals(
                        desc.expandCurie("coin:LowerCasedTranslation")))
                    lowerCasedTranslation = true;
                if (type.getAbout().equals(
                        desc.expandCurie("coin:BaseCharTranslation")))
                    baseCharTranslation = true;
            }
            String slugSpaceRepl = slugTransl.getValue("coin:spaceReplacement");
            if (slugSpaceRepl != null)
                spaceRepl = slugSpaceRepl;
        }

        List<String> coinUris(Description desc) {
            List<String> uris = new ArrayList<String>();
            for (CoinTemplate tplt : templates) {
                String uri = tplt.coinUri(desc);
                if (uri != null) {
                    uris.add(uri);
                }
            }
            return uris;
        }

        String translateValue(String value) {
            if (this.lowerCasedTranslation)
                value = value.toString().toLowerCase();
            if (this.baseCharTranslation)
                value = toBaseChars(value);
            if (this.spaceRepl != null)
                value = value.replace(" ", this.spaceRepl);
            return value;
        }

        String toBaseChars(String value) {
            StringBuffer sb = new StringBuffer();;
            int i = 0;
            while (i < value.length()) {
                int codePoint = value.codePointAt(i);
                i += Character.charCount(codePoint);
                String strChar = new String(Character.toChars(codePoint));
                String mappedChar = baseCharMap.get(strChar);
                sb.append(mappedChar != null ? mappedChar : strChar);
            }
            return sb.toString();
        }

    }

    static class CoinTemplate {

        CoinScheme scheme;
        String forType;
        String uriTemplate;
        String relToBase;
        String relFromBase;
        List<CoinComponent> components = new ArrayList<CoinComponent>();

        CoinTemplate(CoinScheme scheme, Description desc) {
            this.scheme = scheme;
            forType = desc.getUri("coin:forType");
            uriTemplate = desc.getValue("coin:uriTemplate");
            relToBase = desc.getUri("coin:relToBase");
            relFromBase = desc.getUri("coin:relFromBase");
            for (Description cmp : desc.getRels("coin:component")) {
                components.add(new CoinComponent(this, cmp));
            }
        }

        String coinUri(Description desc) {
            if (forType != null) {
                boolean ok = false;
                for (Description type : desc.getTypes())
                    if (type.getAbout().equals(forType)) ok = true;
                if (!ok)
                    return null;
            }
            Map<String, String> matches = new HashMap<String, String>();
            for (CoinComponent component : components) {
                String match = component.findMatch(desc);
                if (match != null) {
                    matches.put(component.variable, match);
                }
            }
            if (matches.size() < components.size()) {
                return null;
            }
            return buildUri(determineBase(desc), matches);
        }

        String determineBase(Description desc) {
            if (relToBase != null) {
                Description baseRel = desc.getRel(relToBase);
                return (baseRel != null)? baseRel.getAbout() : null;
            }
            if (relFromBase != null) {
                Description baseRev = desc.getRev(relFromBase);
                return (baseRev != null)? baseRev.getAbout() : null;
            }
            return scheme.base;
        }

        String buildUri(String base, Map<String, String> matches) {
            if (base == null)
                return null;
            if (uriTemplate == null) {
                return null; // TODO: one value, fragmentTemplate etc..
            }
            String expanded = uriTemplate;
            expanded = expanded.replace("{base}", base);
            for (Map.Entry<String,String> entry : matches.entrySet()) {
                String var = "{"+entry.getKey()+"}";
                String value = scheme.translateValue(entry.getValue());
                expanded = expanded.replace(var, value);
            }
            // TODO: if (expanded.indexOf("{") > -1)
            try {
                return new java.net.URI(base).resolve(expanded).toString();
            } catch (java.net.URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

    }

    static class CoinComponent {

        CoinTemplate tplt;
        String property;
        String variable;
        String slugFrom;

        CoinComponent(CoinTemplate tplt, Description desc) {
            this.tplt = tplt;
            property = desc.getUri("coin:property");
            variable = desc.getValue("coin:variable");
            if (variable == null)
                variable = getLeaf(property);
            slugFrom = desc.getUri("coin:slugFrom");
            if (slugFrom != null) {
                Map<String, String> slugMap = tplt.scheme.slugMappings.get(slugFrom);
                if (slugMap == null) {
                    slugMap = new HashMap<String, String>();
                    tplt.scheme.slugMappings.put(slugFrom, slugMap);
                }
                for (Description slugged : desc.getDescriber().
                        subjectDescriptions(slugFrom, null)) {
                    slugMap.put(slugged.getAbout(), slugged.getValue(slugFrom));
                }
            }
        }

        String findMatch(Description desc) {
            if (slugFrom != null) {
                Description rel = desc.getRel(property);
                if (rel == null)
                    return null;
                String v = rel.getValue(slugFrom);
                if (v != null)
                    return v;
                Map<String, String> slugMap = tplt.scheme.slugMappings.get(slugFrom);
                if (slugMap != null) {
                    return slugMap.get(rel.getAbout());
                }
            } else {
                return desc.getValue(property);
            }
            return null;
        }

        String getLeaf(String uri) {
            int frag = uri.lastIndexOf('#');
            return (frag != -1)? uri.substring(frag+1) :
                    uri.substring(uri.lastIndexOf('/')+1);
        }

    }


    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: <scheme-uri> <base-data> <doc>");
            System.exit(1);
        }
        String schemeUri = args[0];
        String schemeDataDir = args[1];
        List<String> docPaths = Arrays.asList(args).subList(2, args.length);

        //String baseCharMapPath = args[0];
        System.out.println("// Reading scheme data..");
        Date time = new Date();
        Repository schemeRepo = RDFUtil.slurpRdf(schemeDataDir);
        double diff = (new Date().getTime() - time.getTime()) / 1000.0;
        System.out.println("// Read scheme data in "+diff+" s.");

        System.out.println("// Configuring from scheme data..");
        time = new Date();
        URIMinter uriMinter = new URIMinter(schemeRepo, schemeUri);//, baseCharMapPath);
        schemeRepo.shutDown();
        diff = (new Date().getTime() - time.getTime()) / 1000.0;
        System.out.println("// Configured in "+diff+" s.");

        for (String docPath : docPaths) {
            time = new Date();
            System.out.println("// Loading <"+docPath+">..");
            Repository docRepo = RDFUtil.createMemoryRepository();
            RDFUtil.loadDataFromFile(docRepo, new File(docPath));
            for (List<String> uris : uriMinter.computeUris(docRepo).values()) {
                for (String uri : uris) {
                    System.out.println("Minted <"+uri+">");
                }
            }
            diff = (new Date().getTime() - time.getTime()) / 1000.0;
            System.out.println("// Time: "+diff+" s.");
            System.out.println();
            docRepo.shutDown();
        }

    }

}
