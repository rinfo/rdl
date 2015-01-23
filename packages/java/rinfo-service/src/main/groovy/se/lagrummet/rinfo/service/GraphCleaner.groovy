package se.lagrummet.rinfo.service

import org.openrdf.OpenRDFException
import org.openrdf.query.QueryLanguage
import org.openrdf.repository.Repository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GraphCleaner {
    private final Logger logger = LoggerFactory.getLogger(GraphCleaner.class)

    def subjectsWithManyPredicate(Repository itemRepo, String predicate) {
        def queryString = getClass().getResourceAsStream(
                '/sparql/select_s_with_multiple_titles.rq').getText("utf-8")
        def itemCon = itemRepo.getConnection()
        def dupeQuery = itemCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString)
        dupeQuery.setBinding("predicate", itemCon.valueFactory.createURI(predicate))
        def dupeRes = dupeQuery.evaluate()

        def subjects = []

        if(dupeRes) {
            try {
                while (dupeRes.hasNext()) {
                    def bindingSet = dupeRes.next()
                    logger.debug("subject with many \"${predicate}\": ${bindingSet.getValue("s")}")
                    subjects << bindingSet.getValue("s").toString()
                }
            } catch (OpenRDFException e) {
                logger.warn("Something went wrong when finding subjects with too many ${predicate} Details: " + e.getMessage())
            } finally {
                itemCon.close()
            }
        }
        return subjects
    }


    def tryGetDataFromNamedGraph(Repository repo, String subject, String predicate, String graph) {
        def conn = repo.getConnection()
        def constructQueryText = getClass().getResourceAsStream(
                '/sparql/select_object_from_named.rq').getText("utf-8")
        try {

            logger.debug("trying to get ${subject} from named graph ${graph}")
            def query = conn.prepareTupleQuery(QueryLanguage.SPARQL, constructQueryText);

            query.setBinding("subject", conn.valueFactory.createURI(subject))
            query.setBinding("graph", conn.valueFactory.createURI(graph))
            query.setBinding("predicate", conn.valueFactory.createURI(predicate))

            def result = query.evaluate();

            if(result.hasNext()) {
                def binding = result.next()
                return binding.getValue("data").toString()
            }
        } catch (OpenRDFException e) {
            logger.warn("Something went wrong when finding data to <${subject}> Details: " + e.getMessage())
        } finally {
            conn.close()
        }
        return ''
    }

    def updateGraph(Repository itemRepo, String subject, String predicate, String newData) {
        def itemCon = itemRepo.getConnection()
        def updateQueryString = getClass().getResourceAsStream(
                '/sparql/replace_object_for_selected.rq').getText("utf-8")
        try {
            logger.debug("updating \"${predicate}\" for ${subject}")
            def updateQuery = itemCon.prepareUpdate(QueryLanguage.SPARQL, updateQueryString)

            updateQuery.setBinding("subject", itemCon.valueFactory.createURI(subject))
            updateQuery.setBinding("predicate", itemCon.valueFactory.createURI(predicate))
            updateQuery.setBinding("data", itemCon.valueFactory.createLiteral(newData))
            updateQuery.execute()

            return itemRepo
        } catch (OpenRDFException e) {
            logger.warn("Something went wrong when updating ${predicate} for subject <${subject}> Details: " + e.getMessage())
        } finally {
            itemCon.close()
        }
    }
}
