package se.lagrummet.rinfo.base;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.lagrummet.rinfo.base.rdf.RDFUtil;
import se.lagrummet.rinfo.base.rdf.Describer;
import se.lagrummet.rinfo.base.rdf.Description;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import org.codehaus.jackson.map.ObjectMapper;


public class URIMinter {

    private final Logger logger = LoggerFactory.getLogger(URIMinter.class);

    CoinURISpace space;
    public static final String BASE_CHAR_MAP_PATH =
            "/uriminter/unicodebasechars-scandinavian.json";

    public URIMinter(Repository repo, String spaceUri) {
        logger.trace("spaceUri=" + spaceUri);
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
        InputStream charMapInStream =
                getClass().getResourceAsStream(jsonPath);
        Map<String, String> baseCharMap = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            baseCharMap = mapper.readValue(charMapInStream, Map.class);
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
        for (List<MintResult> results : computeUris(docRepo).values()) {
            for (MintResult result : results) {
                if (result.getUri() != null) {
                    return result.getUri();
                }
            }
        }
        return null;
    }

    public Map<String, List<MintResult>> computeUris(Repository docRepo)
            throws Exception {
        RepositoryConnection conn = docRepo.getConnection();
        try {
            Map<String, List<MintResult>> resultMap =
                    new HashMap<String, List<MintResult>>();
            Describer describer = newDescriber(conn);
            for (Description desc : describer.subjects(null, null)) {
                logger.trace("desc.about='"+desc.getAbout()+"'");
                List<MintResult> results = space.coinUris(desc, false);
                logger.trace("results.size='"+results.size()+"'");
                if (results != null) {
                    resultMap.put(desc.getAbout(), results);
                }
            }
            return resultMap;
        } finally {
            conn.close();
        }
    }

    public Map<String, List<MintResult>> computeSuggestionUris(Repository docRepo)
            throws Exception {
        RepositoryConnection conn = docRepo.getConnection();
        try {
            Map<String, List<MintResult>> resultMap =
                    new HashMap<String, List<MintResult>>();
            Describer describer = newDescriber(conn);
            for (Description desc : describer.subjects(null, null)) {
                logger.trace("desc.about='"+desc.getAbout()+"'");
                List<MintResult> results = space.coinUris(desc, true);
                logger.trace("results.size='"+results.size()+"'");
                if (results != null) {
                    resultMap.put(desc.getAbout(), results);
                }
            }

            return bestMatch(resultMap);

        } finally {
            conn.close();
        }
    }

    private Map<String, List<MintResult>> bestMatch(Map<String, List<MintResult>> resultMap) {

        final int requiredMinMatchCount = 1;

        for (Map.Entry<String, List<MintResult>> entry : resultMap.entrySet()) {
            String uri = entry.getKey();
            List<MintResult> results = entry.getValue();
            int lowestDiff = Integer.MAX_VALUE;

            for (MintResult result : results) {
                int diff = result.getRulesSize() - result.getMatchCount();
                if(diff < lowestDiff) {
                    lowestDiff = diff;
                }
            }

            List<MintResult> resultsWithBestMatch = new ArrayList<MintResult>();

            for (MintResult result : results) {
                int diff = result.getRulesSize() - result.getMatchCount();
                if(diff == lowestDiff && result.getMatchCount() >= requiredMinMatchCount) {
                    logger.trace("Adding to resultsWithBestMatch, uri: " + result.getUri() + ", rulesSize: " + result.getRulesSize() + ", matchCount: " + result.getMatchCount());
                    resultsWithBestMatch.add(result);
                }
            }

            resultMap.put(uri, resultsWithBestMatch);
        }

        return resultMap;
    }


    static class CoinURISpace {

        private final Logger logger = LoggerFactory.getLogger(CoinURISpace.class);

        List<CoinTemplate> templates = new ArrayList<CoinTemplate>();
        Map<String, Map<String, String>> slugMappings = new HashMap<String, Map<String, String>>();
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
            logger.trace(">>>>>>>>>>>>>>>>>>>>>>> SlugMappings");
            for (String key : slugMappings.keySet()) {
                Map<String, String> stringStringMap = slugMappings.get(key);
                logger.trace(" - mappings for key '"+key+"'");
                for (String key2 : stringStringMap.keySet()) {
                    String value = stringStringMap.get(key2);
                    logger.trace(key2+"="+value);
                }
            }
            logger.trace("<<<<<<<<<<<<<<<<<<<<<<< SlugMappings");
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

