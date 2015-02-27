package se.lagrummet.rinfo.service

import se.lagrummet.rinfo.service.elasticsearch.ElasticSearchQueryBuilder
import se.lagrummet.rinfo.service.elasticsearch.impl.ElasticSearchQueryBuilderImpl

import static org.restlet.data.CharacterSet.UTF_8

class SimpleElasticQuery {

    private final def CONST = [
            requestQueryParam: "q",
            requestTypeParam: "type",
            defaultPageSize:50,
            pageParamKey: '_page',
            pageSizeParamKey: '_pageSize',
            statsParam: '_stats',
    ]

    ElasticData elasticData
    String serviceAppBaseUrl
    ElasticSearchQueryBuilder builder

    SimpleElasticQuery(ElasticData elasticData, String serviceAppBaseUrl) {
        this.elasticData = elasticData
        this.serviceAppBaseUrl = serviceAppBaseUrl
        this.builder = new ElasticSearchQueryBuilderImpl(elasticData)
    }

    Map search(docType, reference) {
        println '++++++++++++++++++++++++++++++++++++++++++++++++++++++ simpleElasticQuery search ++++++++++++++++++++++++++++++++++++++++++++++++++++++++'

        def queryForm = reference.getQueryAsForm(UTF_8)

        def qb = builder.createBuilder()
        try {
            boolean first = true;
            queryForm.getValuesArray(CONST.requestQueryParam).each { if (first) { qb.addQuery(it); first=false; } else qb.addSynonym(it); }
            int page = queryForm.getFirstValue(CONST.pageParamKey)?.toInteger()?:0
            int pageSize = queryForm.getFirstValue(CONST.pageSizeParamKey)?.toInteger()?:CONST.defaultPageSize
            println "se.lagrummet.rinfo.service.SimpleElasticQuery.search page=${page} pageSize=${pageSize}"
            if (page<0||pageSize<0)
                return [:]

            qb.setPagination(page, pageSize)

            queryForm.getValuesArray(CONST.requestTypeParam).collect { qb.restrictType(it)}
            //queryForm.getFirstValue(CONST.statsParam)

            return createResult(qb.result(serviceAppBaseUrl), reference.getQuery())
        } finally {
            qb.close()
        }
    }

    private Map createResult(ElasticSearchQueryBuilder.Result result, String query) {
        def data = [
                "@language": "sv",
                "@context": "/json-ld/context.json",
                "startIndex": result.startIndex(),
                "itemsPerPage": result.pageSize(),
                "totalResults": result.totalHits(),
                duration: "PT${result.duration()}S" as String,
        ]
        data.items = result.items()
        data.statistics = result.stats()
        addPagination("publ", [page: result.page(), queryString: query], result.hitsLength(), data)
        return data
    }


    def addPagination(docType, prepSearch, hitsLength, data) {
        def pageParam = CONST.pageParamKey + '=' + prepSearch.page
        def currentPage = "/-/${docType}?" +
                ((prepSearch.queryString.indexOf(pageParam) == -1)? "${pageParam}&" : "") +
                prepSearch.queryString
        if (prepSearch.page > 0) {
            data.prev = currentPage.replace(pageParam, CONST.pageParamKey + '=' + (prepSearch.page - 1))
        }
        data.current = currentPage as String
        if (hitsLength == data.itemsPerPage &&
                (data.startIndex + data.itemsPerPage) < data.totalResults) {
            data.next = currentPage.replace(pageParam, CONST.pageParamKey + '=' + (prepSearch.page + 1))
        }
    }



}

/*    def data = [
            "@language": "sv",
            "@context": jsonLdSettings.ldContextPath,
            startIndex: prepSearch.startIndex,
            itemsPerPage: prepSearch.pageSize,
            totalResults: esRes.hits.totalHits(),

    ]
*/

