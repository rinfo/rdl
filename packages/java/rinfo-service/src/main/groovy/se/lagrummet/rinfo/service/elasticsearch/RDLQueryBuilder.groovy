package se.lagrummet.rinfo.service.elasticsearch

/**
 * Created by christian on 2/16/15.
 */
public interface RDLQueryBuilder {

    String PREPARED_SEARCH_NAME_INDICE = "rinfo"
    float TYPE_BOOST_KONSOLIDERAD_GRUNDFORFATTNING = 1.05f
    String QUERY_MINIMUM_MATCH = "80%"

    String SELECT_FIELDS =
            "type, iri, identifier, title, malnummer, beslutsdatum, issued, ikrafttradandedatum"

    String[] QUERY_SEARCH_FIELDS = ["identifier^5", "title^2", "text"]

    def HIGHLIGHTERS_TAG = [start:"<em class=\"match\">",end:"</em>"]
    def HIGHLIGHTED_FIELDS = [
            [field:"title", size:150, number:1],
            [field:"identifier", size:150, number:1],
            [field:"text", size:150, number:1],
            [field:"referatrubrik", size:150, number:1]
    ]
    
    enum Type {
        KonsolideradGrundforfattning
    }

    QueryBuilder createBuilder()

    interface QueryBuilder {

        void addQuery(String query)

        void restrictType(String type)

        Result result(String iriReplaceUrl)

        void close()

        void setPagination(int page, int pageSize)
    }

    interface Result {
        List items();

        double duration()

        Map stats()

        int startIndex()

        int pageSize()

        long totalHits()

        int hitsLength()

        int page()
    }
}