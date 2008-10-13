<?xml version="1.0" encoding='utf-8'?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:st="http://oort.to/ns/2008/09/sparqltree">

    <xsl:output method="text" indent="no" encoding="utf-8" omit-xml-declaration="yes"/>

    <xsl:template match="/st:sparqltree">
        <xsl:for-each select="ancestor-or-self::*/namespace::*">
            <xsl:text>PREFIX </xsl:text>
            <xsl:value-of select="name()"/>
            <xsl:text>: &lt;</xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>>&#10;</xsl:text>
        </xsl:for-each>
        <xsl:text>&#10;</xsl:text>
        <xsl:text>SELECT</xsl:text>
        <xsl:if test="@distinct = 'true'">
            <xsl:text> DISTINCT </xsl:text>
        </xsl:if>
        <xsl:for-each select=".//*">
            <xsl:if test="position() != 1">
                <xsl:text> </xsl:text>
            </xsl:if>
                <xsl:value-of select="concat('?', name())"/>
        </xsl:for-each>
        <xsl:text> WHERE {&#10;</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>}&#10;</xsl:text>
        <xsl:if test="@order-by">
            <xsl:text>ORDER BY </xsl:text>
            <xsl:value-of select="@order-by"/>
            <xsl:text>&#10;</xsl:text>
        </xsl:if>
        <xsl:if test="@limit">
            <xsl:text>LIMIT </xsl:text>
            <xsl:value-of select="@limit"/>
            <xsl:if test="@offset">
                <xsl:text> OFFSET </xsl:text>
                <xsl:value-of select="@offset"/>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <xsl:template match="*">
        <!-- TODO: only if mixed (allows for separate tree and sparql)
        <xsl:if test="not(*) or normalize-space(text()) != ''">
        </xsl:if>
        -->
        <xsl:if test="not(@st:as-var = 'no')">
            <xsl:value-of select="concat('?', name())"/>
        </xsl:if>
        <xsl:apply-templates/>
        <xsl:if test="@uri">
            <xsl:text>FILTER( ?</xsl:text><xsl:value-of select="name()"/>
            <xsl:text> = &lt;</xsl:text>
            <xsl:value-of select="@uri"/>
            <xsl:text>>)</xsl:text>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
