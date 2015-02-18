package se.lagrummet.rinfo.service

import static org.restlet.data.CharacterSet.UTF_8

class SimpleElasticQuery {

    private final String REQUEST_QUERY_PARAM_NAME = 'q'

    ElasticData elasticData
    String serviceAppBaseUrl
    RDLQueryBuilder builder

    SimpleElasticQuery(ElasticData elasticData, String serviceAppBaseUrl, RDLQueryBuilder builder) {
        this.elasticData = elasticData
        this.serviceAppBaseUrl = serviceAppBaseUrl
        this.builder = builder;
    }

    Map search(docType, reference) {
        println '++++++++++++++++++++++++++++++++++++++++++++++++++++++ simpleElasticQuery search ++++++++++++++++++++++++++++++++++++++++++++++++++++++++'

        def queryForm = reference.getQueryAsForm(UTF_8)

        def qb = builder.createBuilder()
        try {
            queryForm.getValuesArray(REQUEST_QUERY_PARAM_NAME).collect() { qb.addQuery(it) }

            //queryForm.getValuesArray('type') { qb.restrictType(it)}
            //queryForm.getFirstValue('_stats')

            return createResult(qb.result(serviceAppBaseUrl), reference.getQuery())
        } finally {
            qb.close()
        }
    }

    private Map createResult(RDLQueryBuilder.Result result, String query) {
        def data = [
                "@language": "sv",
                "@context": "/json-ld/context.json",
                "startIndex": 0,
                "itemsPerPage": 50,
                "totalResults": result.items().size(),
                duration: "PT${result.duration()}S" as String,
                //"current": "/-/publ?_page=0&q=r%C3%A4ttsinformationsf%C3%B6rordning"
                "current": "/-/publ?"+query
        ]
        data.items = result.items()
        data.statistics = result.stats()
        return data
    }

/*
    def addPagination(docType, prepSearch, hitsLength, data) {
        def pageParam = pageParamKey + '=' + prepSearch.page
        def currentPage = "/-/${docType}?" +
                ((prepSearch.queryString.indexOf(pageParam) == -1)? "${pageParam}&" : "") +
                prepSearch.queryString
        if (prepSearch.page > 0) {
            data.prev = currentPage.replace(pageParam, pageParamKey + '=' + (prepSearch.page - 1))
        }
        data.current = currentPage as String
        if (hitsLength == data.itemsPerPage &&
                (data.startIndex + data.itemsPerPage) < data.totalResults) {
            data.next = currentPage.replace(pageParam, pageParamKey + '=' + (prepSearch.page + 1))
        }
    }
*/


}

/*    def data = [
            "@language": "sv",
            "@context": jsonLdSettings.ldContextPath,
            startIndex: prepSearch.startIndex,
            itemsPerPage: prepSearch.pageSize,
            totalResults: esRes.hits.totalHits(),

    ]
*/

