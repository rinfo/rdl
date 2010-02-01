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
                xmlns:bibo="http://purl.org/ontology/bibo/"
                xmlns:rpubl="http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#"
                xmlns:protege="http://protege.stanford.edu/plugins/owl/protege#"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="exslt func dyn str self grit"
                xmlns:exslt="http://exslt.org/common"
                xmlns:func="http://exslt.org/functions"
                xmlns:dyn="http://exslt.org/dynamic"
                xmlns:str="http://exslt.org/strings"
                xmlns:self="tag:localhost,2010:exslt:self"
                xmlns:grit="http://purl.org/oort/impl/xslt/grit/grit-util#"
                extension-element-prefixes="func">

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
        <title><xsl:value-of select="$ontology/dct:title | $ontology/rdfs:label"/></title>
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
      <h1><xsl:value-of select="dct:title | rdfs:label"/></h1>
      <xsl:if test="comment">
        <p><xsl:value-of select="rdfs:comment"/></p>
      </xsl:if>
      <xsl:if test="dct:description">
        <p><xsl:value-of select="dct:description"/></p>
      </xsl:if>
      <dl class="tech">
        <dt>URI:</dt>
        <dd>
          <code><xsl:value-of select="@uri"/></code>
        </dd>
      </dl>
      <xsl:for-each select="$r[a[rdfs:Class|owl:Class|owl:DeprecatedClass]
                    and rdfs:isDefinedBy/@ref = current()/@uri]">
        <xsl:sort select="rdfs:label"/>
        <xsl:apply-templates select="."/>
      </xsl:for-each>
    </div>
  </xsl:template>


  <xsl:template match="resource[a[rdfs:Class|owl:Class|owl:DeprecatedClass]]">
    <xsl:variable name="abstract" select="protege:abstract = 'true'"/>
    <div class="classInfo" about="{@uri}" id="{self:uri-term(@uri)}">
      <h2><xsl:value-of select="rdfs:label"/></h2>
      <xsl:variable name="superClassLinks">
        <xsl:for-each select="rdfs:subClassOf">
          <xsl:variable name="label" select="grit:get(.)/rdfs:label"/>
          <xsl:if test="$label[@xml:lang=$lang]">
            <xsl:if test="position() > 1">
              <xsl:text>, </xsl:text>
            </xsl:if>
            <a href="#{self:uri-term(@ref)}">
              <xsl:value-of select="$label"/>
            </a>
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
            <a href="#{self:uri-term(@uri)}">
              <xsl:value-of select="rdfs:label"/>
            </a>
          </xsl:for-each>
        </p>
      </xsl:if>
      <xsl:if test="a/owl:DeprecatedClass">
        <h4 class="warning">[obsolet typ]</h4>
      </xsl:if>
      <p class="comment"><xsl:value-of select="rdfs:comment"/></p>
      <dl class="tech">
        <dt>URI:</dt>
        <dd>
          <code><xsl:value-of select="@uri"/></code>
        </dd>
        <xsl:if test="not($abstract)">
          <dt>Som XML:</dt>
          <dd>
            <xsl:call-template name="markup-example"/>
          </dd>
        </xsl:if>
      </dl>

      <xsl:variable name="all-restrictions" select="self:get-restrictions(.)"/>
      <xsl:variable name="all-properties" select="self:get-properties($r, .)"/>

      <xsl:variable name="properties-restrs"
                    select="$all-restrictions[grit:get(owl:onProperty)/rdfs:label[
                                              @xml:lang = $lang]]
                            | $all-properties[rdfs:label[@xml:lang = $lang]]"/>

      <xsl:if test="$properties-restrs">
        <table>
          <thead>
            <th style="width: 22%">Egenskap</th>
            <th style="width: 66%">Beskrivning</th>
            <th style="width: 12%">Förekomst</th>
          </thead>
          <tbody>
            <xsl:variable name="class" select="."/>
            <xsl:for-each select="$properties-restrs">
              <xsl:sort select="rdfs:label | grit:get(owl:onProperty)/rdfs:label"/>
              <xsl:choose>
                <xsl:when test="a/owl:Restriction">
                  <xsl:call-template name="table-row">
                    <xsl:with-param name="property" select="grit:get(owl:onProperty)"/>
                    <xsl:with-param name="restr" select="."/>
                    <xsl:with-param name="direct"
                                    select="$class/rdfs:subClassOf[a/owl:Restriction and
                                    owl:onProperty/@ref = current()/owl:onProperty/@ref]"/>
                  </xsl:call-template>
                </xsl:when>
                <xsl:when test="not(preceding::*[a/owl:Restriction and
                                        owl:onProperty/@ref = current()/@uri])">
                  <xsl:call-template name="table-row">
                    <xsl:with-param name="property" select="."/>
                    <xsl:with-param name="direct" select="rdfs:domain/@ref = $class/@uri"/>
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
    <xsl:param name="restr" select="*[false()]"/>
    <xsl:param name="direct" select="true()"/>
    <xsl:variable name="abstract" select="$property/protege:abstract = 'true'"/>
    <tr class="propdef">
      <th about="{$property/@uri}">
        <xsl:variable name="label" select="$property/rdfs:label"/>
        <xsl:choose>
          <xsl:when test="not($direct)">
            <!-- TODO: only use css for inherited, not em! -->
            <em class="inherited"><xsl:value-of select="$label"/></em>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$label"/>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="$abstract">
          <div class="warning">[abstrakt egenskap]</div>
        </xsl:if>
      </th>
      <td>
        <xsl:if test="$property/rdfs:comment">
          <p><xsl:value-of select="$property/rdfs:comment"/></p>
        </xsl:if>
        <xsl:if test="$abstract">
          <p>
            Mer specifika egenskaper:
            <dl>
              <xsl:for-each select="$r[rdfs:subPropertyOf/@ref = $property/@uri]">
                <xsl:sort select="rdfs:label"/>
                <dt><xsl:value-of select="rdfs:label"/></dt>
                <dd>
                  <xsl:value-of select="rdfs:comment"/>
                  <xsl:variable name="range"
                                select="grit:get(rdfs:range)"/>
                  <xsl:if test="$range">
                    <xsl:text> </xsl:text>
                    <em>(<xsl:value-of
                        select="$range/rdfs:label"/>)
                    </em>
                  </xsl:if>
                </dd>
              </xsl:for-each>
            </dl>
          </p>
        </xsl:if>
        <xsl:if test="$restr">
          <xsl:for-each select="self:computed-range($restr)/rdfs:label[@xml:lang = $lang]">
            <xsl:variable name="uri" select="../@uri"/>
            <p>
              <em class="rangeType">
                <xsl:text>(Anges som: </xsl:text>
                <xsl:choose>
                  <xsl:when test="starts-with($uri, $ontologyUri)">
                    <a href="#{self:uri-term($uri)}"><xsl:value-of select="."/></a>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="."/>
                  </xsl:otherwise>
                </xsl:choose>
                <xsl:text>)</xsl:text>
              </em>
            </p>
          </xsl:for-each>
        </xsl:if>
        <xsl:if test="not($abstract)">
          <dl class="tech">
            <dt>URI:</dt>
            <dd>
              <code><xsl:value-of select="$property/@uri"/></code>
            </dd>
            <dt>Som XML:</dt>
            <dd>
              <xsl:for-each select="$property">
                <xsl:call-template name="markup-example"/>
              </xsl:for-each>
            </dd>
          </dl>
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
  </xsl:template>


  <xsl:template name="cardinality-label">
    <xsl:param name="restr"/>
    <xsl:variable name="cardinality" select="$restr/owl:cardinality"/>
    <xsl:variable name="minCardinality" select="$restr/owl:minCardinality"/>
    <xsl:variable name="maxCardinality" select="$restr/owl:maxCardinality"/>
    <xsl:choose>
      <xsl:when test="$cardinality = 0">
        <!-- TODO: isn't this "not allowed"? -->
        <xsl:text>noll eller flera</xsl:text>
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
    <xsl:variable name="term" select="self:uri-term(@uri)"/>
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
        <xsl:when test="a/owl:ObjectProperty and (rdfs:range or not(a/rdf:Property))">
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
    <func:result select="$r[rdfs:domain/@ref = $class/@uri] |
                    dyn:map(self:super-classes($class), 'self:get-properties($r, .)')"/>
  </func:function>

  <func:function name="self:super-classes">
    <xsl:param name="e"/>
    <xsl:param name="recursive" select="true()"/>
    <xsl:variable name="supers" select="dyn:map($e/rdfs:subClassOf, 'grit:get(.)')"/>
    <xsl:choose>
      <xsl:when test="$recursive">
        <func:result select="$supers | dyn:map($supers, 'self:super-classes(., $recursive)')"/>
      </xsl:when>
      <xsl:otherwise>
        <func:result select="$supers"/>
      </xsl:otherwise>
    </xsl:choose>
  </func:function>


  <func:function name="self:computed-range">
    <xsl:param name="restr"/>
    <!-- TODO: really use someValuesFrom? (isn't it more like an "example range"?) -->
    <xsl:variable name="classref" select="
                  $restr/owl:allValuesFrom | $restr/owl:someValuesFrom |
                  grit:get($restr/owl:onProperty)/rdfs:range"/>
    <func:result select="grit:get($classref[1])"/>
  </func:function>

  <func:function name="self:contains">
    <xsl:param name="nodes"/>
    <xsl:param name="node"/>
    <func:result select="count($nodes[. = $node]) > 0"/>
  </func:function>

  <func:function name="self:uri-term">
    <xsl:param name="uri"/>
    <xsl:choose>
      <xsl:when test="contains($uri, '#')">
        <func:result select="substring-after($uri, '#')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="str:split($uri, '/')[position() = last()]">
          <func:result select="current()"/>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </func:function>

  <func:function name="grit:get">
    <xsl:param name="e"/>
    <xsl:choose>
      <xsl:when test="$e/@ref">
        <func:result select="$r[@uri = $e/@ref]"/>
      </xsl:when>
      <xsl:otherwise>
        <func:result select="$e"/>
      </xsl:otherwise>
    </xsl:choose>
  </func:function>

</xsl:stylesheet>
