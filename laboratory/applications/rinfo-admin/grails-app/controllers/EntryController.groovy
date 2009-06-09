class EntryController {

    static allowedMethods = [feed:'GET']

    def feed = {
        params.max = 1000
        params.sort = 'dateCreated'
        params.order = 'desc'

        def feedUrl = request.scheme + "://" + request.serverName + ":" + request.serverPort + "/" + grailsApplication.metadata.'app.name' + "/" + controllerName + "/feed" 
       
        //def entryList = Entry.list( params )	
        def entryList = Entry.findAll()	

        def df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+01:00'")
        def dateUpdated = df.format(new Date())

        def feedHeader = """<feed xmlns="http://www.w3.org/2005/Atom" \n\txmlns:at="http://purl.org/atompub/tombstones/1.0" \n\txmlns:le="http://purl.org/atompub/link-extensions/1.0" \n\txmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <title type="text">Rinfo admin data feed</title>
        <link rel="self" href="${feedUrl}" />
        <updated>${dateUpdated}</updated>
        <author><name>Rinfo</name></author>
        <id>tag:admin.lagrummet.se,2009:data</id>
        """

        def feedFooter = "</feed>"

        StringBuffer feed = new StringBuffer()
        entryList.each { item ->
            Writer sw = new StringWriter()
                def x = new groovy.xml.MarkupBuilder(sw)

                if(!item.dateDeleted) {
                    x.entry {
                        title(type:"text", item.title)
                        published(df.format(item.dateCreated))
                        updated(df.format(item.lastUpdated))
                        author{name("Rinfo")}
                        id(item.uri)
                        x.yieldUnescaped "\n<content type=\"application/rdf+xml\">\n"
                        x.yieldUnescaped item.content
                        x.yieldUnescaped "\n</content>"
                    }
                } else {
                    x.'at:deleted-entry'(ref: item.uri, when: df.format(item.dateDeleted)) {
                        'at:comment'("Post med URI " + item.uri + " raderades.")
                    }
                }
                feed.append(sw.toString() + "\n")
        }

        response.setContentType("application/atom+xml")				
        render "${feedHeader}${feed}${feedFooter}"
    }
}
