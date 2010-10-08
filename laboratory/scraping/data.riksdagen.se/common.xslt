<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/2005/Atom"
                xmlns:os="http://a9.com/-/spec/opensearchrss/1.0/"
                extension-element-prefixes="func"
                xmlns:func="http://exslt.org/functions"
                exclude-result-prefixes="func self"
                xmlns:self="file:.">

  <func:function name="self:rpubl-uri">
    <xsl:param name="node"/>
    <func:result>
      <xsl:text>http://rinfo.lagrummet.se/publ/</xsl:text>
      <xsl:value-of select="$node/typ"/>
      <xsl:text>/</xsl:text>
      <xsl:value-of select="$node/rm"/>
      <xsl:text>:</xsl:text>
      <xsl:value-of select="$node/beteckning"/>
    </func:result>
  </func:function>

  <func:function name="self:w3c-dt">
    <xsl:param name="node"/>
    <func:result select="concat(translate($node, ' ', 'T'), '-01:00')"/>
  </func:function>

</xsl:stylesheet>
