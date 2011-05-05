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
import org.openrdf.model.URI;

import net.sf.json.JSONSerializer;


public class URIMinter {

    CoinURISpace space;
    public static final String BASE_CHAR_MAP_PATH =
            "/uriminter/unicodebasechars-scandinavian.json";

    public URIMinter(Repository repo, String spaceUri) {
        try {
            RepositoryConnection conn = repo.getConnection();
            try {
                Description desc = newDescriber(conn).newDescription(spaceUri);
                space = new CoinURISpace(desc, loadBaseCharMap(BASE_CHAR_MAP_PATH));
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
            // TODO:IMPROVE: add to Set first (subjects may occur multiple times)
            for (Description desc : describer.subjects(null, null)) {
                List<String> uris = space.coinUris(desc);
                if (uris != null) {
                    resultMap.put(desc.getAbout(), uris);
                }
            }
            return resultMap;
        } finally {
            conn.close();
        }
    }


    static class CoinURISpace {

        List<CoinTemplate> templates = new ArrayList<CoinTemplate>();
        Map<String, Map<String, String>> slugMappings =
                new HashMap<String, Map<String, String>>();

        Map<String, String> baseCharMap;

        String base;
        String fragmentSeparator;
        boolean lowerCasedTransform;
        boolean baseCharTransform;
        String spaceRepl = "_";

        CoinURISpace(Description desc, Map<String, String> baseCharMap) {
            this.baseCharMap = baseCharMap;
            base = desc.getString("coin:base");
            fragmentSeparator = desc.getString("coin:fragmentSeparator");
            for (Description tdesc : desc.getRels("coin:template")) {
                templates.add(new CoinTemplate(this, tdesc));
            }
            Description slugTransl = desc.getRel("coin:slugTransform");
            if (slugTransl != null) {
              for (Description transform : slugTransl.getRels("coin:apply")) {
                  if (transform.getAbout().equals(
                          desc.expandCurie("coin:ToLowerCase")))
                      lowerCasedTransform = true;
                  if (transform.getAbout().equals(
                          desc.expandCurie("coin:ToBaseChar")))
                      baseCharTransform = true;
              }
              String slugSpaceRepl = slugTransl.getString("coin:spaceReplacement");
              if (slugSpaceRepl != null)
                  spaceRepl = slugSpaceRepl;
            }
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
            if (this.lowerCasedTransform)
                value = value.toString().toLowerCase();
            if (this.baseCharTransform)
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

        CoinURISpace space;
        String forType;
        String uriTemplate;
        String relToBase;
        String relFromBase;
        List<CoinBinding> bindings = new ArrayList<CoinBinding>();

        CoinTemplate(CoinURISpace space, Description desc) {
            this.space = space;
            forType = desc.getObjectUri("coin:forType");
            uriTemplate = desc.getString("coin:uriTemplate");
            relToBase = desc.getObjectUri("coin:relToBase");
            relFromBase = desc.getObjectUri("coin:relFromBase");
            for (Description cmp : desc.getRels("coin:binding")) {
                bindings.add(new CoinBinding(this, cmp));
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
            for (CoinBinding binding : bindings) {
                String match = binding.findMatch(desc);
                if (match != null) {
                    matches.put(binding.variable, match);
                }
            }
            if (matches.size() < bindings.size()) {
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
            return space.base;
        }

        String buildUri(String base, Map<String, String> matches) {
            if (base == null)
                return null;
            if (uriTemplate == null) {
                return null; // TODO: one value, fragmentTemplate etc..
            }
            String expanded = uriTemplate;
            expanded = expanded.replace("{+base}", base);
            for (Map.Entry<String,String> entry : matches.entrySet()) {
                String var = "{"+entry.getKey()+"}";
                String value = space.translateValue(entry.getValue());
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

    static class CoinBinding {

        CoinTemplate tplt;
        String property;
        String variable;
        String slugFrom;

        CoinBinding(CoinTemplate tplt, Description desc) {
            this.tplt = tplt;
            property = desc.getObjectUri("coin:property");
            variable = desc.getString("coin:variable");
            if (variable == null)
                variable = getLeaf(property);
            slugFrom = desc.getObjectUri("coin:slugFrom");
            if (slugFrom != null) {
                Map<String, String> slugMap = tplt.space.slugMappings.get(slugFrom);
                if (slugMap == null) {
                    slugMap = new HashMap<String, String>();
                    tplt.space.slugMappings.put(slugFrom, slugMap);
                }
                for (Description slugged : desc.getDescriber().
                        subjects(slugFrom, null)) {
                    slugMap.put(slugged.getAbout(), slugged.getString(slugFrom));
                }
            }
        }

        String findMatch(Description desc) {
            if (slugFrom != null) {
                Description rel = desc.getRel(property);
                if (rel == null)
                    return null;
                String v = rel.getString(slugFrom);
                if (v != null)
                    return v;
                Map<String, String> slugMap = tplt.space.slugMappings.get(slugFrom);
                if (slugMap != null) {
                    return slugMap.get(rel.getAbout());
                }
            } else {
                return desc.getString(property);
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
            System.out.println("Usage: <space-uri> <base-data> <doc>");
            System.exit(1);
        }
        String spaceUri = args[0];
        String spaceDataDir = args[1];
        List<String> docPaths = Arrays.asList(args).subList(2, args.length);

        //String baseCharMapPath = args[0];
        System.out.println("// Reading space data..");
        Date time = new Date();
        Repository spaceRepo = RDFUtil.slurpRdf(spaceDataDir);
        double diff = (new Date().getTime() - time.getTime()) / 1000.0;
        System.out.println("// Read space data in "+diff+" s.");

        System.out.println("// Configuring from space data..");
        time = new Date();
        URIMinter uriMinter = new URIMinter(spaceRepo, spaceUri);//, baseCharMapPath);
        spaceRepo.shutDown();
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
