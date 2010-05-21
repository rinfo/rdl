<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:owl="http://www.w3.org/2002/07/owl#"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                xmlns:skos="http://www.w3.org/2004/02/skos/core#"
                xmlns:dct="http://purl.org/dc/terms/"
                xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:protege="http://protege.stanford.edu/plugins/owl/protege#"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="exslt func dyn str self grit"
                xmlns:exslt="http://exslt.org/common"
                xmlns:func="http://exslt.org/functions"
                xmlns:dyn="http://exslt.org/dynamic"
                xmlns:str="http://exslt.org/strings"
                xmlns:self="tag:localhost,2010:exslt:self"
                xmlns:grit="http://purl.org/oort/impl/xslt/grit/lib/common#"
                extension-element-prefixes="func">

  <!-- TODO: Really hide things w/o annots in current $lang? See uses of $lang below. -->

  <xsl:import href="../../resources/external/xslt/grit/lib/common.xslt"/>

  <xsl:param name="ontologyUri"
             >http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#</xsl:param>
  <xsl:param name="lang">sv</xsl:param>
  <xsl:param name="mediabase">.</xsl:param>

  <xsl:key name="rel" match="/graph/resource" use="@uri"/>
  <xsl:variable name="r" select="/graph/resource"/>

  <xsl:template match="/graph">
    <xsl:variable name="ontology" select="resource[@uri=$ontologyUri]"/>
    <html xml:lang="{$lang}">
      <head profile="http://www.w3.org/ns/rdfa/">
        <title><xsl:apply-templates select="$ontology/dct:title | $ontology/rdfs:label"/></title>
        <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
        <link rel="stylesheet" href="{$mediabase}/css/ontology.css" />
      </head>
      <body>
        <div id="main">
          <xsl:apply-templates select="$ontology"/>
        </div>
      </body>
    </html>
  </xsl:template>


  <xsl:template match="resource[a/owl:Ontology]">
    <div class="ontologyInfo" about="{@uri}">
      <h1><xsl:apply-templates select="dct:title | rdfs:label"/></h1>
      <xsl:if test="comment">
        <p><xsl:apply-templates select="rdfs:comment"/></p>
      </xsl:if>
      <xsl:if test="dct:description">
        <p><xsl:apply-templates select="dct:description"/></p>
      </xsl:if>
      <dl class="tech">
        <dt>URI:</dt>
        <dd>
          <code><xsl:value-of select="@uri"/></code>
        </dd>
      </dl>
      <xsl:variable name="classes" select="$r[a[rdfs:Class|owl:Class|owl:DeprecatedClass]
                        and rdfs:isDefinedBy/@ref = current()/@uri]"/>
      <!-- TODO: ToC -->
      <xsl:for-each select="$classes">
        <xsl:sort select="rdfs:label"/>
        <xsl:apply-templates select="."/>
      </xsl:for-each>
    </div>
  </xsl:template>


  <xsl:template match="resource[a[rdfs:Class|owl:Class|owl:DeprecatedClass]]">
  <!-- <xsl:template match="resource[a[rdfs:Class|owl:Class]]"> -->
    <xsl:variable name="class" select="."/>
    <xsl:variable name="abstract" select="protege:abstract = 'true'"/>
    <div class="classInfo" about="{@uri}" id="{grit:term(@uri)}">
      <h2><xsl:apply-templates select="rdfs:label"/></h2>
      <xsl:variable name="superClassLinks">
        <xsl:for-each select="rdfs:subClassOf[grit:get(.)/rdfs:label/@xml:lang=$lang]">
          <xsl:variable name="label" select="grit:get(.)/rdfs:label[@xml:lang=$lang]"/>
          <xsl:if test="$label">
            <xsl:if test="position() > 1">
              <xsl:text>, </xsl:text>
            </xsl:if>
            <xsl:choose>
              <xsl:when test="grit:get(.)/rdfs:isDefinedBy/@ref = $class/rdfs:isDefinedBy/@ref">
                <a href="#{grit:term(@ref)}">
                  <xsl:apply-templates select="$label"/>
                </a>
              </xsl:when>
              <xsl:otherwise>
                <xsl:apply-templates select="$label"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:if>
        </xsl:for-each>
      </xsl:variable>
      <xsl:if test="normalize-space(string($superClassLinks)) != ''">
        <h3>(en typ av <xsl:copy-of select="$superClassLinks"/>)</h3>
      </xsl:if>
      <xsl:if test="$abstract">
        <h4 class="warning">[abstrakt typ]</h4>
      </xsl:if>
      <xsl:variable name="subclasses" select="$r[rdfs:subClassOf/@ref = current()/@uri]"/>
      <xsl:if test="$subclasses">
        <p class="kindof">
          <xsl:text>Mer specifika typer: </xsl:text>
          <xsl:for-each select="$subclasses">
            <xsl:sort select="rdfs:label"/>
            <xsl:if test="position() != 1">, </xsl:if>
            <a href="#{grit:term(@uri)}">
              <xsl:apply-templates select="rdfs:label"/>
            </a>
          </xsl:for-each>
        </p>
      </xsl:if>
      <xsl:if test="a/owl:DeprecatedClass">
        <h4 class="warning">[obsolet typ]</h4>
      </xsl:if>
      <p class="comment"><xsl:apply-templates select="rdfs:comment"/></p>
      <dl class="tech">
        <dt>URI:</dt>
        <dd>
          <code><xsl:value-of select="@uri"/></code>
        </dd>
      </dl>

      <xsl:variable name="all-classrefs" select="./@uri | self:super-classes(.)/@uri"/>

      <xsl:variable name="all-restrictions" select="self:get-restrictions(.)"/>
      <xsl:variable name="all-properties" select="self:get-properties($r, .)"/>

      <xsl:variable name="properties-restrs"
                    select="$all-restrictions[grit:get(owl:onProperty)/rdfs:label[
                                              @xml:lang = $lang]]
                            | $all-properties[rdfs:label[@xml:lang = $lang]]"/>

      <xsl:variable name="all-proprefs" select="$all-properties/@uri
                            | $all-restrictions/owl:onProperty/@ref"/>

      <xsl:if test="$properties-restrs">
        <table>
          <thead>
            <th style="width: 22%">Egenskap</th>
            <th style="width: 66%">Beskrivning</th>
            <th style="width: 12%">Förekomst</th>
          </thead>
          <tbody>
            <xsl:for-each select="$properties-restrs">
              <xsl:sort select="rdfs:label[@xml:lang=$lang] | grit:get(owl:onProperty)/rdfs:label[@xml:lang=$lang]"/>
              <xsl:choose>
                <xsl:when test="a/owl:Restriction">
                  <xsl:call-template name="table-row">
                    <xsl:with-param name="property" select="grit:get(owl:onProperty)"/>
                    <xsl:with-param name="all-proprefs" select="$all-proprefs"/>
                    <xsl:with-param name="all-classrefs" select="$all-classrefs"/>
                    <xsl:with-param name="restr" select="."/>
                    <xsl:with-param name="direct"
                                    select="$class/rdfs:subClassOf[a/owl:Restriction and
                                    owl:onProperty/@ref = current()/owl:onProperty/@ref]"/>
                  </xsl:call-template>
                </xsl:when>
                <xsl:when test="not($properties-restrs[a/owl:Restriction and
                                        owl:onProperty/@ref = current()/@uri])">
                  <xsl:call-template name="table-row">
                    <xsl:with-param name="property" select="."/>
                    <xsl:with-param name="all-proprefs" select="$all-proprefs"/>
                    <xsl:with-param name="all-classrefs" select="$all-classrefs"/>
                    <xsl:with-param name="direct"
                                    select="rdfs:domain[@ref = $class/@uri or owl:unionOf[li/@ref = $class/@uri]]"/>
                  </xsl:call-template>
                </xsl:when>
              </xsl:choose>
            </xsl:for-each>
          </tbody>
        </table>
      </xsl:if>
    </div>
  </xsl:template>

  <xsl:template name="table-row">
    <xsl:param name="property"/>
    <xsl:param name="all-proprefs"/>
    <xsl:param name="all-classrefs"/>
    <xsl:param name="restr" select="*[false()]"/>
    <xsl:param name="direct" select="true()"/>
    <xsl:variable name="abstract" select="$property/protege:abstract = 'true'"/>
    <xsl:variable name="sub-props" select="$r[rdfs:subPropertyOf/@ref =
                  $property/@uri and rdfs:label[@xml:lang=$lang] and
                  self:domain-within(., $all-classrefs)]"/>
    <xsl:variable name="has-more-specific"
                  select="count($sub-props[self:contains($all-proprefs, @uri)]) > 0"/>
    <xsl:if test="true()"><!-- not($has-more-specific)">-->
      <tr class="propdef">
        <th about="{$property/@uri}">
          <xsl:variable name="label" select="$property/rdfs:label"/>
          <xsl:choose>
            <xsl:when test="not($direct)">
              <!-- TODO: only use css for inherited, not em! -->
              <em class="inherited"><xsl:apply-templates select="$label"/></em>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="$label"/>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:if test="$abstract">
            <div class="warning">[abstrakt egenskap]</div>
          </xsl:if>
          <xsl:if test="$all-proprefs[. = $property/rdfs:subPropertyOf/@ref]">
            <div class="note">
              <xsl:text>(specifik variant av: </xsl:text>
              <xsl:value-of select="grit:get($property/rdfs:subPropertyOf)/
                                rdfs:label[@xml:lang=$lang]"/>
              <xsl:text>)</xsl:text>
            </div>
          </xsl:if>
          <!-- -->
          <xsl:if test="$has-more-specific">
            <div class="note">(se även mer specifik variant)</div>
          </xsl:if>
          <!-- -->
        </th>
        <td>
          <xsl:if test="$property/rdfs:comment">
            <p><xsl:apply-templates select="$property/rdfs:comment"/></p>
          </xsl:if>
          <xsl:variable name="ranges"
                        select="self:computed-range($property, $restr)/rdfs:label[@xml:lang = $lang]"/>
          <xsl:for-each select="$ranges">
            <xsl:variable name="uri" select="../@uri"/>
            <p>
              <em class="rangeType">
                <xsl:text>(Anges som: </xsl:text>
                <xsl:choose>
                  <xsl:when test="starts-with($uri, $ontologyUri)">
                    <a href="#{grit:term($uri)}"><xsl:apply-templates select="."/></a>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:apply-templates select="."/>
                  </xsl:otherwise>
                </xsl:choose>
                <xsl:text>)</xsl:text>
              </em>
            </p>
          </xsl:for-each>
          <xsl:if test="not($abstract)">
            <dl class="tech">
              <dt>URI:</dt>
              <dd>
                <code><xsl:value-of select="$property/@uri"/></code>
              </dd>
            </dl>
          </xsl:if>
          <!-- TODO: look over if unwanted subprops are pruned by adding limiting ranges -->
          <xsl:if test="$sub-props">
            <p>
              Mer specifika egenskaper:
              <dl>
                <xsl:for-each select="$sub-props">
                  <xsl:sort select="rdfs:label[@xml:lang=$lang]"/>
                  <!-- -->
                  <xsl:choose>
                    <xsl:when test="$has-more-specific">
                      <dt><em><xsl:apply-templates select="rdfs:label"/></em></dt>
                      <dd><em class="note">(se separat beskrivning av denna egenskap)</em></dd>
                    </xsl:when>
                    <xsl:otherwise>
                    <!-- -->
                      <dt><xsl:apply-templates select="rdfs:label"/></dt>
                      <dd>
                        <xsl:apply-templates select="rdfs:comment"/>
                        <xsl:variable name="range"
                                      select="grit:get(rdfs:range)"/>
                        <xsl:if test="$range">
                          <xsl:text> </xsl:text>
                          <em>(<xsl:apply-templates
                                  select="$range/rdfs:label[@xml:lang = $lang]"/>)</em>
                        </xsl:if>
                      </dd>
                    <!-- -->
                    </xsl:otherwise>
                  </xsl:choose>
                  <!-- -->
                </xsl:for-each>
              </dl>
            </p>
          </xsl:if>
        </td>
        <td>
          <span class="cardinalityValue">
            <xsl:call-template name="cardinality-label">
              <xsl:with-param name="restr" select="$restr"/>
            </xsl:call-template>
          </span>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="*[@xml:lang]">
    <xsl:if test="@xml:lang = $lang">
      <xsl:value-of select="."/>
    </xsl:if>
  </xsl:template>


  <xsl:template name="cardinality-label">
    <xsl:param name="restr"/>
    <xsl:variable name="cardinality" select="$restr/owl:cardinality"/>
    <xsl:variable name="minCardinality" select="$restr/owl:minCardinality"/>
    <xsl:variable name="maxCardinality" select="$restr/owl:maxCardinality"/>
    <xsl:choose>
      <xsl:when test="$cardinality = 0">
        <!-- TODO:? is this "not allowed"? -->
        <xsl:text>noll</xsl:text>
      </xsl:when>
      <xsl:when test="$cardinality = 1">
        <xsl:text>exakt en</xsl:text>
      </xsl:when>
      <xsl:when test="number($cardinality) > 1">
          <xsl:text>exakt </xsl:text>
          <xsl:value-of select="$cardinality"/>
      </xsl:when>
      <xsl:when test="$minCardinality">
        <xsl:choose>
          <xsl:when test="$minCardinality = 0 and not($maxCardinality)">
            <xsl:text>noll eller flera</xsl:text>
          </xsl:when>
          <xsl:when test="$minCardinality = 0 and $maxCardinality = 1">
            <xsl:text>noll eller en</xsl:text>
          </xsl:when>
          <xsl:when test="$minCardinality = 1">
            <xsl:text>minst en</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>minst </xsl:text>
            <xsl:value-of select="$minCardinality"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="$restr/owl:someValuesFrom">
        <xsl:text>minst en</xsl:text>
      </xsl:when>
      <xsl:when test="not($maxCardinality)">
        <xsl:text>valfri</xsl:text>
      </xsl:when>
    </xsl:choose>
    <xsl:if test="$maxCardinality and not($minCardinality = 0 and $maxCardinality = 1)">
      <xsl:if test="$minCardinality">, </xsl:if>
      <xsl:text>max </xsl:text>
      <xsl:value-of select="$maxCardinality"/>
    </xsl:if>
  </xsl:template>

  <xsl:template name="markup-example">
    <xsl:variable name="term" select="grit:term(@uri)"/>
    <xsl:variable name="base" select="substring-before(@uri, $term)"/>
    <xsl:variable name="ns" select="document('')/*/namespace::*"/>
    <xsl:variable name="name">
      <xsl:value-of select="local-name($ns[string(.) = $base])"/>
      <xsl:text>:</xsl:text>
      <xsl:value-of select="$term"/>
    </xsl:variable>
    <code>
      <xsl:choose>
        <xsl:when test="a/owl:Class | a/rdfs:Class | a/owl:DeprecatedClass">
          &lt;<xsl:value-of select="$name"/> rdf:about="...">
        </xsl:when>
        <xsl:when test="a/owl:ObjectProperty and (rdfs:range or not(a/rdf:Property)) and
                      not(rdfs:range/@ref = 'http://www.w3.org/2000/01/rdf-schema#Literal')">
          &lt;<xsl:value-of select="$name"/> rdf:resource="..."/>
        </xsl:when>
        <xsl:when test="a/owl:DatatypeProperty and rdfs:range and
                      rdfs:range/@ref != 'http://www.w3.org/2000/01/rdf-schema#Literal'">
          &lt;<xsl:value-of select="$name"/> datatype="<xsl:value-of select="rdfs:range/@ref"/>">... &lt;/<xsl:value-of select="$name"/>>
        </xsl:when>
        <xsl:when test="a/owl:DatatypeProperty or a/owl:AnnotationProperty or a/rdf:Property">
          &lt;<xsl:value-of select="$name"/> ...>...&lt;/<xsl:value-of select="$name"/>>
        </xsl:when>
      </xsl:choose>
    </code>
  </xsl:template>


  <func:function name="self:get-restrictions">
    <xsl:param name="class"/>
    <xsl:param name="collected-restrictions" select="*[false()]"/>
    <xsl:variable name="restrictions"
                  select="$class/rdfs:subClassOf[a/owl:Restriction and
                  not(self:contains($collected-restrictions/owl:onProperty/@ref,
                                    owl:onProperty/@ref))]"/>
    <xsl:variable name="supers"
                  select="dyn:map($class/rdfs:subClassOf[not(a/owl:Restriction)],
                                  'grit:get(.)')"/>
    <func:result select="$restrictions |
                    dyn:map($supers, 'self:get-restrictions(.,
                        $restrictions | $collected-restrictions)')"/>
  </func:function>

  <func:function name="self:get-properties">
    <xsl:param name="r"/><!-- TODO: why does xalan need $r here but not in grit:get? -->
    <xsl:param name="class"/>
    <func:result select="$r[rdfs:domain[@ref = $class/@uri or owl:unionOf[li/@ref = $class/@uri]]] |
                    dyn:map(self:super-classes($class), 'self:get-properties($r, .)')"/>
  </func:function>

  <func:function name="self:super-classes">
    <xsl:param name="e"/>
    <xsl:variable name="supers" select="dyn:map($e/rdfs:subClassOf, 'grit:get(.)')"/>
    <func:result select="$supers | dyn:map($supers, 'self:super-classes(.)')"/>
  </func:function>

  <func:function name="self:super-properties">
    <xsl:param name="e"/>
    <xsl:variable name="supers" select="dyn:map($e/rdfs:subPropertyOf, 'grit:get(.)')"/>
    <func:result select="$supers | dyn:map($supers, 'self:super-properties(.)')"/>
  </func:function>

  <func:function name="self:computed-range">
    <xsl:param name="property"/>
    <xsl:param name="restr"/>
    <xsl:choose>
      <xsl:when test="$restr/owl:allValuesFrom">
        <func:result select="grit:get($restr/owl:allValuesFrom)"/>
      </xsl:when>
      <!-- TODO: really use someValuesFrom? (means "at least one of the type") -->
      <xsl:when test="$restr/owl:someValuesFrom">
        <func:result select="grit:get($restr/owl:someValuesFrom)"/>
      </xsl:when>
      <xsl:when test="grit:get($restr/owl:onProperty)/rdfs:range">
        <func:result select="grit:get(grit:get($restr/owl:onProperty)/rdfs:range)"/>
      </xsl:when>
      <xsl:when test="$property/rdfs:range">
        <func:result select="grit:get($property/rdfs:range)"/>
      </xsl:when>
      <xsl:otherwise>
        <func:result select="*[false()]"/>
      </xsl:otherwise>
    </xsl:choose>
  </func:function>

  <func:function name="self:domain-within">
    <xsl:param name="node"/>
    <xsl:param name="classrefs"/>
    <xsl:variable name="domains"
                  select="$node/rdfs:domain[@ref] | $node/rdfs:domain/*[@ref] |
                      self:super-properties($node)/rdfs:domain[@ref] |
                      self:super-properties($node)/rdfs:domain/*[@ref]"/>
    <xsl:variable name="matched-domain">
      <xsl:for-each select="$domains">
        <xsl:if test="self:contains($classrefs, @ref)">TRUE</xsl:if>
      </xsl:for-each>
    </xsl:variable>
    <func:result select="not($domains) or contains($matched-domain, 'TRUE')"/>
  </func:function>

  <func:function name="self:contains">
    <xsl:param name="nodes"/>
    <xsl:param name="node"/>
    <func:result select="count($nodes[. = $node]) > 0"/>
  </func:function>

</xsl:stylesheet>
