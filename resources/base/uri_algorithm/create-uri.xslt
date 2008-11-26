<?xml version="1.0" encoding='utf-8'?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:s="http://www.w3.org/2005/sparql-results#">

    <xsl:output method="xml" indent="no" encoding="utf-8" omit-xml-declaration="no"/>
    <xsl:param name="uriBase">http://rinfo.lagrummet.se</xsl:param>

    <xsl:template match="/s:sparql">
        <xsl:variable name="result">
            <xsl:apply-templates select="s:results"/>
        </xsl:variable>
        <!-- TODO: Keep? $debug flag? Or use explicit error element?
        <xsl:variable name="resultCount" select="count(s:results/s:result)"/>
        <xsl:if test="$resultCount > 1">
            <xsl:message>
                <xsl:text>[ERROR: too many result elements. Expected 1,  got: </xsl:text>
                <xsl:value-of select="$resultCount"/>
                <xsl:text>]</xsl:text>
            </xsl:message>
        </xsl:if>
        -->
        <xsl:copy-of select="$result"/>
    </xsl:template>


    <xsl:template match="s:results">
        <entry>
            <id>
                <!-- TODO: can we trust the *last* result to be the expected at
                     all times? Feels to brittle (might be necessary to filter
                     in the query). -->
                <xsl:apply-templates select="s:result[last()]" mode="make-uri"/>
            </id>
            <!-- TODO: Keep? $debug flag?
            <source>
                <xsl:value-of select="s:binding[@name='about']/*"/>
            </source>
            -->
        </entry>
    </xsl:template>

    <xsl:template match="s:result[s:binding[@name='rattsdokumentnummer']]"
                  mode="make-uri">
        <xsl:value-of select="$uriBase"/>
        <xsl:value-of select="s:binding[@name='containerId']/*"/>
        <xsl:value-of select="s:binding[@name='rattsdokumentnummer']/*"/>
    </xsl:template>

    <xsl:template match="s:result[s:binding[@name='konsoliderar']]"
                  mode="make-uri">
        <!-- TODO: piece together more controllably.. -->
        <xsl:value-of select="s:binding[@name='konsoliderar']/*"/>
        <xsl:text>/konsolidering/</xsl:text>
        <xsl:value-of select="s:binding[@name='konsDatum']/*"/>
    </xsl:template>



</xsl:stylesheet>
