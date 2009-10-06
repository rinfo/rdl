<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    exclude-result-prefixes="h"
    xmlns:h="http://www.w3.org/1999/xhtml"
    xmlns:set="http://exslt.org/sets"
    extension-element-prefixes="set"
    version="1.0">
    <xsl:output method="xml" indent="yes"/>
    <xsl:template match="h:html">
        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:dct="http://purl.org/dc/terms/"
            xmlns:foaf="http://xmlns.com/foaf/0.1/"
            xmlns="http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#">
            <Kommittedirektiv>
                <xsl:attribute name="rdf:about">
                    <xsl:value-of select="h:head/h:meta[@name='DC.identifier']/@content" />
                </xsl:attribute>
                <direktivnummer><xsl:value-of select="h:head/h:meta[@name='RPUBL.direktivnummer']/@content" /></direktivnummer>
                <dct:title xml:lang="sv"><xsl:value-of select="h:head/h:meta[@name='RPUBL.title']/@content" /></dct:title>
                <beslutsdatum rdf:datatype="http://www.w3.org/2001/XMLSchema#date"><xsl:value-of select="h:head/h:meta[@name='RPUBL.beslutsdatum']/@content" /></beslutsdatum>
                <avkravtAvrapporteringsdatum rdf:datatype="http://www.w3.org/2001/XMLSchema#date"><xsl:value-of select="h:head/h:meta[@name='RPUBL.avkravtavrapporteringsdatum']/@content" /></avkravtAvrapporteringsdatum>
                <dct:publisher>
                    <xsl:attribute name="rdf:resource">
                        <xsl:value-of select="h:head/h:link[@rel='DC.publisher']/@href" />
                    </xsl:attribute>
                </dct:publisher>
                <departement rdf:parseType="Resource">
                    <foaf:name><xsl:value-of select="h:head/h:meta[@name='RPUBL.departement']/@content" /></foaf:name>
                </departement>

                <xsl:for-each select="set:distinct(h:body/h:div[@class='rpubl-content']//h:a/@href[starts-with(., 'http://rinfo.lagrummet.se')])">
                    <xsl:sort select="." />
                    <xsl:choose>
                        <xsl:when test="contains(., '#K')">
                            <dct:references>
                                <xsl:comment><xsl:text> </xsl:text><xsl:value-of select="."/><xsl:text> </xsl:text></xsl:comment>
                                <Forfattningsreferens>
                                    <angerGrundforfattning rdf:resource="{substring-before(., '#')}" />
                                        <xsl:variable name="fragment" select="substring-after(., '#K')"/>
                                        <xsl:choose>
                                            <xsl:when test="contains($fragment, '-P')">
                                                <angerKapitelnummer>
                                                    <xsl:value-of select="substring-before($fragment, '-P')"/>
                                                </angerKapitelnummer>
                                                <angerParagrafnummer><xsl:value-of select="substring-after($fragment, '-P')"/></angerParagrafnummer>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <angerKapitelnummer>
                                                    <xsl:value-of select="$fragment"/>
                                                </angerKapitelnummer>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                </Forfattningsreferens>
                            </dct:references>
                        </xsl:when>
                        <xsl:when test="contains(., '#P')">
                            <dct:references>
                                <Forfattningsreferens>
                                    <angerGrundforfattning rdf:resource="{substring-before(., '#P')}" />
                                    <angerParagrafnummer><xsl:value-of select="substring-after(., '#P')"/></angerParagrafnummer>
                                </Forfattningsreferens>
                            </dct:references>
                        </xsl:when>
                        <xsl:otherwise>
                            <dct:references>
                                <xsl:attribute name="rdf:resource">
                                    <xsl:value-of select="." />
                                </xsl:attribute>
                            </dct:references>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
            </Kommittedirektiv>
        </rdf:RDF>
    </xsl:template>
</xsl:stylesheet>
