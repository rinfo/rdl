<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:a="http://www.w3.org/2005/Atom"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

  <xsl:import href="layout.xslt"/>

  <xsl:template match="/h:html">
    <xsl:call-template name="master">
      <xsl:with-param name="title-lead">System</xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="h:div[@class='atom-uri-examples']/
                h:a[@class='atom-source']">
    <xsl:variable name="feed" select="document(@href)/a:feed"/>
    <dl>
      <xsl:for-each select="$feed/a:entry">
        <dt>
          <code><xsl:value-of select="a:id"/></code>
        </dt>
        <dd>
          <xsl:if test="a:summary/node()">
            <p> <em><xsl:value-of select="a:summary"/></em> </p>
          </xsl:if>
          <dl>
            <xsl:apply-templates select="a:content[@type='application/rdf+xml']/*" mode="rdf-example"/>
          </dl>
          <!--
          <pre><code><xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                <xsl:copy-of select="a:content[@type='application/rdf+xml']/node()"/>
          <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text></code></pre>
          -->
        </dd>
      </xsl:for-each>
    </dl>
  </xsl:template>

  <xsl:template match="*" mode="rdf-example">
    <dt>
      <xsl:value-of select="name(.)"/>
    </dt>
    <dd>
      <xsl:choose>
        <xsl:when test="*">
          <dl>
            <xsl:apply-templates mode="rdf-example"/>
          </dl>
        </xsl:when>
        <xsl:when test="@rdf:resource">
          <xsl:text>&lt;</xsl:text>
          <xsl:value-of select="@rdf:resource"/>
          <xsl:text>&gt;</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="."/>
        </xsl:otherwise>
      </xsl:choose>
    </dd>
  </xsl:template>

</xsl:stylesheet>
