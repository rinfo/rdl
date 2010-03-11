<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:xi="http://www.w3.org/2001/XInclude"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="xi">

  <xsl:param name="docdate"/>
  <xsl:param name="svnversion"/>
  <xsl:param name="root" select="'../'"/>
  <xsl:param name="show-formats" select="false()"/>

  <xsl:template name="master">
    <xsl:param name="title-lead"></xsl:param>
    <html xml:lang="sv">
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <title>
          <xsl:if test="$title-lead">
            <xsl:value-of select="$title-lead"/>
            <xsl:text>: </xsl:text>
          </xsl:if>
          <xsl:value-of select="h:head/h:title"/>
        </title>
        <link rel="stylesheet" type="text/css" media="screen,print" href="{$root}css/dv.css"/>
        <link rel="stylesheet" type="text/css" media="screen,print" href="{$root}css/syntax.css"/>
      </head>
      <body>
        <div id="header">
            <img src="{$root}img/logotyp.png" class="logo" alt=""/>
        </div>
        <div id="pagemeta">
          <p><span id="pagenumber"/></p>
          <table>
            <tr>
              <th>DATUM</th>
              <th>VERSION</th>
            </tr>
            <tr>
              <td id="docdate">
                <xsl:value-of select="$docdate"/>
              </td>
              <td id="svnversion">
                <xsl:value-of select="$svnversion"/>
              </td>
            </tr>
          </table>
        </div>

        <div id="body">
          <xsl:apply-templates select="h:body/node()"/>
        </div>

      </body>
    </html>
  </xsl:template>

  <xsl:template match="h:div[@id='toc']/h:ul[not(h:li)]">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:for-each select="/h:html/h:body/h:div[contains(@class, 'section') or contains(@class, 'appendix')]">
        <xsl:variable name="div-id">
          <xsl:choose>
            <xsl:when test="@id"><xsl:value-of select="@id"/></xsl:when>
            <xsl:otherwise>s_<xsl:value-of select="position()"/></xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <li>
          <a href="#{$div-id}"><xsl:value-of select="h:h2"/></a>
        </li>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="h:div[contains(concat(' ',@class, ' '), ' section ')]">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:if test="not(@id)">
        <xsl:attribute name="id">
          <xsl:text>s_</xsl:text>
          <xsl:value-of select="1 + count(preceding-sibling::h:div[
                        contains(concat(' ',@class, ' '), ' section ')])"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="*|@*">
    <xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy>
  </xsl:template>

</xsl:stylesheet>
