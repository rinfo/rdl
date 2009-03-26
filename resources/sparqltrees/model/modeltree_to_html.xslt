<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exslt="http://exslt.org/common"
                xmlns:func="http://exslt.org/functions"
                xmlns:str="http://exslt.org/strings"
                xmlns:lf="tag:localhost,2008:exslt-local-functions"
                extension-element-prefixes="func"
                exclude-result-prefixes="exslt func str lf st"
                xmlns:st="http://oort.to/ns/2008/09/sparqltree"
                xmlns="http://www.w3.org/1999/xhtml"
                >

  <!-- Xalan and javax serialization chain doesn't play with this.
  <xsl:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no"
              doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
              doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
              />
              doctype-public="-//W3C//DTD XHTML+RDFa 1.0//EN"
  -->

  <xsl:param name="ontologyUri"
             >http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#</xsl:param>
  <xsl:param name="lang">sv</xsl:param>

  <xsl:variable name="objects" select="/st:tree/ontology"/>
  <!-- NOTE: These don't work from "within" exslt:node-sets ("real" root is lost).
  <xsl:key name="get-class" match="/st:tree/ontology/class" use="@uri"/>
  <xsl:key name="get-property" match="/st:tree/ontology/property" use="@uri"/>
  -->


  <xsl:template match="/st:tree">
    <xsl:text disable-output-escaping="yes"><![CDATA[<!DOCTYPE html
              PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
              "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">]]></xsl:text>
    <html xml:lang="{$lang}">
      <head profile="http://www.w3.org/ns/rdfa/">
        <title><xsl:value-of select="ontology/title | ontology/label"/></title>
        <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
        <link rel="stylesheet" href="/css/ontology.css" />
      </head>
      <body>
        <div id="main">
          <xsl:apply-templates select="ontology"/>
          <!--
          <xsl:apply-templates select="ontology[@uri=$ontologyUri]"/>
          -->
        </div>
      </body>
    </html>
  </xsl:template>


  <xsl:template match="ontology">
    <div class="ontologyInfo" about="{@uri}">
      <h1><xsl:value-of select="title | ontoLabel"/></h1>
      <xsl:if test="comment">
        <p><xsl:value-of select="ontoComment"/></p>
      </xsl:if>
      <xsl:if test="description">
        <p><xsl:value-of select="description"/></p>
      </xsl:if>
      <xsl:for-each select="class[isDefinedBy/@uri = current()/@uri]">
        <xsl:sort select="label"/>
        <xsl:apply-templates select="."/>
      </xsl:for-each>
    </div>
  </xsl:template>


  <xsl:template match="class">
    <div class="classInfo" about="{@uri}" id="{lf:uri-term(@uri)}">
      <h2><xsl:value-of select="label"/></h2>
      <xsl:variable name="superClassLinks">
        <xsl:for-each select="subClassOf">
          <xsl:variable name="label" select="lf:get-class(@uri)/label"/>
          <xsl:if test="$label != ''">
            <xsl:if test="position() > 1">
              <xsl:text>, </xsl:text>
            </xsl:if>
            <a href="#{lf:uri-term(@uri)}">
              <xsl:value-of select="$label"/>
            </a>
          </xsl:if>
        </xsl:for-each>
      </xsl:variable>
      <xsl:if test="string($superClassLinks) != ''">
        <h3>(en typ av <xsl:copy-of select="$superClassLinks"/>)</h3>
      </xsl:if>
      <xsl:if test="abstract='true'">
        <h4 class="warning">[abstrakt typ]</h4>
      </xsl:if>
      <xsl:if test="classType/@uri = 'http://www.w3.org/2002/07/owl#DeprecatedClass'">
        <h4 class="warning">[obsolet typ]</h4>
      </xsl:if>
      <p class="comment"><xsl:value-of select="comment"/></p>

      <!--
      <xsl:variable name="all-restrictions" select="restriction"/>
      -->
      <xsl:variable name="all-restrictions-rt">
        <xsl:call-template name="copy-restrictions">
          <xsl:with-param name="class" select="."/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="all-restrictions" select="exslt:node-set($all-restrictions-rt)/restriction"/>
      <!-- TODO: should be part of $all-restrictions... -->
      <xsl:variable name="properties-in-domain"
                    select="$objects/property[domain/@uri = current()/@uri]"/>

      <xsl:if test="$all-restrictions | $properties-in-domain">
        <table>
          <thead>
            <th style="width: 22%">Egenskap</th>
            <th style="width: 66%">Beskrivning</th>
            <th style="width: 12%">Förekomst</th>
          </thead>
          <tbody>
            <!-- TODO: remove dups $all-restrictions (if onProperty, don't add from super) -->
            <xsl:for-each select="$all-restrictions">
              <xsl:sort select="lf:get-property(onProperty/@uri)/label"/>
              <xsl:call-template name="table-row">
                <xsl:with-param name="property" select="lf:get-property(onProperty/@uri)"/>
                <xsl:with-param name="restriction" select="."/>
              </xsl:call-template>
            </xsl:for-each>
            <xsl:for-each select="$properties-in-domain">
              <xsl:sort select="label"/>
              <xsl:call-template name="table-row">
                <xsl:with-param name="property" select="."/>
              </xsl:call-template>
            </xsl:for-each>
          </tbody>
        </table>
      </xsl:if>
    </div>
  </xsl:template>

  <xsl:template name="copy-restrictions">
    <xsl:param name="class"/>
    <xsl:param name="collected-restrictions" select="*[false()]"/>
    <xsl:variable name="restrictions" select="$class/restriction"/>
    <xsl:for-each select="$class/subClassOf">
      <xsl:call-template name="copy-restrictions">
        <xsl:with-param name="class" select="lf:get-class(@uri)"/>
        <xsl:with-param name="collected-restrictions" select="$restrictions | $collected-restrictions"/>
      </xsl:call-template>
    </xsl:for-each>
    <xsl:for-each select="$restrictions">
      <xsl:if test="not($collected-restrictions[onProperty/@uri = current()/onProperty/@uri])">
        <xsl:copy-of select="."/>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <func:function name="lf:get-property">
    <xsl:param name="uri"/>
    <func:result select="$objects/property[@uri = $uri]"/>
  </func:function>

  <func:function name="lf:get-class">
    <xsl:param name="uri"/>
    <func:result select="$objects/class[@uri = $uri]"/>
  </func:function>


  <xsl:template name="table-row">
    <xsl:param name="property"/>
    <xsl:param name="restriction"/>
    <tr>
      <td about="{onProperty/@uri}">
        <strong><xsl:value-of select="$property/label"/></strong>
        <xsl:if test="$property/abstract = 'true'">
          <div class="warning">[abstrakt egenskap]</div>
        </xsl:if>
      </td>
      <td>
        <xsl:if test="$property/comment">
          <p><xsl:value-of select="$property/comment"/></p>
        </xsl:if>
        <xsl:if test="$property/abstract = 'true'">
          <p>
            Mer specifika egenskaper:
            <dl>
              <xsl:for-each select="$property/hasSubProperty">
                <xsl:variable name="subprop"
                              select="lf:get-property(@uri)"/>
                <dt><xsl:value-of select="$subprop/label"/></dt>
                <dd>
                  <xsl:value-of select="$subprop/comment"/>
                  <xsl:variable name="range"
                                select="lf:get-class($subprop/range/@uri)"/>
                  <xsl:if test="$range">
                    <em>(<xsl:value-of
                        select="$range/label"/>)
                    </em>
                  </xsl:if>
                </dd>
              </xsl:for-each>
            </dl>
          </p>
        </xsl:if>
        <xsl:if test="$restriction">
          <xsl:for-each select="lf:computed-range($restriction)/label[@xml:lang = $lang]">
            <xsl:variable name="uri" select="../@uri"/>
            <p>
              <em class="rangeType">
                <xsl:text>(Anges som: </xsl:text>
                <xsl:choose>
                  <xsl:when test="starts-with($uri, $ontologyUri)">
                    <a href="#{lf:uri-term($uri)}"><xsl:value-of select="."/></a>
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
      </td>
      <td>
        <xsl:if test="$restriction">
          <span class="cardinalityValue">
            <xsl:call-template name="cardinality-label">
              <xsl:with-param name="restr" select="$restriction"/>
            </xsl:call-template>
          </span>
        </xsl:if>
      </td>
    </tr>
  </xsl:template>

  <func:function name="lf:computed-range">
    <xsl:param name="restr"/>
    <!-- TODO: use someValuesFrom? -->
    <xsl:variable name="classref" select="$restr/allValuesFrom |
                  lf:get-property($restr/onProperty/@uri)/range"/>
    <func:result select="lf:get-class($classref[1]/@uri)"/>
  </func:function>

  <xsl:template name="cardinality-label">
    <xsl:param name="restr"/>
    <xsl:choose>
      <xsl:when test="$restr/cardinality = '0'">
        <xsl:text>noll eller en</xsl:text>
      </xsl:when>
      <xsl:when test="$restr/cardinality = '1'">
        <xsl:text>exakt en</xsl:text>
      </xsl:when>
      <xsl:when test="number($restr/cardinality) > 1">
          <xsl:text>exakt </xsl:text>
          <xsl:value-of select="$restr/cardinality"/>
      </xsl:when>
      <xsl:when test="$restr/minCardinality = '1'">
        <xsl:text>minst en</xsl:text>
        <xsl:if test="$restr/maxCardinality">
          <xsl:text>, max </xsl:text>
          <xsl:value-of select="$restr/maxCardinality"/>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>noll eller flera</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <func:function name="lf:uri-term">
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


</xsl:stylesheet>
