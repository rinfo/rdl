<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:gr="http://purl.org/oort/impl/xslt/grit/lib/common#"
                xmlns:func="http://exslt.org/functions"
                xmlns:str="http://exslt.org/strings"
                extension-element-prefixes="func">

  <xsl:key name="rel" match="/graph/resource" use="@uri"/>
  <xsl:variable name="r" select="/graph/resource"/>

  <func:function name="gr:get">
    <xsl:param name="e"/>
    <xsl:choose>
      <xsl:when test="$e/@ref">
        <func:result select="key('rel', $e/@ref)"/>
      </xsl:when>
      <xsl:otherwise>
        <func:result select="$e"/>
      </xsl:otherwise>
    </xsl:choose>
  </func:function>

  <func:function name="gr:name-to-uri">
    <xsl:param name="e"/>
    <func:result>
      <xsl:value-of select="namespace-uri($e)"/>
      <xsl:value-of select="local-name($e)"/>
    </func:result>
  </func:function>

  <func:function name="gr:term">
    <xsl:param name="uri"/>
    <xsl:choose>
      <xsl:when test="contains($uri, '#')">
        <func:result select="substring-after($uri, '#')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="str:split($uri, '/')[position() = last()]">
            <xsl:choose>
                <xsl:when test="contains(., ':')">
                    <xsl:for-each select="str:split($uri, ':')[position() = last()]">
                        <func:result select="current()"/>
                    </xsl:for-each>
                </xsl:when>
                <xsl:otherwise>
                    <func:result select="current()"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </func:function>

</xsl:stylesheet>
