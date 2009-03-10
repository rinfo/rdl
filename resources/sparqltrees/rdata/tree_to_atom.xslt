<?xml version="1.0" encoding='utf-8'?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:st="http://oort.to/ns/2008/09/sparqltree"
                xmlns:rdata="http://oort.to/ns/2008/09/rdata"
                xmlns:date="http://exslt.org/dates-and-times"
                xmlns:str="http://exslt.org/strings"
                xmlns="http://www.w3.org/2005/Atom"
                extension-element-prefixes="date str"
                exclude-result-prefixes="st">

  <xsl:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no"/>

  <xsl:param name="publBase">http://rinfo.lagrummet.se/publ/</xsl:param>
  <xsl:param name="rdataBase">http://rdata.lagrummet.se/feeds/</xsl:param>

  <xsl:template match="/st:tree">
    <feed>
        <xsl:apply-templates/>
    </feed>
  </xsl:template>

  <xsl:template match="subject">
    <entry>
      <id><xsl:value-of select="@uri"/></id>
      <title>
        <xsl:choose>
          <xsl:when test="serieNummer">
            <xsl:value-of select="serieLabel"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="serieNummer"/>
            <xsl:if test="konsDatum">
              <xsl:text> (konsolidering </xsl:text>
              <xsl:value-of select="konsDatum"/>
              <xsl:text>)</xsl:text>
            </xsl:if>
          </xsl:when>
          <xsl:when test="rattsdokNr">
            <xsl:value-of select="type/typeLabel"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="rattsdokNr"/>
          </xsl:when>
          <xsl:otherwise>
            <!-- TODO: missing knowledge.. -->
            <xsl:comment>N/A</xsl:comment>
          </xsl:otherwise>
        </xsl:choose>
      </title>
      <subtitle><xsl:value-of select="dcTitle"/></subtitle>
      <!-- TODO:
      merge from-url="subject"
                    skip="id; title; summary; link[@rel='self']"/>
      -->

      <xsl:variable name="entryUrl" select="concat(@uri, '/entry')"/>
      <link rel="via" type="application/atom+xml;type=entry">
        <xsl:attribute name="href"><xsl:value-of select="$entryUrl"/></xsl:attribute>
      </link>

      <link rel="self" type="application/atom+xml;type=entry">
        <xsl:attribute name="href">
          <xsl:value-of select="concat($rdataBase,
                        substring-after($entryUrl, $publBase))"/>
        </xsl:attribute>
      </link>

      <xsl:for-each select="publisher[1]">
        <!-- TODO: category or (slighly ambiguous) author? -->
        <author>
          <uri><xsl:value-of select="@uri"/></uri>
          <xsl:if test="publisherLabel">
            <name><xsl:value-of select="publisherLabel"/></name>
            <!-- <email/> -->
          </xsl:if>
        </author>
        <xsl:if test="publisherLabel">
          <category scheme="{$inf-cat}publishers/">
            <xsl:attribute name="term">
              <xsl:value-of select="publisherLabel"/>
            </xsl:attribute>
          </category>
        </xsl:if>
      </xsl:for-each>

      <xsl:for-each select="type">
        <category>
          <xsl:attribute name="scheme">
            <xsl:call-template name="uri-space">
            </xsl:call-template>
          </xsl:attribute>
          <xsl:attribute name="term">
            <xsl:call-template name="uri-term"/>
          </xsl:attribute>
          <xsl:attribute name="label"><xsl:value-of select="typeLabel"/></xsl:attribute>
        </category>
      </xsl:for-each>

      <xsl:variable name="inf-cat">http://rdata.lagrummet.se/categories/inferred/</xsl:variable>

      <xsl:if test="serieLabel">
        <category scheme="{$inf-cat}collection/">
          <xsl:attribute name="term">
            <xsl:value-of select="serieLabel"/>
          </xsl:attribute>
        </category>
      </xsl:if>

      <xsl:for-each select="dateRel">
        <category scheme="{@uri}">
          <xsl:attribute name="term">
            <xsl:value-of select="date:year(dateValue)"/>
          </xsl:attribute>
        </category>
      </xsl:for-each>

      <!-- TODO:
          - get only links
          - entry values are better to get from pure entry storage layer
          - rel-labels also better to store separately (from entries with vocabs..)
      -->
      <xsl:for-each select="rel">
        <rdata:entryLink type="application/atom+xml;type=entry">
          <xsl:attribute name="rel"><xsl:value-of select="@uri"/></xsl:attribute>
          <xsl:attribute name="href">
            <xsl:value-of select="concat($rdataBase,
                          substring-after(relSubject/@uri, $publBase), '/entry')"/>
          </xsl:attribute>
          <xsl:attribute name="relLabel"><xsl:value-of select="relLabel"/></xsl:attribute>
          <xsl:if test="relInverseLabel">
            <xsl:attribute name="relInverseLabel"><xsl:value-of select="relInverseLabel"/></xsl:attribute>
          </xsl:if>
          <id><xsl:value-of select="relSubject/@uri"/></id>
          <xsl:if test="relSubject/relText">
            <title><xsl:value-of select="relSubject/relText"/></title>
          </xsl:if>
        </rdata:entryLink>
      </xsl:for-each>

      <!-- TODO: these are definitely better looked up on demand from entry storage! -->
      <xsl:for-each select="rev">
        <rdata:entryLink
          type="application/atom+xml;type=entry">
          <xsl:attribute name="rev"><xsl:value-of select="@uri"/></xsl:attribute>
          <xsl:attribute name="href">
            <xsl:value-of select="concat($rdataBase,
                          substring-after(revSubject/@uri, $publBase), '/entry')"/>
          </xsl:attribute>
          <xsl:attribute name="revLabel"><xsl:value-of select="revLabel"/></xsl:attribute>
          <id><xsl:value-of select="revSubject/@uri"/></id>
        </rdata:entryLink>
      </xsl:for-each>

    </entry>
  </xsl:template>

  <xsl:template name="uri-space">
    <xsl:param name="uri" select="@uri"/>
    <xsl:choose>
      <xsl:when test="contains($uri, '#')">
        <xsl:value-of select="concat(substring-before($uri, '#'), '#')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="str:split($uri, '/')[position() != last()]">
          <xsl:value-of select="current()"/>
          <xsl:text>/</xsl:text>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="uri-term">
    <xsl:param name="uri" select="@uri"/>
    <xsl:choose>
      <xsl:when test="contains($uri, '#')">
        <xsl:value-of select="substring-after($uri, '#')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="str:split($uri, '/')[position() = last()]">
          <xsl:value-of select="current()"/>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
