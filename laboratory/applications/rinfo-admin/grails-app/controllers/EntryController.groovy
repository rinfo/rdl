/**
* The main task of this controller is to generate the Atom feed with information
* for the main application.
*/
class EntryController {

    static allowedMethods = [feed:'GET']

    def feed = {
        params.sort = 'lastUpdated'
        params.order = 'desc'

        def feedUrl = request.scheme + "://" + request.serverName + ":" + request.serverPort + "/" + grailsApplication.metadata.'app.name' + "/" + controllerName + "/feed" 
       
        def entryList = Entry.list( params )	

        def df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+01:00'")
        def dateUpdated = df.format(new Date())

        Writer sw = new StringWriter()
        def mb = new groovy.xml.MarkupBuilder(sw)

        mb.feed(xmlns: "http://www.w3.org/2005/Atom",
                'xmlns:at':"http://purl.org/atompub/tombstones/1.0",
                'xmlns:le':"http://purl.org/atompub/link-extensions/1.0",
                'xmlns:rdf':"http://www.w3.org/1999/02/22-rdf-syntax-ns#") {
                title(type:"text", "Rinfo admin data feed")
                link(rel:"self", href: feedUrl)
                updated(dateUpdated)
                author{ name("Rinfo") }
                id("tag:admin.lagrummet.se,2009:data")

                entryList.each { item ->
                    if(!item.dateDeleted) {
                        entry {
                            title(type:"text", item.title)
                            published(df.format(item.dateCreated))
                            updated(df.format(item.lastUpdated))
                            author{name("Rinfo")}
                            id(item.uri)
                            content(type: "application/rdf+xml") {
                                mb.yieldUnescaped item.content
                            }
                        }
                    } else {
                        'at:deleted-entry'(ref: item.uri, when: df.format(item.dateDeleted)) {
                            'at:comment'("Post med URI " + item.uri + " raderades.")
                        }
                    }
                }
        }

        response.setContentType("application/atom+xml")
        render sw.toString()
    }
}
