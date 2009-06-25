/**
* Represents a collection of legal information (eg laws). All instances belong
* to an Organization. For information about the formal name and formal short
* name see <a href="https://lagen.nu/1976:725">SFS 1976:725</a>.
*/
class Publicationcollection {

    static constraints = {
        name(blank:false, maxSize:500)
        shortname(blank:false, maxSize:500)
        organization()
        homepage(url:true, blank:false, maxSize:400)
        lastUpdated(nullable: true)
        dateCreated()
    }

    static belongsTo = Organization
    Organization organization    

    String name
    String shortname
    String homepage
    Date dateCreated
    Date lastUpdated

    String toString() { 
        return name + " (" + shortname + ")"
    }

    String rinfoURI() {
        return "http://rinfo.lagrummet.se/serie/fs/" + shortname.toLowerCase().replaceAll(" ", "_").replaceAll("ö","o").replaceAll("ä","a").replaceAll("å","a")
    }   

    String toEntryContent() {
        Writer sw = new StringWriter()
        def mb = new groovy.xml.MarkupBuilder(sw)
        mb.'rdf:RDF'('xmlns:rdf':"http://www.w3.org/1999/02/22-rdf-syntax-ns#", 
                    'xmlns:rdfs':"http://www.w3.org/2000/01/rdf-schema#",
                    'xmlns:skos':"http://www.w3.org/2004/02/skos/core#",
                    'xmlns:publ':"http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#") {
            'publ:Forfattningssamling'('rdf:about': this.rinfoURI()) {
                'skos:prefLabel'('xml:lang': "sv", this.name)
                'skos:altLabel'('xml:lang': "sv", this.shortname)
                'rdfs:seeAlso'('rdf:resoruce': this.homepage)
            }
        }

        return sw.toString()
    }
}
