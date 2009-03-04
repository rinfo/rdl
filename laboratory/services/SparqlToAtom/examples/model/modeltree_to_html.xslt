<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:func="http://exslt.org/functions"
                xmlns:lf="tag:localhost,2008:exslt-local-functions"
                extension-element-prefixes="func"
                exclude-result-prefixes="lf"
                xmlns:st="http://oort.to/ns/2008/09/sparqltree"
                xmlns="http://www.w3.org/1999/xhtml">

  <xsl:output method="html" indent="yes" encoding="utf-8" omit-xml-declaration="yes"
              doctype-public="-//W3C//DTD XHTML+RDFa 1.0//EN"/>

  <xsl:key name="get-class" match="/st:tree/ontology/class" use="@uri"/>
  <xsl:key name="get-prop" match="/st:tree/ontology/property" use="@uri"/>

  <xsl:template match="/st:tree">
    <html xml:lang="sv">
      <head profile="http://www.w3.org/ns/rdfa/">
        <title><xsl:value-of select="ontology/title | ontology/label"/></title>
        <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
        <link rel="stylesheet" href="/css/ontology.css" />
      </head>
      <body>
        <div id="main">

        <div class="ontologyInfo" about="{ontology/@uri}">
          <h1><xsl:value-of select="ontology/title | ontology/label"/></h1>
          <xsl:if test="ontology/comment">
            <p><xsl:value-of select="ontology/comment"/></p>
          </xsl:if>
          <xsl:if test="ontology/description">
            <p><xsl:value-of select="ontology/description"/></p>
          </xsl:if>
          <xsl:for-each select="ontology/class">
            <xsl:sort select="label"/>
            <div class="classInfo" about="{@uri}">
              <h2><xsl:value-of select="label"/></h2>
              <xsl:variable name="superClassLabels">
                <xsl:for-each select="subClassOf">
                  <xsl:value-of select="key('get-class', @uri)/label"/>
                </xsl:for-each>
              </xsl:variable>
              <xsl:if test="$superClassLabels != ''">
                <h3>(en typ av <xsl:copy-of select="$superClassLabels"/>)</h3>
              </xsl:if>
              <xsl:if test="abstract='true'">
                <h4 class="warning">[abstrakt typ]</h4>
              </xsl:if>
              <xsl:if test="classType/@uri = 'http://www.w3.org/2002/07/owl#DeprecatedClass'">
                <h4 class="warning">[obsolet typ]</h4>
              </xsl:if>
              <p class="comment"><xsl:value-of select="comment"/></p>
                <xsl:if test="restriction">
                  <table>
                    <thead>
                    <th style="width: 22%">Egenskap</th>
                    <th style="width: 66%">Beskrivning</th>
                    <th style="width: 12%">Förekomst</th>
                    </thead>
                    <tbody>
                      <xsl:for-each select="restriction">
                        <xsl:sort select="key('get-prop', onProperty/@uri)/label"/>
                        <xsl:variable name="prop" select="key('get-prop', onProperty/@uri)"/>
                        <tr>
                          <td about="{onProperty/@uri}">
                            <strong><xsl:value-of select="$prop/label"/></strong>
                            <xsl:if test="$prop/abstract = 'true'">
                              <div class="warning">[abstrakt egenskap]</div>
                            </xsl:if>
                          </td>
                          <td>
                            <xsl:if test="$prop/comment">
                              <p><xsl:value-of select="$prop/comment"/></p>
                            </xsl:if>

                            <xsl:if test="$prop/abstract = 'true'">
                              <p>
                                Mer specifika egenskaper:
                                <dl>
                                  <xsl:for-each select="$prop/hasSubProperty">
                                    <xsl:variable name="subprop"
                                                  select="key('get-prop', @uri)"/>
                                    <dt><xsl:value-of select="$subprop/label"/></dt>
                                    <dd>
                                      <xsl:value-of select="$subprop/comment"/>
                                      <xsl:variable name="range"
                                                    select="key('get-class', $subprop/range/@uri)"/>
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

                            <xsl:for-each select="lf:computed-range(.)/label">
                              <p>
                                <em class="rangeType">
                                  (Anges som: <xsl:value-of select="."/>)
                                </em>
                              </p>
                            </xsl:for-each>
                          </td>
                          <td>
                            <span class="cardinalityValue">
                              <xsl:call-template name="cardinality-label">
                                <xsl:with-param name="restr" select="."/>
                              </xsl:call-template>
                            </span>
                          </td>
                        </tr>
                      </xsl:for-each>
                    </tbody>
                  </table>
                </xsl:if>
                <!--
                <xsl:if test="$show-notes">
                  <div class="notes"> <h3>Anteckningar</h3> </div>
                </xsl:if>
                -->
            </div>
          </xsl:for-each>
        </div>
        </div>

      </body>
    </html>
  </xsl:template>

  <func:function name="lf:computed-range">
    <xsl:param name="restr"/>
    <!-- TODO: use someValuesFrom? -->
    <xsl:variable name="classref" select="$restr/allValuesFrom |
                  key('get-prop', $restr/onProperty/@uri)/range"/>
    <func:result select="key('get-class', $classref[1]/@uri)"/>
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

</xsl:stylesheet>
