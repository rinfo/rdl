<?xml version="1.0" encoding='utf-8'?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exslt="http://exslt.org/common"
                xmlns:func="http://exslt.org/functions"
                xmlns:str="http://exslt.org/strings"
                xmlns:set="http://exslt.org/sets"
                xmlns:date="http://exslt.org/dates-and-times"
                xmlns:a="http://www.w3.org/2005/Atom"
                xmlns:os="http://a9.com/-/spec/opensearchrss/1.0/"
                xmlns:rdata="http://oort.to/ns/2008/09/rdata"
                xmlns:lf="tag:localhost,2008:exslt-local-functions"
                extension-element-prefixes="exslt func str set date"
                exclude-result-prefixes="lf">

  <xsl:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no"/>

  <xsl:param name="entry-id"/>
  <xsl:param name="q"/>
  <xsl:param name="category"/>
  <xsl:param name="author"/>

  <xsl:param name="min-updated"/>
  <xsl:param name="max-updated"/>
  <xsl:param name="min-published"/>
  <xsl:param name="max-published"/>

  <xsl:param name="start-index" select="1"/>
  <xsl:param name="max-results"/>

  <xsl:variable name="category-groups-rt">
    <xsl:for-each select="str:split($category, '/')">
      <group>
        <xsl:copy-of select="str:split(., '|')"/>
      </group>
    </xsl:for-each>
  </xsl:variable>
  <xsl:variable name="category-groups" select="exslt:node-set($category-groups-rt)"/>

  <xsl:key name="entry-by-id" match="/a:feed/a:entry" use="a:id"/>

  <xsl:template match="/a:feed">
    <xsl:variable name="selected-entries" select="a:entry[
        (not($entry-id) or $entry-id = a:id)
        and
        (not($q) or contains(a:title, $q) or contains(a:subtitle, $q))
        and
        (not($category-groups) or lf:test-categories(., $category-groups))
        and
        (not($author) or contains(a:author/a:*, $author))
      ]"/>
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <os:startIndex><xsl:value-of select="$start-index"/></os:startIndex>
      <os:totalResults><xsl:value-of select="count($selected-entries)"/></os:totalResults>
      <xsl:if test="$max-results">
        <os:itemsPerPage><xsl:value-of select="$max-results"/></os:itemsPerPage>
      </xsl:if>
      <xsl:copy-of select="*[not(self::a:entry)]"/>
      <xsl:apply-templates select="$selected-entries"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="a:entry">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates/>
      <!-- TODO: fetch from depot entry when storing (not querying) -->
      <xsl:variable name="realBase">http://rinfo.lagrummet.se</xsl:variable>
      <xsl:variable name="realDepotEntry" select="a:link[@rel='via']/@href"/>
      <xsl:variable name="depotEntry" select="concat(
                    'http://localhost:8180',
                    substring-after($realDepotEntry, $realBase)
                  )"/>
      <xsl:for-each select="
          document($depotEntry)/a:entry/a:*[
            self::a:content or
            self::a:link[@rel='alternate'] or
            self::a:link[@rel='enclosure']
          ]
        ">
        <xsl:copy>
          <xsl:for-each select="@*">
              <xsl:choose>
                <xsl:when test="name(.) = 'src' or name(.) = 'href' and starts-with(., '/')">
                  <xsl:attribute name="{name()}">
                    <xsl:value-of select="concat($realBase, .)"/>
                  </xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:copy-of select="."/>
                </xsl:otherwise>
              </xsl:choose>
          </xsl:for-each>
        </xsl:copy>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>

  <!-- TODO: expand plain links? And infer rev links from store.. -->
  <xsl:template match="rdata:entryLink">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:copy-of select="a:id"/>
      <xsl:copy-of select="key('entry-by-id', a:id)/a:title"/>
      <xsl:copy-of select="key('entry-by-id', a:id)/a:subtitle"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="*|@*">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

  <func:function name="lf:test-categories">
    <xsl:param name="entry"/>
    <xsl:param name="category-groups"/>
    <xsl:variable name="term-attrs" select="$entry/a:category/@term"/>
    <xsl:variable name="suite-rt">
      <xsl:for-each select="$category-groups/group">
        <xsl:if test="not(lf:attrs-contains-any($term-attrs, token))">
          <false/>
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>
    <func:result select="not(exslt:node-set($suite-rt)/false)"/>
  </func:function>

  <func:function name="lf:attrs-contains-any">
    <xsl:param name="attrs"/>
    <xsl:param name="values"/>
    <xsl:variable name="attr-values-rt">
      <xsl:for-each select="$attrs">
        <token><xsl:value-of select="."/></token>
      </xsl:for-each>
    </xsl:variable>
    <xsl:variable name="attr-values" select="exslt:node-set($attr-values-rt)/*"/>
    <!-- TODO: this works; but how?! Those aren't sorted.. As if "=" means "some in other".. -->
    <func:result select="($values = $attr-values)"/>
  </func:function>

</xsl:stylesheet>
