<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:a="http://www.w3.org/2005/Atom">

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
          <p><xsl:value-of select="a:summary"/></p>
        </dt>
        <dd>
          <code><xsl:value-of select="a:id"/></code>
          <!--
          <pre><code><xsl:text disable-output-escaping="yes">&lt;![CDATA[</xsl:text>
                <xsl:copy-of select="a:content[@type='application/rdf+xml']/node()"/>
          <xsl:text disable-output-escaping="yes">]]&gt;</xsl:text></code></pre>
          -->
        </dd>
      </xsl:for-each>
    </dl>
  </xsl:template>

</xsl:stylesheet>
