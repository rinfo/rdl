<?xml version="1.0" encoding='utf-8'?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:alias="file:xslt-alias"
                xmlns:st="http://oort.to/ns/2008/09/sparqltree">

  <xsl:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no"/>
  <xsl:namespace-alias stylesheet-prefix="alias" result-prefix="xsl"/>

  <xsl:template match="/st:sparqltree">
    <alias:stylesheet version="1.0"
                      xmlns:s="http://www.w3.org/2005/sparql-results#"
                      xmlns:exslt="http://exslt.org/common"
                      xmlns:date="http://exslt.org/dates-and-times"
                      extension-element-prefixes="date">
      <alias:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no"/>
      <alias:template match="/s:sparql">
        <alias:apply-templates select="s:results"/>
      </alias:template>

      <alias:template match="s:results">
        <alias:variable name="resultgroup-{generate-id(.)}" select="s:result"/>
        <st:tree>
            <xsl:apply-templates>
            <xsl:with-param name="st-current" select="s:results/s:result"/>
            </xsl:apply-templates>
        </st:tree>
      </alias:template>

    <alias:template match="s:binding">
        <alias:choose>
            <alias:when test="s:uri">
                <alias:attribute name="uri">
                    <alias:value-of select="s:uri"/>
                </alias:attribute>
            </alias:when>
            <alias:when test="s:bnode">
                <alias:attribute name="bnode">
                    <alias:value-of select="s:bnode"/>
                </alias:attribute>
            </alias:when>
            <alias:otherwise>
                <alias:copy-of select="*/@*"/>
                <alias:value-of select="*"/>
            </alias:otherwise>
        </alias:choose>
    </alias:template>

    </alias:stylesheet>
  </xsl:template>

  <xsl:template match="*">
    <xsl:variable name="binding" select="name()"/>
    <xsl:variable name="bindinggroup" select="generate-id(parent::*)"/>
    <!-- TODO: preceding-sibling is *not* scoped to select context -->
    <alias:for-each select="$resultgroup-{$bindinggroup}[
        s:binding[@name='{$binding}']/*/text() and
        not(
          s:binding[@name='{$binding}']/*/text() =
            preceding-sibling::s:result/s:binding[@name='{$binding}']/*/text()
        )
      ]">
      <!-- TODO: warning - while solving preceding-sibling above, this
           may cause *huge* memory consumption!-->
      <alias:variable name="rt-resultgroup-{generate-id(.)}">
        <alias:for-each select="$resultgroup-{$bindinggroup}[
                        s:binding[@name='{$binding}']/*/text() =
                          current()/s:binding[@name='{$binding}']/*/text()]">
          <alias:copy-of select="."/>
        </alias:for-each>
      </alias:variable>
      <alias:variable name="resultgroup-{generate-id(.)}"
                      select="exslt:node-set($rt-resultgroup-{generate-id(.)})/*"/>
      <xsl:copy>
        <!-- TODO: filter attrs on real namespace()! -->
        <xsl:copy-of select="@*[not(starts-with(name(), 'st:'))]"/>
        <alias:apply-templates select="s:binding[@name='{$binding}']"/>
        <xsl:apply-templates/>
      </xsl:copy>
    </alias:for-each>
  </xsl:template>

  <xsl:template match="text()"></xsl:template>

</xsl:stylesheet>
