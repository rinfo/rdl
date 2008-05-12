package se.lagrummet.rinfo.service.sparql


import java.net.URL

import org.apache.abdera.Abdera

/*
import org.openrdf.repository.Repository
import org.openrdf.repository.RepositoryConnection
import org.openrdf.repository.http.HTTPRepository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.rio.RDFFormat
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.sail.nativerdf.NativeStore
*/


class FeedReader {
/*

    Repository repository
    private abdera = new Abdera()

    FeedReader(repository) {
        this.repository = repository
    }

    void readFeed(URL url) {
        def nextUrl = url
        while (nextUrl) {
            def feed = readFeedPage(nextUrl)
            def nextHref = feed?.getLinkResolvedHref("next")
            nextUrl = nextHref? new URL(nextHref.toString()) : null
            if (nextUrl)
                println "Reading next.."
        }
        println "Done."
    }

    def readFeedPage(url) {
        println "Reading <${url}>"
        def feed = abdera.parser.parse(url.openStream(), url.toString())?.root
        println "Title: ${feed.title}"
        def conn = repository.connection
        for (entry in feed.entries) {
            print "  Entry ID <${entry.id}>; "
            println "content <${entry.contentSrc}> ."
            loadDataFromURL(conn, new URL(entry.contentSrc.toString()), "${entry.id}")
            // TODO: Cehck for tombstone; if so, delete.. conn.clear(context)
        }
        conn.close()
        return feed
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

        def reader = new FeedReader(repo)
        reader.readFeed new URL(args[0])

        repo.shutDown()
    }

*/
}
