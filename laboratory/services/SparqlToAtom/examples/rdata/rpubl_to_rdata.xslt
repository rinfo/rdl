<?xml version="1.0" encoding='utf-8'?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:st="http://oort.to/ns/2008/09/sparqltree"
                xmlns:rdata="http://purl.org/net/court/ns/2008/09/rdata"
                xmlns:date="http://exslt.org/dates-and-times"
                xmlns:str="http://exslt.org/strings"
                xmlns="http://www.w3.org/2005/Atom"
                xmlns:a="http://www.w3.org/2005/Atom"
                extension-element-prefixes="date str"
                exclude-result-prefixes="st">

  <xsl:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no"/>

  <xsl:param name="sourceBase">http://rinfo.lagrummet.se/</xsl:param>
  <xsl:param name="rdataBase">/view/rdata/</xsl:param>

  <xsl:template match="/st:tree">
    <xsl:processing-instruction
      name="xml-stylesheet">href="/css/rdata.css" type="text/css"</xsl:processing-instruction>
    <xsl:choose>
        <xsl:when test="subject[2]">
            <feed>
                <id></id>
                <xsl:apply-templates select="subject"/>
            </feed>
        </xsl:when>
        <xsl:when test="subject">
            <xsl:apply-templates select="subject"/>
        </xsl:when>
        <xsl:otherwise>
        </xsl:otherwise>
    </xsl:choose>
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
        <summary></summary>

        <xsl:variable name="entryUrl" select="concat(@uri, '/entry')"/>
        <link rel="via" type="application/atom+xml;type=entry">
            <xsl:attribute name="href"><xsl:value-of select="$entryUrl"/></xsl:attribute>
        </link>
        <!-- TODO: remove '/entry'; see NOTE on entryLink below. -->
        <link rel="self" type="application/atom+xml;type=entry">
        <xsl:attribute name="href">
            <xsl:value-of select="concat($rdataBase,
                        substring-after($entryUrl, $sourceBase))"/>
        </xsl:attribute>
        </link>

        <!-- FIXME: as params; esp. localBase (if at all)! -->
        <xsl:variable name="realBase">http://rinfo.lagrummet.se</xsl:variable>
        <xsl:variable name="localBase">http://localhost:8180</xsl:variable>
        <xsl:variable name="realDepotEntry" select="$entryUrl"/>
        <xsl:variable name="depotEntry" select="concat(
                    $localBase,
                    substring-after($realDepotEntry, $realBase)
                    )"/>
        <xsl:for-each select="
            document($depotEntry)/a:entry/a:*[
                self::a:content or
                self::a:link[@rel='alternate'] or
                self::a:link[@rel='enclosure']
            ]
        ">
            <!-- TODO: use?
                self::a:source or
            -->
            <xsl:copy>
                <xsl:for-each select="@*">
                    <xsl:choose>
                    <xsl:when test="name(.) = 'src' or name(.) = 'href' and starts-with(., '/')">
                        <xsl:attribute name="{name()}">
                        <xsl:value-of select="concat($localBase, .)"/>
                        </xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy-of select="."/>
                    </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
                <xsl:copy-of select="*"/>
            </xsl:copy>
        </xsl:for-each>

        <xsl:variable name="inf-cat">http://rdata.lagrummet.se/categories/inferred/</xsl:variable>

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

        <!-- TODO: The categorizing done for dates may be extremely worthwhile as a
                generic tagging mechanism:

                - prepend with term *from predicate*
                - term from either uri or year-in-date

            Examine how to manage category documents for such aggregates.
            Perhaps a fully controlled category-from-property map (in the repo)?

        -->
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
            <xsl:call-template name="uri-term"/>
            <xsl:text>-</xsl:text>
            <xsl:value-of select="date:year(dateValue)"/>
            </xsl:attribute>
        </category>
        </xsl:for-each>

        <!-- TODO:
            - get only links
            - entry values are better to get from pure entry storage layer
            - rel-labels also better to store separately (from entries with vocabs..)
        NOTE:
            - we append no '/entry' segment, since rdata is only Atom!
        -->
        <xsl:for-each select="rel">
        <rdata:entryLink type="application/atom+xml;type=entry">
            <xsl:attribute name="rel"><xsl:value-of select="@uri"/></xsl:attribute>
            <xsl:attribute name="href">
            <xsl:value-of select="concat($rdataBase,
                            substring-after(relSubject/@uri, $sourceBase))"/>
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
                            substring-after(revSubject/@uri, $sourceBase))"/>
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
