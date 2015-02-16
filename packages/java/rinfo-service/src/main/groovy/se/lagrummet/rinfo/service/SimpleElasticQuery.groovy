package se.lagrummet.rinfo.service

import groovy.transform.CompileStatic
import org.restlet.data.Reference

class SimpleElasticQuery {

    ElasticData elasticData
    String serviceAppBaseUrl

    SimpleElasticQuery(ElasticData elasticData, String serviceAppBaseUrl) {
        this.elasticData = elasticData
        this.serviceAppBaseUrl = serviceAppBaseUrl
        println 'simpleElasticQuery constructor'
    }

    def search(String docType, Reference ref) {
        println 'simple elasticsearch'
    }
}
