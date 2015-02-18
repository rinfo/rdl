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

        Result result()

        void close()
    }

    interface Result {
        List items();
    }
}