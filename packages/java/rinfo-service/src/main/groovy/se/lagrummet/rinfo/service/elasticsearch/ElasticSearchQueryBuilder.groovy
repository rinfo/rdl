package se.lagrummet.rinfo.service.elasticsearch

/**
 * Created by christian on 2/16/15.
 */
public interface ElasticSearchQueryBuilder {

    String regex_sanitize_elasticsearch = "([+\\-|&!{}\\[\\]\\/^~\"\\\\]|[&\\|]{2}" +
            "|\\:(?!\\S)" + //matchar kolon ej följt av ett icke "whitespace" (om tecknet är sista tecken..)
            "|\\((?!\\w)\\)" + //matchar ( ej följt av tecken följt av )
            "|\\((?!\\w)" + //matchar ( ej följt av tecken
            "|(?<!\\w)\\))"; //matchar ) utan tecken _innan_ )
    String replacement = "\\\\\$1";


    String QUERY_MINIMUM_MATCH = "80%"

    Float EXACT_MATCH_BOOST = null;

    String SELECT_FIELDS =
            "type, iri, identifier, title, malnummer, beslutsdatum, issued, ikrafttradandedatum, referatrubrik"

    String[] QUERY_SEARCH_FIELDS = ["identifier^5", "title^2", "text", "referatrubrik^2", "malnummer"]

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
            [type:"KonsolideradGrundforfattning",group:"Lagar", boost: 3f],
            [type:"Grundlag",group:"Lagar"],
            [type:"Tillkannagivande",group:"Lagar"],
            [type:"Rattelseblad",group:"Lagar"],
            [type:"Brev",group:"Lagar"],
            [type:"Cirkular",group:"Lagar"],
            [type:"AllmannaRad",group:"Lagar"],
            /* Rattsfall */
            [type:"Rattsfallsnotis",group:"Rattsfall"],
            [type:"Rattsfallsreferat",group:"Rattsfall", boost: 2f],
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

        void setExplain(Boolean explain)

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