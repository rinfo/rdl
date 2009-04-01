from oort.sparqltree.treelens import PlainTreeLens, AttrDict


class ModelData(object):

    def __init__(self, locale, tree, labeltree):
        self.locale = locale
        self.labels = PlainTreeLens(labeltree, locale)
        tree.pop('someProperty', None)
        lens = PlainTreeLens(tree, locale)
        self._all_properties = [prop for onto in lens.ontology
                                for prop in onto.property]
        self.ontologies = [self._prep_ontology(it) for it in lens.ontology]

    def _prep_ontology(self, onto):
        onto['sorted_classes'] = sorted(
                (self._prep_class(c) for c in onto['class']
                    if _is_defined_by(c, onto)),
                key=_lsort)
        return onto

    def _prep_class(self, aclass):
        if 'subClassOf' in aclass.ref_via:
            aclass['subclasses'] = sorted(
                    aclass.ref_via.subClassOf, key=_lsort)
        aclass['deprecated'] = any(t.uri_term == 'DeprecatedClass'
                for t in aclass.type)
        aggr = {}
        self._merge_restrictions_properties(aclass, aggr)
        aclass['merged_restrictions_properties'] = sorted(
                aggr.values(), key=_lsort)
        return aclass

    def _prep_property(self, prop):
        if 'subPropertyOf' in prop.ref_via:
            prop['subproperties'] = sorted(
                    prop.ref_via.subPropertyOf, key=_lsort)
        return prop

    def _merge_restrictions_properties(self, aclass, aggr, direct=True):
        get_super = True
        def add(prop):
            if prop.resource_uri not in aggr:
                prop['direct'] = direct
                aggr[prop.resource_uri] = self._prep_property(prop)

        if 'restriction' in aclass:
            for rp in map(self._property_with_restriction, aclass.restriction):
                add(rp)
        for prop in self._all_properties:
            if prop.domain and prop.domain.resource_uri == aclass.resource_uri:
                add(prop)

        if get_super and 'subClassOf' in aclass:
            for sup_class in aclass.subClassOf:
                self._merge_restrictions_properties(sup_class, aggr, False)

    def _property_with_restriction(self, restr):
        restr['cardinality_label'] = _cardinality_label(
                self.labels.cardinalities, restr)
        restr['computed_range'] = _computed_range(restr)
        return AttrDict(restr.onProperty, restriction=restr)


def _is_defined_by(o, onto):
    return (o.get('isDefinedBy') and
            o.isDefinedBy.resource_uri == onto.resource_uri)

def _lsort(it):
    return it.label.lower() if (
            'label' in it and isinstance(it.label, basestring)
        ) else it.uri_term.lower()

def _cardinality_label(labels, restr):
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

def _computed_range(restr):
    # TODO:? Flatten if these are lists..
    ranges = filter(None,
            [restr.allValuesFrom, restr.someValuesFrom,
                restr.onProperty.get('range')]
        )
    onto = restr.onProperty.get('isDefinedBy')
    for rg in ranges:
        rg['same_ontology'] = onto and _is_defined_by(rg, onto)
    return ranges


