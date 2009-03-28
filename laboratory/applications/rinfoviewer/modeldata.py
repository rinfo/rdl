from oort.sparqltree.autotree import URI_KEY
from oort.sparqltree.treelens import AttrDict, TreeLens


class ModelData(object):

    def __init__(self, locale, tree, labeltree):
        del tree['someProperty']
        self.locale = locale
        self.lens = TreeLens(tree, locale, decorator=self._uri_decorator)
        self.labels = TreeLens(labeltree, locale)
        self._all_properties=[prop for onto in self.lens.ontology
                                for prop in onto.property]
        self.ontologies = [self._ontology(it) for it in self.lens.ontology]

    def _uri_decorator(self, node):
        uri = node.get(URI_KEY)
        if uri:
            node['uri'] = uri
            node['uri_term'] = self.uri_term(uri)

    def _ontology(self, onto):
        onto['sorted_classes'] = sorted(
                ( self._a_class(c) for c in onto['class']
                    if self._is_defined_by(c, onto) ),
                key=self._lsort )
        return onto

    def _a_class(self, c):
        aggr = {}
        self._merge_restrictions_properties(c, aggr)
        c['merged_restrictions_properties'] = sorted(
                aggr.values(), key=self._lsort)
        return c

    def _merge_restrictions_properties(self, c, aggr, direct=True):
        get_super = True
        def add(prop):
            if prop.uri not in aggr:
                prop['direct'] = direct
                aggr[prop.uri] = prop
        if 'restriction' in c:
            for rp in map(self._property_with_restriction, c.restriction):
                add(rp)
        for prop in self._all_properties:
            if prop.domain and prop.domain.uri == c.uri:
                add(prop)
        if get_super and 'subClassOf' in c:
            for sup in c.subClassOf:
                self._merge_restrictions_properties(sup, aggr, False)

    def _property_with_restriction(self, restr):
        restr['cardinality_label'] = self._cardinality_label(restr)
        restr['computed_range'] = self._computed_range(restr)
        return AttrDict(restr.onProperty, restriction=restr)

    def _computed_range(self, restr):
        # TODO:? Flatten if these are lists..
        ranges = filter(None,
                [restr.allValuesFrom, restr.someValuesFrom,
                    restr.onProperty.get('range')]
            )
        onto = restr.onProperty.get('isDefinedBy')
        for rg in ranges:
            rg['same_ontology'] = onto and self._is_defined_by(rg, onto)
        return ranges

    def _cardinality_label(self, restr):
        labels = self.labels.cardinalities
        if restr.cardinality == 0:
            return labels.zero_or_one
        elif restr.cardinality == 1:
            return labels.exactly_one
        elif restr.cardinality > 1:
            return "%s %s" % (labels.exactly, restr.cardinality)
        # TODO: any minCardinality
        elif restr.minCardinality == 1:
            l = labels.at_least_one
            if restr.maxCardinality:
                l += ", %s %s" % (labels.max, restr.maxCardinality)
            return l
        else:
            return "noll eller flera"

    @staticmethod
    def _is_defined_by(o, onto):
        return o.get('isDefinedBy') and o.isDefinedBy.uri == onto.uri

    @staticmethod
    def _lsort(it):
        return it.label.lower() if (
                'label' in it and isinstance(it.label, basestring)
            ) else it.uri_term.lower()

    @staticmethod
    def uri_term(uri):
        if '#' in uri:
            return uri.rsplit('#', 1)[-1]
        else:
            return uri.rsplit('/', 1)[-1]


