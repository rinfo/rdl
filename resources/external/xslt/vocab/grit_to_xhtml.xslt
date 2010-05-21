<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:owl="http://www.w3.org/2002/07/owl#"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                xmlns:skos="http://www.w3.org/2004/02/skos/core#"
                xmlns:dcelem="http://purl.org/dc/elements/1.1/"
                xmlns:dct="http://purl.org/dc/terms/"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:gr="http://purl.org/oort/impl/xslt/grit/lib/common#">

  <xsl:import href="../grit/lib/common.xslt"/>

  <xsl:param name="default-lang">en</xsl:param>
  <xsl:param name="lang">
    <xsl:choose>
      <xsl:when test="//*/@xml:lang">
        <xsl:value-of select="//*/@xml:lang[1]"/>
      </xsl:when>
      <xsl:otherwise>
          <xsl:value-of select="$default-lang"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:param>
  <xsl:param name="localized" select="true()"/>
  <xsl:param name="labels-url" select="'labels.xml'"/>
  <xsl:param name="mediabase">.</xsl:param>
  <xsl:param name="vocab-uri" select="$r[a/owl:Ontology][1]/@uri"/>

  <xsl:variable name="l" select="document($labels-url)/labels/label[
                not($localized) or not(../label/@xml:lang = $lang)
                  and @xml:lang = $default-lang
                or @xml:lang = $lang]"/>

  <xsl:variable name="vocab" select="$r[@uri = $vocab-uri]"/>

  <xsl:output method="xml" encoding="utf-8"
              omit-xml-declaration="yes" indent="yes"/>

  <xsl:template match="/">
    <html lang="{$lang}" xml:lang="{$lang}">
      <xsl:variable name="title">
        <xsl:apply-templates mode="label" select="$vocab"/>
        <xsl:text> | </xsl:text>
        <xsl:value-of select="$l[@name='vocabspec']"/>
      </xsl:variable>
      <head>
        <title><xsl:value-of select="$title"/></title>
        <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
        <!--
        <meta name="author" content="" />
        <meta name="copyright" content="" />
        <meta name="language" content="{$lang}" />
        -->
        <link rel="stylesheet" href="{$mediabase}/css/vocab.css" type="text/css" />
      </head>
      <body>
          <div id="main" role="main">
            <xsl:apply-templates select="$vocab"/>
          </div>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="*[a/owl:Ontology]">
    <xsl:variable name="defs" select="$r[rdfs:isDefinedBy/@ref=current()/@uri]"/>
    <xsl:variable name="classes" select="$defs[a/*[contains(local-name(), 'Class')]]"/>
    <xsl:variable name="properties" select="$defs[a/*[contains(local-name(), 'Property')]]"/>

    <div id="vocab">

      <h1><xsl:apply-templates mode="label" select="."/></h1>

      <div id="toc" role="navigation">
          <h2><xsl:value-of select="$l[@name='table-of-contents']"/></h2>
        <ul>
          <li><a href="#details"><xsl:value-of select="$l[@name='vocabulary']"/></a></li>
          <li><a href="#hierarchy"><xsl:value-of select="$l[@name='hierarchy']"/></a></li>
          <li><a href="#classes"><xsl:value-of select="$l[@name='classes']"/></a></li>
          <li><a href="#properties"><xsl:value-of select="$l[@name='properties']"/></a></li>
          <li><a href="#reference"><xsl:value-of select="$l[@name='reference']"/></a></li>
          <li><a href="#license"><xsl:value-of select="$l[@name='license']"/></a></li>
          <li><a href="#changes"><xsl:value-of select="$l[@name='changes']"/></a></li>
        </ul>
      </div>

      <p>
        <xsl:apply-templates select="dct:description | dcelem:description | rdfs:comment"/>
      </p>

      <div id="details" class="section">
          <h2><xsl:value-of select="$l[@name='vocabulary']"/></h2>
        <xsl:call-template name="termdefs">
          <xsl:with-param name="pre">
              <dt><xsl:value-of select="$l[@name='namespace-uri']"/></dt>
            <dd>
              <a href="{@uri}">
                <code><xsl:value-of select="@uri"/></code>
              </a>
            </dd>
          </xsl:with-param>
        </xsl:call-template>
        <div id="defs" role="navigation">
            <h3><xsl:value-of select="$l[@name='definitions']"/></h3>
          <dl>
              <dt>
                <xsl:value-of select="count($classes)"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="$l[@name='classes-lc']"/>
              </dt>
            <dd>
              <ul>
                <xsl:for-each select="$classes">
                  <xsl:sort select="rdfs:label"/>
                  <xsl:sort select="@uri"/>
                  <li>
                    <xsl:apply-templates mode="ref" select="."/>
                  </li>
                </xsl:for-each>
              </ul>
            </dd>
            <dt>
              <xsl:value-of select="count($properties)"/>
              <xsl:text> </xsl:text>
              <xsl:value-of select="$l[@name='properties-lc']"/>
            </dt>
            <dd>
              <ul>
                <xsl:for-each select="$properties">
                  <xsl:sort select="rdfs:label"/>
                  <xsl:sort select="@uri"/>
                  <li>
                    <xsl:apply-templates mode="ref" select="."/>
                  </li>
                </xsl:for-each>
              </ul>
            </dd>
          </dl>
        </div>
      </div>

      <div id="hierarchy" class="col-2 section">
        <div class="header">
          <h2><xsl:value-of select="$l[@name='hierarchy']"/></h2>
          <a class="tool" href="#main">[top]</a>
        </div>
        <div class="col">
          <h3><xsl:value-of select="$l[@name='classes']"/></h3>
          <ul class="classes">
            <xsl:for-each select="$classes[
                          not( gr:get(rdfs:subClassOf)/rdfs:isDefinedBy/@ref =
                          rdfs:isDefinedBy/@ref )]">
              <xsl:sort select="rdfs:label"/>
              <xsl:sort select="@uri"/>
              <li>
                  <xsl:apply-templates mode="ref" select="."/>
                  <xsl:call-template name="hierarchy">
                    <xsl:with-param name="ns" select="'http://www.w3.org/2000/01/rdf-schema#'"/>
                    <xsl:with-param name="term" select="'subClassOf'"/>
                  </xsl:call-template>
              </li>
            </xsl:for-each>
          </ul>
        </div>
        <div class="col">
          <h3><xsl:value-of select="$l[@name='properties']"/></h3>
          <ul class="properties">
            <xsl:for-each select="$properties[
                          not( gr:get(rdfs:subPropertyOf)/rdfs:isDefinedBy/@ref =
                          rdfs:isDefinedBy/@ref )]">
              <xsl:sort select="rdfs:label"/>
              <xsl:sort select="@uri"/>
              <li>
                  <xsl:apply-templates mode="ref" select="."/>
                  <xsl:call-template name="hierarchy">
                    <xsl:with-param name="ns" select="'http://www.w3.org/2000/01/rdf-schema#'"/>
                    <xsl:with-param name="term" select="'subPropertyOf'"/>
                  </xsl:call-template>
              </li>
            </xsl:for-each>
          </ul>
        </div>
      </div>

      <xsl:call-template name="defs">
        <xsl:with-param name="id" select="'classes'"/>
        <xsl:with-param name="label"><xsl:value-of select="$l[@name='classes']"/></xsl:with-param>
        <xsl:with-param name="defs" select="$classes"/>
      </xsl:call-template>
      <xsl:call-template name="defs">
        <xsl:with-param name="id" select="'properties'"/>
        <xsl:with-param name="label"><xsl:value-of select="$l[@name='properties']"/></xsl:with-param>
        <xsl:with-param name="defs" select="$properties"/>
      </xsl:call-template>

      <div id="reference" class="section">
        <div class="header">
          <h2><xsl:value-of select="$l[@name='reference']"/></h2>
          <a class="tool" href="#main">[top]</a>
        </div>
        <table class="reference">
          <tr>
            <th><xsl:value-of select="$l[@name='term']"/></th>
            <th><xsl:value-of select="$l[@name='type']"/></th>
            <th><xsl:value-of select="$l[@name='definition']"/></th>
          </tr>
          <xsl:for-each select="$defs">
            <xsl:sort select="local-name(a/*)"/>
            <xsl:sort select="rdfs:label"/>
            <xsl:sort select="@uri"/>
            <tr>
              <td class="ref">
                <xsl:apply-templates mode="ref" select="."/>
              </td>
              <td class="type">
                <xsl:call-template name="type-ref"/>
              </td>
              <td><xsl:apply-templates select="rdfs:comment"/></td>
            </tr>
          </xsl:for-each>
        </table>
      </div>

      <div id="license">
        <div class="header">
          <h2><xsl:value-of select="$l[@name='license']"/></h2>
          <a class="tool" href="#main">[top]</a>
        </div>
          <!-- TODO -->TBD
      </div>

      <div id="changes">
        <div class="header">
          <h2><xsl:value-of select="$l[@name='changes']"/></h2>
          <a class="tool" href="#main">[top]</a>
        </div>
          <!-- TODO -->TBD
      </div>

    </div>
  </xsl:template>

  <xsl:template name="hierarchy">
    <xsl:param name="ns"/>
    <xsl:param name="term"/>
    <xsl:variable name="sub"
                  select="$r[*[namespace-uri()=$ns and local-name()=$term]/@ref = current()/@uri]"/>
    <xsl:if test="$sub">
      <ul>
        <xsl:for-each select="$sub">
          <xsl:sort select="@uri"/>
          <li>
            <xsl:apply-templates mode="ref" select="."/>
            <xsl:call-template name="hierarchy">
              <xsl:with-param name="ns" select="$ns"/>
              <xsl:with-param name="term" select="$term"/>
            </xsl:call-template>
          </li>
        </xsl:for-each>
      </ul>
    </xsl:if>
  </xsl:template>

  <xsl:template name="defs">
    <xsl:param name="id"/>
    <xsl:param name="label"/>
    <xsl:param name="defs"/>
    <div id="{$id}" class="section">
      <div class="header">
        <h2><xsl:value-of select="$label"/></h2>
        <a class="tool" href="#main">[top]</a>
      </div>
      <xsl:for-each select="$defs">
        <xsl:sort select="rdfs:label"/>
        <xsl:sort select="@uri"/>
        <xsl:apply-templates mode="def" select="."/>
      </xsl:for-each>
    </div>
  </xsl:template>

  <xsl:template mode="def" match="*[a/*[contains(local-name(), 'Class')]]">
    <xsl:call-template name="def">
      <xsl:with-param name="htclass">def class</xsl:with-param>
      <xsl:with-param name="prelabel"><xsl:value-of select="$l[@name='class']"/>: </xsl:with-param>
      <xsl:with-param name="content">
        <xsl:variable name="subclasses" select="$r[rdfs:subClassOf/@ref = current()/@uri]"/>
        <xsl:variable name="domainof" select="$r[rdfs:domain[@ref = current()/@uri
                                             or owl:unionOf[li/@ref = current()/@uri] ] ]"/>
        <xsl:variable name="rangeof" select="$r[rdfs:range[@ref = current()/@uri
                                             or owl:unionOf[li/@ref = current()/@uri] ] ]"/>
        <xsl:if test="$subclasses | $domainof | $rangeof">
          <dl class="usage">
            <xsl:if test="$domainof">
              <dt><xsl:value-of select="$l[@name='domain-of']"/>:</dt>
              <dd>
                <ul>
                  <xsl:for-each select="$domainof">
                    <li>
                      <xsl:apply-templates mode="ref" select="."/>
                    </li>
                  </xsl:for-each>
                </ul>
              </dd>
            </xsl:if>
            <xsl:if test="$subclasses">
              <dt><xsl:value-of select="$l[@name='has subclasses']"/>:</dt>
              <dd>
                <xsl:for-each select="$subclasses">
                  <xsl:if test="position() != 1">, </xsl:if>
                  <xsl:apply-templates mode="ref" select="."/>
                </xsl:for-each>
              </dd>
            </xsl:if>
            <xsl:if test="$rangeof">
              <dt><xsl:value-of select="$l[@name='range-of']"/>:</dt>
              <dd>
                <xsl:for-each select="$rangeof">
                  <xsl:if test="position() != 1">, </xsl:if>
                  <xsl:apply-templates mode="ref" select="."/>
                </xsl:for-each>
              </dd>
            </xsl:if>
          </dl>
        </xsl:if>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template mode="def" match="*[a/*[contains(local-name(), 'Property')]]">
    <xsl:call-template name="def">
      <xsl:with-param name="htclass">def property</xsl:with-param>
      <xsl:with-param name="prelabel"><xsl:value-of select="$l[@name='property']"/>: </xsl:with-param>
      <xsl:with-param name="content">
        <xsl:variable name="subproperties" select="$r[rdfs:subPropertyOf/@ref = current()/@uri]"/>
        <xsl:if test="$subproperties">
          <dl class="usage">
            <xsl:if test="$subproperties">
              <dt><xsl:value-of select="$l[@name='subproperties']"/>:</dt>
              <dd>
                <xsl:for-each select="$subproperties">
                  <xsl:if test="position() != 1">, </xsl:if>
                  <xsl:apply-templates mode="ref" select="."/>
                </xsl:for-each>
              </dd>
            </xsl:if>
          </dl>
        </xsl:if>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="def">
    <xsl:param name="htclass"/>
    <xsl:param name="prelabel"/>
    <xsl:param name="content"/>
    <xsl:variable name="term" select="gr:term(@uri)"/>
    <div class="{$htclass}" id="{$term}">
      <div class="header">
        <h3>
          <xsl:copy-of select="$prelabel"/>
          <xsl:apply-templates select="rdfs:label"/>
        </h3>
        <a class="tool" href="#main">[top]</a>
      </div>
      <p>
        <xsl:apply-templates select="rdfs:comment"/>
      </p>
      <xsl:call-template name="termdefs">
        <xsl:with-param name="pre">
          <dt>URI</dt>
          <dd>
            <a href="{@uri}">
              <code><xsl:value-of select="@uri"/></code>
            </a>
          </dd>
        </xsl:with-param>
      </xsl:call-template>
      <xsl:copy-of select="$content"/>
      <div class="footer">
        <a class="tool" href="#{$term}">[#]</a>
      </div>
    </div>
  </xsl:template>

  <xsl:template name="termdefs">
    <xsl:param name="pre"/>
    <xsl:param name="post"/>
    <dl class="termdefs">
      <xsl:copy-of select="$pre"/>
      <xsl:for-each select="*">
        <xsl:sort select="name()"/>
        <xsl:sort select="not(@ref)"/>
        <xsl:sort select="@ref"/>
        <xsl:apply-templates mode="termdef" select="."/>
      </xsl:for-each>
      <xsl:copy-of select="$post"/>
    </dl>
  </xsl:template>

  <xsl:template mode="termdef" match="a">
    <dt class="raw">rdf:type</dt>
    <dd class="raw">
      <xsl:for-each select="*">
        <xsl:if test="position() != 1">, </xsl:if>
        <xsl:call-template name="meta-ref"/>
      </xsl:for-each>
    </dd>
  </xsl:template>

  <!-- TODO: these used to be "richer forms of termdef" items. It might be nice
       to have two panels for a "def" block, one new with "richdef":s like
       these, and a "source" view with the current termdef list.

  <xsl:template mode="termdef" match="a">
    <dt>Type</dt>
    <dd>
      <xsl:for-each select="*">
        <xsl:if test="position() != 1">, </xsl:if>
        <xsl:call-template name="meta-ref"/>
      </xsl:for-each>
    </dd>
  </xsl:template>

  <xsl:template mode="termdef" match="dct:created">
    <dt>Created</dt>
    <dd><xsl:apply-templates select="."/></dd>
  </xsl:template>

  <xsl:template mode="termdef" match="dct:issued">
    <dt>Issued</dt>
    <dd><xsl:apply-templates select="."/></dd>
  </xsl:template>

  <xsl:template mode="termdef" match="owl:versionInfo">
    <dt>Version</dt>
    <dd><xsl:apply-templates select="."/></dd>
  </xsl:template>

  <xsl:template mode="termdef" match="rdfs:domain">
    <dt>Domain (everything with this property is a)</dt>
    <dd><xsl:apply-templates select="."/></dd>
  </xsl:template>

  <xsl:template mode="termdef" match="rdfs:range">
    <dt>Range (every value of this property is a)</dt>
    <dd><xsl:apply-templates select="."/></dd>
  </xsl:template>

  -->

  <xsl:template mode="termdef" match="*">
    <dt class="raw">
      <xsl:choose>
        <xsl:when test="self::li">
          <xsl:value-of select="position()"/>
          <xsl:text>.</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="meta-ref"/>
        </xsl:otherwise>
      </xsl:choose>
    </dt>
    <dd class="raw">
      <xsl:choose>
        <xsl:when test="* and not(@fmt)">
          <dl>
            <xsl:apply-templates mode="termdef" select="*"/>
          </dl>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="."/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:if test="self::li and position() != last()">
        <xsl:text>, </xsl:text>
      </xsl:if>
    </dd>
  </xsl:template>


  <xsl:template mode="label" match="*[a/owl:Ontology]">
      <xsl:apply-templates select="dct:title | dcelem:title | rdfs:label"/>
  </xsl:template>

  <xsl:template mode="ref" match="*">
    <a href="#{gr:term(@uri)}">
      <xsl:apply-templates select="rdfs:label"/>
    </a>
  </xsl:template>

  <xsl:template match="*[@ref]" priority="-1">
    <xsl:variable name="it" select="gr:get(.)"/>
    <xsl:choose>
      <!-- TODO: looser than "in chosen vocab"?  -->
      <xsl:when test="$it/rdfs:isDefinedBy/@ref = $vocab/@uri">
        <xsl:apply-templates mode="ref" select="$it"/>
      </xsl:when>
      <xsl:otherwise>
        <code><xsl:value-of select="@ref"/></code>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="*[@xml:lang]">
    <xsl:if test="$lang and @xml:lang = $lang or @xml:lang = ''">
      <xsl:value-of select="."/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="xsd:dateTime">
    <xsl:value-of select="."/>
  </xsl:template>

  <xsl:template name="type-ref">
    <xsl:for-each select="a/*">
      <xsl:if test="position() != 1">, </xsl:if>
      <xsl:call-template name="meta-ref"/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="meta-ref">
    <!-- <a href="{gr:name-to-uri(.)}"> ... </a> -->
    <code><xsl:value-of select="name(.)"/></code>
  </xsl:template>


</xsl:stylesheet>
