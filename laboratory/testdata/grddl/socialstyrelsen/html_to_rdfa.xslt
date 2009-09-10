<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns="http://www.w3.org/1999/xhtml">


    <!--

    Syfte: Denna transformation gör om den råa (X)HTML som Socialstyrelsen publicerar
    myndighetsföreskrifter på till XHTML+RDFa.

    Slutsats: Det är *inte värt mödan* att försöka städa upp i den markup som
    produceras. Även om det är möligt, som detta exempel illustrerar, blir det
    väldigt skört och är orimligt att underhålla (bl.a. p.g.a. hänsyn till
    mänger av potentiella avvikelser etc.).

    Förslag: Att fånga detaljerna och annotera mer RDFa var görbart. Procuera
    istället *enbart* påståenden som RDF/XML och ignorera den oregerligt
    uppmärkta löptexten. Detta är sannolikt långt mer rimligt att underhålla.

    -->

    <xsl:include href="html-variables_inc.xslt"/>


    <xsl:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no"/>

    <xsl:template match="/h:html">
        <html xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
              xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
              xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
              xmlns:dct="http://purl.org/dc/terms/"
              xmlns:dc="http://purl.org/dc/elements/1.1/"
              xmlns:rpubl="http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#"
              xmlns:fs="http://rinfo.lagrummet.se/serie/fs/"
              xml:lang="sv">
            <head>
                <title><xsl:value-of select="h:head/h:title"/></title>
                <base href="{$base}"/>
                <meta http-equiv="Content-Type" content="text/html;charset=utf-8"/>
            </head>
            <body about="" typeof="rpubl:Myndighetsforeskrift">
                <div class="aside" role="contentinfo">
                    <span property="dct:identifier" xml:lang=""><xsl:value-of select="$fsId"/></span>
                    <xsl:apply-templates select=".//h:div[@id='socextQuickLinkArea']//*[@class='facts']"/>
                    <xsl:apply-templates select=".//h:div[@id='socextQuickLinkArea']//*[@class='keywords']"/>
                </div>
                <div class="article" role="main">
                    <xsl:apply-templates select=".//h:div[@id='socextContentPageArea']/node()"/>
                </div>
            </body>
        </html>
    </xsl:template>


    <!-- contentinfo {{{ -->
    <xsl:template match="*[@class='facts']//h:dl/h:dd[preceding-sibling::h:dt[text()='Beslutad:']]">
        <xsl:copy>
            <xsl:attribute name="property">rpubl:beslutsdatum</xsl:attribute>
            <xsl:attribute name="datatype">xsd:date</xsl:attribute>
            <xsl:attribute name="content"><xsl:value-of select="."/></xsl:attribute>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="*[@class='facts']//h:dl/h:dd[preceding-sibling::h:dt[text()='Tryckdatum:']]">
        <xsl:copy>
            <xsl:attribute name="property">rpubl:utkomFranTryck</xsl:attribute>
            <xsl:attribute name="datatype">xsd:date</xsl:attribute>
            <xsl:attribute name="content"><xsl:value-of select="."/></xsl:attribute>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="*[@class='facts']//h:dl/h:dd[preceding-sibling::h:dt[text()='Ansvarig utgivare:']]">
        <xsl:copy>
            <a rel="dct:publisher" href="http://rinfo.lagrummet.se/org/socialstyrelsen">
                <xsl:apply-templates/>
            </a>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="h:div[@id='socextQuickLinkArea']//*[@class='keywords']//h:a[@rel='nofollow']">
        <span property="dc:subject">
            <xsl:choose>
                <xsl:when test="contains(., ',')">
                    <xsl:value-of select="substring-before(normalize-space(.), ',')"/>
                </xsl:when>
                <xsl:otherwise><xsl:value-of select="normalize-space(.)"/></xsl:otherwise>
            </xsl:choose>
        </span>
    </xsl:template>
    <!-- }}} -->


    <!-- main {{{ -->

    <xsl:template match="*[@class='sosfs']//h:h1">
        <h1 property="dct:title"><xsl:value-of select="$docTitle"/></h1>
        <h2>
            <xsl:if test="$fsLabel = 'SOSFS'">
                <a rel="rpubl:forfattningssamling" href="{$sosfsUri}"><xsl:value-of select="$fsLabel"/></a>
            </xsl:if>
            <xsl:text> </xsl:text>
            <span property="rpubl:fsNummer" xml:lang=""><xsl:value-of select="$fsNummer"/></span>
        </h2>
    </xsl:template>

    <xsl:template match="*[@class='sosfs']//h:p[h:strong[contains(text(), '§')]]">
        <xsl:apply-templates/>
        <xsl:text disable-output-escaping="yes"><![CDATA[</p>]]></xsl:text>
        <xsl:text disable-output-escaping="yes"><![CDATA[</div>]]></xsl:text>
    </xsl:template>

    <xsl:template match="*[@class='sosfs']//h:p/h:strong[contains(text(), '§')]">
        <xsl:variable name="paragrafnummer" select="normalize-space(substring-before(., '§'))"/>
        <xsl:if test="preceding-sibling::h:strong[contains(text(), '§')]">
            <xsl:text disable-output-escaping="yes"><![CDATA[</p>]]></xsl:text>
            <xsl:text disable-output-escaping="yes"><![CDATA[</div>]]></xsl:text>
        </xsl:if>
        <xsl:text disable-output-escaping="yes"><![CDATA[<div class="paragraph"]]></xsl:text>
            <xsl:text> rel="rpubl:paragraf"</xsl:text>
            <xsl:text> resource="#paragraf-</xsl:text><xsl:value-of select="$paragrafnummer"
                    /><xsl:text>"</xsl:text>
            <xsl:text disable-output-escaping="yes"><![CDATA[>]]></xsl:text>
        <span rel="rdf:type" resource="[rpubl:Paragraf]"/>
        <xsl:element name="h4">
            <span property="rpubl:paragrafnummer" xml:lang="">
                <xsl:value-of select="$paragrafnummer"/>
            </span> §
        </xsl:element>
        <xsl:text disable-output-escaping="yes"><![CDATA[<p>]]></xsl:text>
    </xsl:template>

    <xsl:template match="*[@class='sosfs']//h:p/h:br[following-sibling::h:br]"></xsl:template>
    <xsl:template match="*[@class='sosfs']//h:p/h:br[preceding-sibling::h:br]"></xsl:template>

    <!-- }}} -->


    <!-- clean or copyover {{{ -->

    <xsl:template match="h:div[@id='socextPageBody']">
        <xsl:apply-templates select="node()[not(
                                self::h:*[@class='tool-links'] |
                                self::h:*[@class='important'] |
                                self::h:*[@class='disclaimer'] |
                                self::h:*[contains(concat(' ', @class, ''), ' buttonContainer ')] )]"/>
        <div class="aside" role="">
            <xsl:apply-templates select="
                                 h:*[@class='tool-links'] |
                                 h:*[@class='important'] |
                                 h:*[@class='disclaimer'] |
                                 h:*[contains(concat(' ', @class, ''), ' buttonContainer ')] "/>
        </div>
    </xsl:template>

    <xsl:template match="h:*[@class='status']">
        <xsl:copy>
            <xsl:attribute name="style">display:none</xsl:attribute>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="h:div[@class='clear' and not(*)]"></xsl:template>

    <xsl:template match="h:script"></xsl:template>

    <xsl:template match="@style"></xsl:template>

    <xsl:template match="*|@*">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    <!-- }}} -->


</xsl:stylesheet>
