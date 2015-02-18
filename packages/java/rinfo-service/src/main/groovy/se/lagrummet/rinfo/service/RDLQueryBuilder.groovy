package se.lagrummet.rinfo.service

/**
 * Created by christian on 2/16/15.
 */
public interface RDLQueryBuilder {

    enum Type {
        KonsolideradGrundforfattning
    }

    QueryBuilder createBuilder()

    interface QueryBuilder {

        void addQuery(String query)

        void restrictType(Type type)

        Result result(String iriReplaceUrl)

        void close()
    }

    interface Result {
        List items();

        double duration()

        Map stats()
    }
}