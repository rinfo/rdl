package se.lagrummet.rinfo.service.elasticsearch

/**
 * Created by christian on 2/16/15.
 */
public interface ElasticSearchQueryBuilder {

    String PREPARED_SEARCH_NAME_INDICE = "rinfo"

    String QUERY_MINIMUM_MATCH = "80%"

    float EXACT_MATCH_BOOST = 100000f;

    String SELECT_FIELDS =
            "type, iri, identifier, title, malnummer, beslutsdatum, issued, ikrafttradandedatum"

    String[] QUERY_SEARCH_FIELDS = ["identifier^5", "title^2", "text"]

    def HIGHLIGHTERS_TAG = [start:"<span class=\"match\">",end:"</span>"]

    def HIGHLIGHTED_FIELDS = [
            [field:"title", size:150, number:1],
            [field:"identifier", size:150, number:1],
            [field:"text", size:150, number:1],
            [field:"referatrubrik", size:150, number:1]
    ]

    List TYPE = [
            /* Lagar */
            [type:"Lag", group:"Lagar"],
            [type:"Forordning",group:"Lagar"],
            [type:"KonsolideradGrundforfattning",group:"Lagar", boost: 5000000.05f],
            [type:"Grundlag",group:"Lagar"],
            [type:"Tillkannagivande",group:"Lagar"],
            [type:"Rattelseblad",group:"Lagar"],
            [type:"Brev",group:"Lagar"],
            [type:"Cirkular",group:"Lagar"],
            [type:"AllmannaRad",group:"Lagar"],
            /* Rattsfall */
            [type:"Rattsfallsnotis",group:"Rattsfall"],
            [type:"Rattsfallsreferat",group:"Rattsfall"],
            [type:"VagledandeDomstolsavgorande",group:"Rattsfall"],
            [type:"VagledandeMyndighetsavgorande",group:"Rattsfall"],
            /* Foreskrifter */
            [type:"Myndighetsforeskrift",group:"Foreskrifter"],
            /* Propositioner */
            [type:"Proposition",group:"Propositioner"],
            /* Utredningar */
            [type:"Kommittedirektiv",group:"Utredningar"],
            [type:"Utredningsbetankande",group:"Utredningar"],
            [type:"Utredningsserie",group:"Utredningar"],
    ]

    QueryBuilder createBuilder()

    interface QueryBuilder {

        void addQuery(String query)

        void addSynonym(String synonym)

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