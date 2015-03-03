package se.lagrummet.rinfo.service

import org.openrdf.OpenRDFException
import org.openrdf.query.QueryLanguage
import org.openrdf.repository.Repository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GraphCleanUtil {
    private static final Logger logger = LoggerFactory.getLogger(GraphCleanUtil.class)

    def static subjectsWithManyPredicate(Repository itemRepo, String predicate) {
        def queryString = readQueryStringFromFile('select_s_with_multiple_titles.rq')
        def conn = itemRepo.getConnection()
        def dupeQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString)
        dupeQuery.setBinding("predicate", conn.valueFactory.createURI(predicate))
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
                conn.close()
            }
        }
        return subjects
    }


    def static tryGetDataFromNamedGraph(Repository repo, String subject, String predicate, String graph) {
        def conn = repo.getConnection()
        def namedGraphQueryString = readQueryStringFromFile('select_object_from_named.rq')
        try {

            logger.debug("trying to get ${subject} from named graph ${graph}")
            def query = conn.prepareTupleQuery(QueryLanguage.SPARQL, namedGraphQueryString);

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
        logger.debug("could not get ${subject} from named graph ${graph}")
        return ''
    }

    def static updateGraph(Repository itemRepo, String subject, String predicate, String newData) {
        def conn = itemRepo.getConnection()
        def updateQueryString = readQueryStringFromFile('replace_object_for_selected.rq')

        try {
            logger.debug("updating \"${predicate}\" for ${subject}")
            def updateQuery = conn.prepareUpdate(QueryLanguage.SPARQL, updateQueryString)

            updateQuery.setBinding("subject", conn.valueFactory.createURI(subject))
            updateQuery.setBinding("predicate", conn.valueFactory.createURI(predicate))
            updateQuery.setBinding("data", conn.valueFactory.createLiteral(newData))
            updateQuery.execute()

            return itemRepo
        } catch (OpenRDFException e) {
            logger.warn("Something went wrong when updating ${predicate} for subject <${subject}> Details: " + e.getMessage())
        } finally {
            conn.close()
        }
    }

    def static filterRepo(Repository itemRepo, String filteredPredicate ,String resourceUri) {
        logger.debug("Filtering duplicates in context of ${resourceUri}")
        def withManyTitles = GraphCleanUtil.subjectsWithManyPredicate(itemRepo, filteredPredicate)

        withManyTitles.each {
            def newTitle = GraphCleanUtil.tryGetDataFromNamedGraph(itemRepo, it as String, filteredPredicate, "${resourceUri}/entry#context")
            if (!newTitle)
                return
            itemRepo = GraphCleanUtil.updateGraph(itemRepo, it as String, filteredPredicate, newTitle)
        }

        return itemRepo
    }


    private static def readQueryStringFromFile(def filename) {
        return GraphCleanUtil.class.getResourceAsStream("/sparql/${filename}").getText("utf-8")
    }
}
