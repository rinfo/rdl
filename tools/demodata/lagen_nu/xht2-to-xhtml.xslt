<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
		xmlns="http://www.w3.org/1999/xhtml"
		xmlns:xht2="http://www.w3.org/2002/06/xhtml2/"
		xmlns:dct="http://purl.org/dc/terms/"
		xmlns:rinfo="http://rinfo.lagrummet.se/taxo/2007/09/rinfo/pub#"
		xmlns:rpubl="http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#"
		xmlns:rinfoex="http://lagen.nu/terms#"
		xmlns:exslt="http://exslt.org/common"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="xml" encoding="utf-8"/>

  <xsl:template name="replace-string">
    <xsl:param name="text"/>
    <xsl:param name="replace"/>
    <xsl:param name="with"/>
    <xsl:choose>
      <xsl:when test="contains($text,$replace)">
        <xsl:value-of select="substring-before($text,$replace)"/>
        <xsl:value-of select="$with"/>
        <xsl:call-template name="replace-string">
          <xsl:with-param name="text"
select="substring-after($text,$replace)"/>
          <xsl:with-param name="replace" select="$replace"/>
          <xsl:with-param name="with" select="$with"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="xht2:html">
    <xsl:element name="{name()}">
      <xsl:attribute name="version">XHTML+RDFa 1.0</xsl:attribute>
      <xsl:variable name="dct-prefix">
	<dct:elem xmlns:dct="http://purl.org/dc/terms/"/>
      </xsl:variable>
      <xsl:variable name="rinfo-prefix">
	<rinfo:elem rinfo:dct="http://rinfo.lagrummet.se/taxo/2007/09/rinfo/pub#"/>
      </xsl:variable>
      <xsl:variable name="rpubl-prefix">
	<rpubl:elem rpubl:dct="http://rinfo.lagrummet.se/taxo/2007/09/rinfo/pub#"/>
      </xsl:variable>
      <!--
      <xsl:variable name="rinfoex-prefix">
	<rinfoex:elem xmlns:rinfoex="http://lagen.nu/terms#"/>
      </xsl:variable>
      -->
      <xsl:copy-of select="exslt:node-set($dct-prefix)/*/namespace::*"/>
      <xsl:copy-of select="exslt:node-set($rpubl-prefix)/*/namespace::*"/>
      <!--<xsl:copy-of select="exslt:node-set($rinfo-prefix)/*/namespace::*"/>-->
      <!--<xsl:copy-of select="exslt:node-set($rinfoex-prefix)/*/namespace::*"/>-->
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="xht2:section">
    <xsl:element name="div">
      <xsl:apply-templates select="@*|node()"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="xht2:h[@class='underrubrik']">
    <xsl:element name="h2">
      <xsl:apply-templates select="@*|node()"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="xht2:h">
    <xsl:element name="h1">
      <xsl:apply-templates select="@*|node()"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="xht2:section[@role='secondary']">
  <!-- remove (same data exists in RDF)-->
  </xsl:template>
  <xsl:template match="xht2:dl[@role='contentinfo']">
  <!-- remove (same data exists in RDF) -->
  </xsl:template>
  <xsl:template match="xht2:head/xht2:meta">
  <!-- remove (same data exists in RDF) -->
  </xsl:template>
  <xsl:template match="xht2:head/xht2:link">
  <!-- remove (same data exists in RDF) -->
  </xsl:template>

  <xsl:template match="xht2:a[@rel='dct:references']">
      <!-- remove link itself (not in original source, high risk of being wrong) -->
      <xsl:apply-templates/>
  </xsl:template>

  <!-- transform attributes from rinfo:* to rpubl:* -->
  <xsl:template match="@typeof">
    <xsl:attribute name="typeof">
      <xsl:value-of select="'rpubl:'"/>
      <xsl:value-of select="substring-after(.,'rinfo:')"/>
    </xsl:attribute>
  </xsl:template>

    
  <xsl:template match="@role">
    <xsl:attribute name="class"><xsl:value-of select="."/></xsl:attribute>
  </xsl:template>
	    
  <xsl:template match="*">
    <xsl:element name="{name()}">
      <xsl:apply-templates select="@*|node()"/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="@*">
    <xsl:copy><xsl:apply-templates/></xsl:copy>
  </xsl:template>

</xsl:stylesheet>
