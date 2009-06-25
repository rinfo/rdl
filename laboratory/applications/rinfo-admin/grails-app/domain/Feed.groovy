/**
* Represents an atom feed where legal information from an organization can be picked up.
*/
class Feed {

    static constraints = {
        url(url:true, blank:false, maxSize:400)
        identifier(blank:false, maxSize:300)
        organization(blank:false) 
        lastUpdated(nullable: true)
        dateCreated()
    }

    static belongsTo = Organization
    Organization organization    

    /**
    * The current location of the feed. 
    * @see identifier
    */
    String url

    /**
    * The long term URI identifier for a feed. Is not necessarily the same as
    * the URL where it resides. This identifier will likely be a <a
    * href="http://www.faqs.org/rfcs/rfc4151.html">Tag URI</a> to enable
    * different locations for the feed.
    */
    String identifier

    Date dateCreated
    Date lastUpdated

    String toString() { 
        return "Källa: " + url
    }

    String toXML() {
        Writer sw = new StringWriter()
        def mb = new groovy.xml.MarkupBuilder(sw)
        mb.'dct:source' {
            'sioc:Space'('rdf:about': this.rinfoURI()) {
                'dct:publisher'('rdf:resource': this.organization.rinfoURI())
                'sioc:feed'('rdf:resource': this.url)
            }
        }
        return sw.toString()
    }


    static String toEntryContent() {

        def items = Feed.findAll() ?: []
        
        Writer sw = new StringWriter()
        def mb = new groovy.xml.MarkupBuilder(sw)
        mb.'rdf:RDF'('xmlns:rdf':"http://www.w3.org/1999/02/22-rdf-syntax-ns#", 
                    'xmlns:dct':"http://purl.org/dc/terms/",
                    'xmlns:sioc':"http://rdfs.org/sioc/ns#") {
            'rdf:Description'('rdf:about':'tag:lagrummet.se,2009:rinfo') {
                items.each { item ->
                    mb.yieldUnescaped item.toXML()
                }
            }
        }

        return sw.toString()
    }


    String rinfoURI() {
        return identifier
    }   
}
