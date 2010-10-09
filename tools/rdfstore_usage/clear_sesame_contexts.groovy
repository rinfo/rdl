#!/usr/bin/env groovy
@Grab('org.openrdf.sesame:sesame-repository-api:2.3.2')
@Grab('org.openrdf.sesame:sesame-repository-http:2.3.2')
@Grab('org.slf4j:slf4j-jcl:1.6.1')
import org.openrdf.repository.http.HTTPRepository


SERVER_URL = "http://localhost:8080/openrdf-sesame"


def clearHttpRepo(String repoId, String... contexts) {
    def serverUrl = SERVER_URL
    println "Using repository '${repoId}' in server <${serverUrl}>"
    def repo = new HTTPRepository(serverUrl, repoId)
    try {
        def conn = repo.connection
        try {
            def (size, i) = [contexts.length, 1]
            for (ctx in contexts) {
                def ctxUri = conn.valueFactory.createURI(ctx)
                print "Context ${i++}/${size}: <${ctxUri}> "
                if (!conn.hasStatement(null, null, null, false, ctxUri)) {
                    println "Empty or does not exist!"
                    continue
                }
                print "Clearing ..."
                conn.clear(ctxUri)
                println " Cleared."
            }
        } finally {
            conn.close()
        }
        println "Done."
    } finally {
        repo.shutDown()
    }
}


if (args.length < 2) {
    println "Usage: REPO_ID CONTEXT_URI ..."
    System.exit 0
} else {
    clearHttpRepo(args[0], args[1..args.length-1] as String[])
}

