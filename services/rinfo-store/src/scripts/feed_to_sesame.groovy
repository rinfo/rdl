
import org.slf4j.LoggerFactory

import org.apache.abdera.model.Feed
import org.apache.abdera.model.Entry

import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.http.HTTPRepository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.rio.RDFFormat
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.sail.nativerdf.NativeStore

import se.lagrummet.rinfo.util.atom.FeedArchiveReader


class SesameFeeder extends FeedArchiveReader {

    Repository repository

    private final logger = LoggerFactory.getLogger(SesameFeeder)

    SesameFeeder(repository) {
        this.repository = repository
    }

    boolean processFeedPage(URL pageUrl, Feed feed) {
        def conn = repository.connection
        for (entry in feed.entries) {
            def entryId = entry.id

            // TODO: from content *or* alternate recognized as RDF
            def rdfPath = entry.contentSrc.toString()
            // FIXME: the ":"-in-url uri-minter problem
            rdfPath = rdfPath.replace(URLEncoder.encode(":", "utf-8"), ":")

            logger.info "Entry <${entryId}>, RDF from <rdfPath>"

            loadDataFromURL(conn, new URL(rdfPath), "${entryId}")
            // TODO: Check for tombstones; delete..: conn.clear(context)

        }
        conn.close()

        // TODO: stop at feed with entry at minDateTime..
        //Date minDateTime=null
        return true
    }

    void loadDataFromURL(RepositoryConnection conn, URL url, String uri) {
        // TODO: def contextUri = repository.valueFactory.createURI(uri + "#context")
        conn.add(url, url.toString(), RDFFormat.RDFXML)
        conn.commit()
    }

    static main(args) {
        Repository repo = null
        if (args.length > 1) {
            def path = args[1]
            if (path =~ /^https?:/) {
                repo = new HTTPRepository(path)
            } else {
                def dataDir = new File(path)
                repo = new SailRepository(new NativeStore(dataDir))
            }
        } else {
            repo = new SailRepository(new MemoryStore())
        }
        repo.initialize()

        def reader = new SesameFeeder(repo)
        reader.readFeed new URL(args[0])

        repo.shutDown()
    }

}
