import net.sf.json.groovy.JsonSlurper

import se.lagrummet.rinfo.base.rdf.sparqltree.SmartLens
import static se.lagrummet.rinfo.base.rdf.sparqltree.GraphBuilder.buildGraph

import static SparqlTreeViewer.*


/* FIXME:
Classifier doesn't work yet, see <http://jira.codehaus.org/browse/GROOVY-3427>
For now I did this after grape-fail:
    $ curl http://repo2.maven.org/maven2/net/sf/json-lib/json-lib/2.2.3/json-lib-2.2.3-jdk15.jar -o ~/.groovy/grapes/net.sf.json-lib/json-lib/jars/json-lib-2.2.3.jar
*/
@Grab(group='net.sf.json-lib', module='json-lib', version='2.2.3'/*,
        classifier='jdk15'*/)
class ModelViewer {

    static void main(String[] args) {
        def labelTree = new JsonSlurper().parse( new File(
                "../../resources/sparqltrees/model/model_labels.json"))

        def repo = loadRepo("../../resources/",
                ["base/model", "base/extended/rdf", "external/rdf"])

        def query = new File(
                "../../resources/sparqltrees/model/model-tree.rq").text
        def tree = queryToTree(repo, query)

        def st = preparedTemplate(
                "../../resources/sparqltrees/model/model_html.st",
                modelData('sv', tree, labelTree))
        st.setAttribute("encoding", "utf-8")
        println st.toString()
    }

    static Map modelData(locale, tree, labelTree) {
        def lens = new SmartLens(locale)
        tree.remove('someProperty')
        def graph = buildGraph(lens, tree)
        def labels = buildGraph(lens, labelTree)
        return [
            labels: labels,
            ontologies: new ModelViewer(graph, labels).ontologies
        ]
    }


    protected labels
    List ontologies
    List allProperties

    ModelViewer(Map graph, Map labels) {
        this.labels = labels
        this.ontologies = graph.ontology.collect { prepOntology(it) }
        this.allProperties = graph.ontology.collectAll { it.property }.flatten()
    }

    def prepOntology(onto) {
        onto['sorted_classes'] = onto['class'].findAll {
            isDefinedBy(it, onto)
            }. collect {
                prepClass(it)
            }.sort(lsort)
        return onto
    }

    def prepClass(cls) {
        if (cls.ref_via.subClassOf) {
            cls['subclasses'] = cls.ref_via.subClassOf.sort(lsort)
        }
        cls['deprecated'] = cls.type.find {
                it.uri_term == 'DeprecatedClass'
            } != null
        def aggr = [:]
        mergeRestrictionsProperties(cls, aggr)
        cls['merged_restrictions_properties'] = aggr.values().sort(lsort)
        return cls
    }

    def prepProperty(prop) {
        if (prop.ref_via.subPropertyOf) {
            prop['subproperties'] = prop.ref_via.subPropertyOf.sort(lsort)
        }
        return prop
    }

    void mergeRestrictionsProperties(cls, aggr, direct=true) {
        def getSuper = true
        def add = {
            if (!aggr.containsKey(it.resource_uri)) {
                it['direct'] = direct
                aggr[it.resource_uri] = prepProperty(it)
            }
        }
        cls.restriction?.collect { propertyWithRestriction(it) }.each(add)

        allProperties.findAll {
            it.domain && it.domain.resource_uri == cls.resource_uri
        }.each(add)

        if (getSuper && cls.subClassOf) {
            for (superClass in cls.subClassOf) {
                mergeRestrictionsProperties(superClass, aggr, false)
            }
        }
    }

    def propertyWithRestriction(restr) {
        restr['cardinality_label'] = cardinalityLabel(
                labels.cardinalities, restr)
        restr['computed_range'] = computedRange(restr)
        def obj = new HashMap(restr.onProperty)
        obj.restriction=restr
        return obj
    }


    Closure lsort = {
        return it.label?.toLowerCase() ?: it.uri_term.toLowerCase()
    }

    boolean isDefinedBy(o, onto) {
        return (o.isDefinedBy?.resource_uri == onto.resource_uri)
    }

    String cardinalityLabel(labels, restr) {
        return "TODO" /* TODO:
        if restr.cardinality == 0:
            return labels.zero_or_more#at_least_zero
        elif restr.cardinality == 1:
            return labels.exactly_one
        elif restr.cardinality > 1:
            return "%s %s" % (labels.exactly, restr.cardinality)
        else:
            if restr.minCardinality >= 0:
                if restr.minCardinality == 0 and not restr.maxCardinality:
                    return labels.zero_or_more#at_least_zero
                if restr.minCardinality == 0 and restr.maxCardinality == 1:
                    return labels.zero_or_one
                if restr.minCardinality == 1:
                    l = labels.at_least_one
                elif restr.minCardinality:
                    l = "%s %s" % (labels.at_least, restr.minCardinality)
                if l and restr.maxCardinality:
                    l += ", %s %s" % (labels.max, restr.maxCardinality)
                if l:
                    return l
            else:
                return labels.zero_or_more
        */
    }

    def computedRange(restr) {
        return null /* TODO:
        ranges = filter(None,
                [restr.allValuesFrom, restr.someValuesFrom,
                    restr.onProperty.get('range')]
            )
        onto = restr.onProperty.get('isDefinedBy')
        for rg in ranges:
            rg['same_ontology'] = onto and _is_defined_by(rg, onto)
        return ranges
        */
    }

}