        /**
         * Results are ordered by the {@link MintResult#getMatchCount()}
         * property. Higher value leads to earlier (lower) index in list.
         */
        List<MintResult> coinUris(Description desc, boolean allowSuggestions) {
            logger.trace("desc.about="+desc.getAbout());
            List<MintResult> results = new ArrayList<MintResult>();
            for (CoinTemplate tplt : templates) {
                logger.trace("tplt.forType="+tplt.forType+", tplt.uriTemplate="+tplt.uriTemplate);
                MintResult result = tplt.coinUri(desc, allowSuggestions);
                if (result.getUri() != null) {
                    results.add(result);
                } else {
                    logger.trace("skipping mintresult="+result.toString());
                }
            }
            Collections.sort(results, new Comparator<MintResult>() {
                public int compare(MintResult a, MintResult b) {
                    int prioCmp = b.getPriority() - a.getPriority();
                    if (prioCmp != 0) {
                        return prioCmp;
                    }
                    return -1 * a.getMatchCount().compareTo(b.getMatchCount());
                }
            });
            return results;
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

        public Map<String, String> getSluggMappings(String slugFrom) {
            Map<String, String> stringStringMap = slugMappings.get(slugFrom);
            if (stringStringMap==null) {
                stringStringMap = new HashMap<String, String>();
                slugMappings.put(slugFrom, stringStringMap);
                logger.trace("Created SluggMappings for "+slugFrom+" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1");
            }
            return stringStringMap;
        }
    }

    static class CoinTemplate {

        private final Logger logger = LoggerFactory.getLogger(CoinTemplate.class);

        CoinURISpace space;
        String forType;
        String uriTemplate;
        String relToBase;
        String relFromBase;
        int priority = 0;
        List<CoinBinding> bindings = new ArrayList<CoinBinding>();

        CoinTemplate(CoinURISpace space, Description desc) {
            this.space = space;
            forType = desc.getObjectUri("coin:forType");
            uriTemplate = desc.getString("coin:uriTemplate");
            relToBase = desc.getObjectUri("coin:relToBase");
            relFromBase = desc.getObjectUri("coin:relFromBase");
            Object givenPriority = desc.getNative("coin:priority");
            if (givenPriority != null) {
                priority = ((java.math.BigInteger) givenPriority).intValue();
            }
            logger.trace("%%%%%%%%%%%%% uriTemplate='"+uriTemplate+"' %%%%%%%%%%%%%%%%%");
            for (Description cmp : desc.getRels("coin:binding")) {
                CoinBinding binding = new CoinBinding(this, cmp);
                bindings.add(binding);
                logger.trace("binding.property="+binding.property+", binding.variable="+binding.variable+", binding.slug="+binding.slugFrom);
            }
        }

