<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--DOCTYPE html PUBLIC "-//W3C//DTD XHTML+RDFa 1.0//EN"
          "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd"-->
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="$locale$">
  <head profile="http://www.w3.org/ns/rdfa/">
    <title>$labels.title$</title>
    <meta http-equiv="Content-Type" content="text/html;charset=$encoding$" />
    <link rel="stylesheet" href="$mediabase$/css/ontology.css" />
  </head>
  <body>
    <div id="main">
      $ ontologies: {
        <div class="ontologyInfo" about="$it.resource_uri$">
          <h1>$it.title $$ it.label$</h1>
          $if (it.comment)$ <p>$it.comment$</p> $endif$
          $if (it.description)$ <p>$it.description$</p> $endif$
          $it.sorted_classes: {
            <div class="classInfo" id="$it.uri_term$" about="$it.resource_uri$">
            <h2>$it.label$</h2>
            $if (first(it.subClassOf))$ $if (first(it.subClassOf).label)$
              <h3 class="kindof">($labels.a_kind_of$ $it.subClassOf: {
                <a href="#$it.uri_term$"> $it.label$</a>};
              separator=", "$)</h3>
            $endif$ $endif$
            $if (it.abstract)$
              <h4 class="warning">[$labels.abstract_type$]</h4>
            $endif$
            $if (it.subclasses)$
              <p class="kindof">$labels.more_specific_types$:
                $it.subclasses:{
                    <a href="#$it.uri_term$">$it.label$</a>};
                separator=", "$
              </p>
            $endif$
            $if (it.deprecated)$
              <h4 class="warning">[$labels.obsolete_type$]</h4>
            $endif$
            <p class="comment">$it.comment$</p>
            $if (it.merged_restrictions_properties)$
              <table>
                <thead>
                  <th style="width: 22%">$labels.property$</th>
                  <th style="width: 66%">$labels.description$</th>
                  <th style="width: 12%">$labels.occurrence$</th>
                </thead>
                <tbody>
                  $it.merged_restrictions_properties: {

                    <tr>
                      <td about="$it.resource_uri$">
                        <strong>$if (it.direct)$
                            $it.label$
                          $else$<em>$it.label$</em>
                          $endif$</strong>
                        $if (it.abstract)$
                          <div class="warning">[$labels.abstract_property$]</div>
                        $endif$
                      </td>
                      <td>
                        $if (it.comment)$
                          <p>$it.comment$</p>
                        $endif$

                        $if (it.abstract)$
                        $!if (it.direct)$ $if (it.subproperties)!$
                          <p class="kindof">
                            $labels.more_specific_properties$:
                          </p>
                          <dl>
                            $it.subproperties:{
                              <dt>$it.label$</dt>
                              <dd>
                                $it.comment$
                                $if (it.range)$
                                  <em>($it.range.label$)</em>
                                $endif$
                              </dd>
                            }$
                          </dl>
                        $!endif$ $endif!$
                        $endif$
                        $it.restriction.computed_range:{
                          <p>
                            <em class="rangeType" about="$it.resource_uri$">
                              ($labels.is_given_as$:
                              $if (it.same_ontology)$
                                <a href="#$it.uri_term$">$it.label$</a>
                              $else$
                                $it.label$
                              $endif$)
                            </em>
                          </p>
                        }$
                      </td>
                      <td>
                        $if (it.restriction)$
                          <span class="cardinalityValue"
                            >$it.restriction.cardinality_label$</span>
                        $endif$
                      </td>
                    </tr>

                  }$
                </tbody>
              </table>
            $endif$
          </div>

          }$
        </div>

      }$
    </div>
  </body>
</html>
