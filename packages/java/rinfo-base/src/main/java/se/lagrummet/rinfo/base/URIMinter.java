package se.lagrummet.rinfo.base;

import java.util.*;
import java.io.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
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

import se.lagrummet.rinfo.base.rdf.RDFUtil;


public class URIMinter {

    private Repository baseData;
    private Map<String, String> baseCharMap;
    private String queryStr;

    public static final String BASE_CHAR_MAP_PATH =
            "/uriminter/unicodebasechars-scandinavian.json";
    public static final String QUERY_PATH =
            "/uriminter/select_coin_items.rq";

    public URIMinter(Repository baseData) throws IOException {
        this.baseData = baseData;
        String encoding = "UTF-8";
        InputStream charMapInStream =
                getClass().getResourceAsStream(BASE_CHAR_MAP_PATH);
        try {
            this.baseCharMap = (Map) JSONSerializer.toJSON(
                    IOUtils.toString(charMapInStream, encoding));
        } finally {
            charMapInStream.close();
        }
        InputStream queryInStream = getClass().getResourceAsStream(QUERY_PATH);
        try {
            this.queryStr = IOUtils.toString(queryInStream, encoding);
        } finally {
            queryInStream.close();
        }
    }

    public Map getBaseCharMap() { return baseCharMap; }
    public void setBaseCharMap(Map baseCharMap) {
        this.baseCharMap = baseCharMap;
    }

    public String getQueryStr() { return queryStr; }
    public void setQueryStr(String queryStr) {
        this.queryStr = queryStr;
    }

    public java.net.URI computeUri(Repository docRepo) throws Exception {
        for (Set<java.net.URI> uris : computeUris(docRepo).values()) {
            for (java.net.URI uri : uris) {
                return uri;
            }
        }
        return null;
    }

    public Map<String, Set<java.net.URI>> computeUris(Repository docRepo) throws Exception {
        // TODO: use contexts instead of addToRepo,
        // - supply files/streams-from-uris instead of baseData and docRepo
        // - give those as namedGraphs to runQuery
        Repository mergedRepo = RDFUtil.createMemoryRepository();
        if (baseData != null) {
            RDFUtil.addToRepo(mergedRepo, baseData);
        }
        RDFUtil.addToRepo(mergedRepo, docRepo);
        RepositoryConnection conn = mergedRepo.getConnection();
        TupleQueryResult result = runQuery(conn);
        Map<String, Set<java.net.URI>> uriMap = new HashMap<String, Set<java.net.URI>>();
        while (result.hasNext()) {
            BindingSet row = result.next();
            java.net.URI uri = new java.net.URI(buildUri(row));
            String thisObj = row.getValue("this").stringValue();
            Set<java.net.URI> minted = uriMap.get(thisObj);
            if (minted == null) {
                minted = new HashSet<java.net.URI>();
                uriMap.put(thisObj, minted);
            }
            minted.add(uri);
        }
        result.close();
        conn.close();
        mergedRepo.shutDown();
        return uriMap;
    }

    public TupleQueryResult runQuery(RepositoryConnection conn) throws Exception {
        return runQuery(conn, null);
    }

    public TupleQueryResult runQuery(RepositoryConnection conn, Set<URI> namedGraphs) throws Exception {
        // TODO:? set subject URI?
        //if (about != null) {
        //    tupleQuery.setBinding("this", repo.valueFactory.createURI(about))
        //}
        TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
        if (namedGraphs != null) {
            DatasetImpl dataset = new DatasetImpl();
            for (URI named : namedGraphs) {
                dataset.addNamedGraph(named);
            }
            query.setDataset(dataset);
        }
        return query.evaluate();
    }

    String buildUri(BindingSet row) {
        return new Builder(row).buildUri();
    }

    class Builder {

        BindingSet row;
        String spaceRepl;
        boolean baseCharTranslation;
        boolean lowerCasedTranslation;
        String root;
        String separator;
        String fragmentSeparator;