        MintResult coinUri(Description desc, boolean allowSuggestion) {

            final boolean trace = uriTemplate!=null? /*(uriTemplate.equals("/publ/rf/{serie}/{arsutgava}:{lopnummer}")
                            ||*/ uriTemplate.equals("/publ/dom/{publisher}/{malnummer}/{avgorandedatum}")
                            /*|| uriTemplate.equals("/publ/{fs}/{arsutgava}:{lopnummer}"))*/:false;

            if (trace) logger.trace("******************* TRACE *******************");
            if (trace) logger.trace("uri="+desc.getAbout());
            if (trace) logger.trace("uriTemplate="+uriTemplate);
            int matchCount = 0;
            int rulesSize = bindings.size();
            if (forType != null) {
                if (trace) logger.trace("forType="+forType);
                rulesSize += 1;
                boolean ok = false;
                for (Description type : desc.getTypes()) {
                    logger.trace(type.getAbout() + " är samma som? " + forType);
                    if (type.getAbout().equals(forType)) {
                        matchCount += 1;
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    logger.trace("missing forType: " + forType + " return MintResult without uri. desc.getAbout() = " + desc.getAbout() + ", uriTemplate = " + uriTemplate + ", matchcount = " + matchCount + ", rulesSize = " + rulesSize + ", priority = " + priority);
                    return new MintResult(null, matchCount, rulesSize, priority);
                }
            } else
                if (trace) logger.trace("missing forType!!!");

            Map<String, String> matches = new HashMap<String, String>();
            for (final CoinBinding binding : bindings) {
                final SluggMappings slugMap = getSluggmappingsViaSlugFrom(binding);
                if (trace) logger.trace("binding.property="+binding.property+", binding.variable="+binding.variable+", binding.slug="+binding.slugFrom);
                String match = binding.findMatch(desc, new Description.ReverseSlug() {
                    @Override
                    public String lookup(String urlStr, Description rel) {
                        if (trace) logger.trace("ReverseSlug.lookupl("+urlStr+")");
                        String relAboutSlug = rel!=null?slugMap.get(rel.getAbout()):null;
                        if (trace) {
                            if (rel!=null)
                                logger.trace("ReverseSlug.relAboutSlug("+rel.getAbout()+")="+relAboutSlug);
                            else
                                logger.trace("ReverseSlug.relAboutSlug("+null+")");
                        }
                        try {
                            URL url = new URL(urlStr);
                            String resultSlug = slugMap.get(url.toString());
                            if (trace) logger.trace("ReverseSlug.url("+url+")="+resultSlug);
                            return resultSlug;
                        } catch (MalformedURLException e) {
                            return urlStr;
                        }
                    }
                });
                if (trace) logger.trace("match="+match);
                if (match != null) {
                    matchCount++;
                    matches.put(binding.variable, match);
                }
            }
            boolean ok = (matchCount == rulesSize);

            String uri;

            if (allowSuggestion) {
                uri = buildUri(determineBase(desc), matches, true);
            } else {
                uri = (matchCount == rulesSize)?
                    buildUri(determineBase(desc), matches, false) :
                    null;
            }

            if (trace&&ok) logger.trace("uri="+uri);
            return new MintResult(uri, matchCount, rulesSize, priority);
        }

        private SluggMappings getSluggmappingsViaSlugFrom(CoinBinding binding) {
            if (binding.slugFrom==null)
                return new EmptySluggMappings();

            return new SluggMappingsImpl(space.getSluggMappings(binding.slugFrom)){
                @Override
                public void put(String about, String info) {
                    logger.trace("slugMappings.put("+about+","+info+")");
                    super.put(about, info);
                    super.put(info, about);
                }

                @Override
                public String get(String about) {
                    logger.trace("slugMappings.get("+about+")="+super.get(about));
                    return super.get(about);
                }
            };
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

        String buildUri(String base, Map<String, String> matches, boolean allowSuggestion) {
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

            if (allowSuggestion) {
                logger.trace("returning expanded = " + expanded);
                return expanded;
            }

            else {
                try {
                    return new java.net.URI(base).resolve(expanded).toString();
                } catch (java.net.URISyntaxException e) {
                    throw new RuntimeException(e);
                }
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
                SluggMappings sluggMappings = tplt.getSluggmappingsViaSlugFrom(this);
                for (Description slugged : desc.getDescriber().subjects(slugFrom, null)) {
                    sluggMappings.put(slugged.getAbout(), slugged.getString(slugFrom));
                }
            }
        }


        String findMatch(Description desc, Description.ReverseSlug reverseSlug) {
            if (slugFrom != null) {
                Description rel = desc.getRel(property);
                if (rel == null)
                    return null;
                String v = rel.getString(slugFrom);
                if (v != null)
                    return v;
                //Map<String, String> slugMap = tplt.space.slugMappings.get(slugFrom);
                SluggMappings slugMap = tplt.getSluggmappingsViaSlugFrom(this);
                if (slugMap != null) {
                    return slugMap.get(rel.getAbout());
                }
            } else if (desc != null && property != null)
                return desc.getLexical(property, reverseSlug);
            return null;
        }

        String getLeaf(String uri) {
            int frag = uri.lastIndexOf('#');
            return (frag != -1)? uri.substring(frag+1) :
                    uri.substring(uri.lastIndexOf('/')+1);
        }

    }

    interface SluggMappings {
        void put(String about, String info);
        String get(String about);
    }

    /**
     * Only empty reply
     */
    private static class EmptySluggMappings implements SluggMappings {
        @Override
        public void put(String about, String info) {}

        @Override
        public String get(String about) {return null;}
    }

    private static class SluggMappingsImpl implements SluggMappings {
        Map<String, String> stringStringMap;

        private SluggMappingsImpl(Map<String, String> stringStringMap) {
            this.stringStringMap = stringStringMap;
        }

        @Override
        public void put(String about, String info) {
            stringStringMap.put(about, info);
        }

        @Override
        public String get(String about) {
            return stringStringMap.get(removeTrailingSlash(about));
        }
    }

    private static String removeTrailingSlash(String url) {
        if (url==null)
            return null;
        if (url.endsWith("/"))
            return url.substring(0,url.length()-1);
        return url;
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
            for (List<MintResult> results : uriMinter.computeUris(docRepo).values()) {
                for (MintResult result : results) {
                    System.out.println("Minted <"+result.getUri()+">");
                }
            }
            diff = (new Date().getTime() - time.getTime()) / 1000.0;
            System.out.println("// Time: "+diff+" s.");
            System.out.println();
            docRepo.shutDown();
        }

    }

}
