<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml">

  <xsl:import href="layout.xslt"/>

  <xsl:template match="/h:html">
    <xsl:call-template name="master">
      <xsl:with-param name="title-lead"/>
    </xsl:call-template>
  </xsl:template>

</xsl:stylesheet>