        Builder(BindingSet row) {
            this.row = row;
            // TODO: undefined in current query
            spaceRepl = getStringValue("translationSpaceRepl", "_");
            baseCharTranslation = true; //row.getValue("baseCharTranslation") != null
            lowerCasedTranslation = true; //row.getValue("lowerCasedTranslation") != null
            //urlEncodedTranslation = true; //row.getValue("URlEncodedTranslation") != null
            root = getStringValue("root");
            separator = getStringValue("separator", "/");
            fragmentSeparator = getStringValue("fragmentSeparator", "");
        }

        String buildUri() {
            String componentParts = getComponentParts();
            if (componentParts != null) {
                return root + componentParts;
            } else {
                return getContainmentPath();
            }
        }

        String getComponentParts() {
            Value start = row.getValue("segment0");
            if (start == null) {
                return null;
            }
            StringBuffer sb = new StringBuffer();
            sb.append(start.stringValue());
            int i = 0;
            while (true) {
                i++;
                String segment = getStringValue("segment"+i);
                if (segment != null) {
                    sb.append(separator);
                    sb.append(segment);
                }
                String token = getObjectToken(""+i);
                if (token != null) {
                    sb.append(separator);
                    sb.append(token);
                }
                if (segment==null && token==null) {
                    break;
                }
            }
            return sb.toString();
        }

        String getContainmentPath() {
            String fragmentMarker = "#";
            StringBuffer sb = new StringBuffer();
            String baseResource = getStringValue("baseResource");
            if (baseResource == null) {
                return null;
            }
            sb.append(baseResource);
            String containmentSegment = getStringValue("containmentSegment");
            String fragmentPrefix = getStringValue("fragmentPrefix");
            if (containmentSegment != null) {
                sb.append(separator);
                sb.append(containmentSegment);
                sb.append(separator);
            } else if (fragmentPrefix != null) {
                if (baseResource.indexOf(fragmentMarker) == -1) {
                    sb.append(fragmentMarker);
                } else {
                    sb.append(fragmentSeparator);
                }
                sb.append(fragmentPrefix);
            }
            sb.append(getObjectToken());
            return sb.toString();
        }

        String getObjectToken() { return getObjectToken(""); }

        String getObjectToken(String suffix) {
            Value token = row.getValue("tokenValue"+suffix);
            if (token != null) return token.stringValue();
            Value obj = row.getValue("object"+suffix);
            if (obj != null) return encodeObject(obj);
            return null;
        }

        String encodeObject(Value value) {
            String token = value.stringValue();
            token = token.replace(" ", spaceRepl);
            if (baseCharTranslation) {
                StringBuffer sb = new StringBuffer();;
                int i = 0;
                while (i < token.length()) {
                    int codePoint = token.codePointAt(i);
                    i += Character.charCount(codePoint);
                    String strChar = new String(Character.toChars(codePoint));
                    String mappedChar = baseCharMap.get(strChar);
                    sb.append(mappedChar != null ? mappedChar : strChar);
                }
                token = sb.toString();
            }
            if (lowerCasedTranslation) {
                token = token.toLowerCase();
            }
            return token;
        }

        String getStringValue(String key) {
            return getStringValue(key, null);
        }

        String getStringValue(String key, String defaultValue) {
            Value v = row.getValue(key);
            return (v != null)? v.stringValue() : defaultValue;
        }

    }



    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: <base-data> <doc>");
            System.exit(1);
        }
        //String baseCharMapPath = args[0];
        //String queryPath = args[1];
        Repository baseData = RDFUtil.slurpRdf(args[0]);
        String docPath = args[1];
        URIMinter uriMinter = new URIMinter(baseData);//, baseCharMapPath, queryPath);
        Repository docRepo = RDFUtil.createMemoryRepository();
        RDFUtil.loadDataFromFile(docRepo, new File(docPath));
        for (Set<java.net.URI> uris : uriMinter.computeUris(docRepo).values()) {
            for (java.net.URI uri : uris) {
                System.out.println(uri);
            }
            System.out.println();
        }
        docRepo.shutDown();
    }

}
