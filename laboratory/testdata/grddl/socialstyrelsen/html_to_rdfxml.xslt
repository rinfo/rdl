<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:date="http://exslt.org/dates-and-times"
                extension-element-prefixes="date"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                xmlns:dct="http://purl.org/dc/terms/"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:rpubl="http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#">

    <!--
    Syfte: Denna transformation gör om den råa (X)HTML som Socialstyrelsen
    publicerar myndighetsföreskrifter på till RDF/XML.

    Slutsats: Det är möligt och relativt rimligt att göra denna transformation.
    Det kommer troligtvis behövas underhåll (p.g.a. hänsyn till mänger av
    potentiella avvikelser etc.).
    -->

    <xsl:include href="html-variables_inc.xslt"/>

    <xsl:variable name="ns-xsd" select="document('')/xsl:*/namespace::*[name()='xsd'][position()=1]"/>

    <xsl:param name="lang">sv</xsl:param>

    <xsl:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no"/>

    <xsl:template match="/h:html">
        <rdf:RDF xml:base="{$base}">
            <rpubl:Myndighetsforeskrift rdf:about="">
                <dct:identifier><xsl:value-of select="$fsId"/></dct:identifier>
                <xsl:apply-templates select=".//h:div[@id='socextQuickLinkArea']//*[@class='facts']"/>
                <xsl:apply-templates select=".//h:div[@id='socextContentPageArea']/node()"/>
                <xsl:apply-templates select=".//h:div[@id='socextQuickLinkArea']//*[@class='keywords']"/>
            </rpubl:Myndighetsforeskrift>
        </rdf:RDF>
    </xsl:template>

    <!-- contentinfo {{{ -->
    <xsl:template match="*[@class='facts']//h:dl/h:dd[preceding-sibling::h:dt[text()='Beslutad:']]">
        <rpubl:beslutsdatum rdf:datatype="{$ns-xsd}date">
            <!-- TODO: convert sv locale human date to W3C-date;
                 .. unfortunately this doesn't seem to work (with xsltproc):
            <date:date-format lang="${lang}"/>
            <xsl:value-of select="date:parse-date(.)"/>
            -->
            <xsl:value-of select="."/>
        </rpubl:beslutsdatum>
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="*[@class='facts']//h:dl/h:dd[preceding-sibling::h:dt[text()='Tryckdatum:']]">
        <rpubl:utkomFranTryck rdf:datatype="{$ns-xsd}date">
            <!-- TODO: convert date (see above) -->
            <xsl:value-of select="."/>
        </rpubl:utkomFranTryck>
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="*[@class='facts']//h:dl/h:dd[preceding-sibling::h:dt[text()='Ansvarig utgivare:']]">
        <dct:publisher rdf:resource="http://rinfo.lagrummet.se/org/socialstyrelsen"/>
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="h:div[@id='socextQuickLinkArea']//*[@class='keywords']//h:a[@rel='nofollow']">
        <dc:subject xml:lang="{$lang}">
            <xsl:choose>
                <xsl:when test="contains(., ',')">
                    <xsl:value-of select="substring-before(normalize-space(.), ',')"/>
                </xsl:when>
                <xsl:otherwise><xsl:value-of select="normalize-space(.)"/></xsl:otherwise>
            </xsl:choose>
        </dc:subject>
    </xsl:template>
    <!-- }}} -->

    <!-- main {{{ -->
    <xsl:template match="*[@class='sosfs']//h:h1">
        <dct:title xml:lang="{$lang}"><xsl:value-of select="$docTitle"/></dct:title>
        <xsl:if test="$fsLabel = 'SOSFS'">
            <rpubl:forfattningssamling rdf:resource="{$sosfsUri}"/>
        </xsl:if>
        <rpubl:fsNummer><xsl:value-of select="$fsNummer"/></rpubl:fsNummer>
    </xsl:template>
    <!-- TODO: kapitel -->
    <xsl:template match="*[@class='sosfs']//h:p/h:strong[contains(text(), '§')]">
        <xsl:variable name="paragrafnummer" select="normalize-space(substring-before(., '§'))"/>
        <rpubl:paragraf>
            <rpubl:Paragraf rdf:ID="paragraf-{$paragrafnummer}">
            <rpubl:paragrafnummer>
                <xsl:value-of select="$paragrafnummer"/>
            </rpubl:paragrafnummer>
        </rpubl:Paragraf>
        </rpubl:paragraf>
    </xsl:template>
    <!-- }}} -->


    <!-- clean or copyover {{{ -->
    <xsl:template match="text()">
        <!-- supress spurious text nodes -->
    </xsl:template>
    <!-- }}} -->


</xsl:stylesheet>
