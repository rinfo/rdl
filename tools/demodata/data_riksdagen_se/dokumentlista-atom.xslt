<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/2005/Atom"
                xmlns:os="http://a9.com/-/spec/opensearchrss/1.0/"
                extension-element-prefixes="func"
                xmlns:func="http://exslt.org/functions"
                exclude-result-prefixes="func self"
                xmlns:self="file:.">

  <xsl:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no"/>

  <xsl:include href="common.xslt"/>

  <xsl:template match="/dokumentlista">
    <feed>
      <id>tag:data.riksdagen.se,2010:rinfo:feed</id>
      <updated><xsl:value-of select="self:w3c-dt(@datum)"/></updated>
      <link rel="self" href="{@xml:base}&amp;format=atom" type="application/atom+xml"/>
      <xsl:if test="@nasta_sida">
        <link rel="next" href="{@nasta_sida}&amp;format=atom" type="application/atom+xml"/>
      </xsl:if>
      <os:startIndex><xsl:value-of select="@traff_fran"/></os:startIndex>
      <os:totalResults><xsl:value-of select="@traffar"/></os:totalResults>
      <os:itemsPerPage><xsl:value-of select="number(@traff_till) - number(@traff_fran) + 1"/></os:itemsPerPage>
      <xsl:apply-templates select="dokument"/>
    </feed>
  </xsl:template>

  <xsl:template match="dokument">
    <entry>
      <!--
      <id>http://data.riksdagen.se/dokument/<xsl:value-of select="id"/></id>
      -->
      <id><xsl:value-of select="self:rpubl-uri(.)"/></id>
      <updated><xsl:value-of select="self:w3c-dt(systemdatum)"/></updated>
      <published><xsl:value-of select="self:w3c-dt(publicerad)"/></published>
      <title><xsl:value-of select="titel"/></title>
      <summary><xsl:value-of select="undertitel"/></summary>
      <content type="application/rdf+xml" src="{dokumentstatus_url_xml}/rdf"/>
      <xsl:apply-templates select="*"/>
      <!-- TODO: "attachment" anges bara i dokumentstatus-xml:en -->
    </entry>
  </xsl:template>

  <xsl:template match="dokument_url_html">
    <link rel="alternate" type="text/html" href="{.}"/>
  </xsl:template>

  <xsl:template match="dokument_url_text">
    <link rel="alternate" type="text/plain" href="{.}"/>
  </xsl:template>

  <xsl:template match="*"/>

</xsl:stylesheet>
