<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:owl="http://www.w3.org/2002/07/owl#"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                xmlns:skos="http://www.w3.org/2004/02/skos/core#"
                xmlns:dct="http://purl.org/dc/terms/"
                xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:rpubl="http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#"
                extension-element-prefixes="func"
                xmlns:func="http://exslt.org/functions"
                exclude-result-prefixes="func self"
                xmlns:self="file:.">

    <xsl:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no"/>

    <xsl:include href="common.xslt"/>

    <xsl:template match="/dokumentstatus">
        <rdf:RDF>
            <xsl:apply-templates select="dokument"/>
        </rdf:RDF>
    </xsl:template>

    <xsl:template match="dokument[typ='prop']">
        <rpubl:Proposition rdf:about="{self:rpubl-uri(.)}">
            <dct:identifier>Prop. <xsl:value-of select="rm"/>:<xsl:value-of select="beteckning"/></dct:identifier>
            <rpubl:arsutgava><xsl:value-of select="rm"/></rpubl:arsutgava>
            <rpubl:lopnummer><xsl:value-of select="beteckning"/></rpubl:lopnummer>
            <xsl:apply-templates/>
            <!--
            <xsl:apply-templates select="../dokaktivitet/*"/>
            <xsl:apply-templates select="../dokintressent/*"/>
            <xsl:apply-templates select="../dokforslag/*"/>
            -->
            <dct:publisher rdf:resource="http://rinfo.lagrummet.se/org/regeringskansliet"/>
            <xsl:apply-templates select="../dokuppgift/*"/>
            <xsl:apply-templates select="../dokbilaga/*"/>
            <xsl:apply-templates select="../dokreferens/*"/>
            <xsl:apply-templates select="../dokkategori/*"/>
        </rpubl:Proposition>
    </xsl:template>

    <xsl:template match="titel[text()]">
        <dct:title><xsl:value-of select="."/></dct:title>
    </xsl:template>
    <xsl:template match="subtitel[text()]">
        <dct:alternative><xsl:value-of select="."/></dct:alternative>
    </xsl:template>
    <xsl:template match="publicerad[text()]">
        <rpubl:utkomFranTryck><xsl:value-of select="self:w3c-dt(.)"/></rpubl:utkomFranTryck>
    </xsl:template>

    <xsl:template match="uppgift[kod='inlamnatav']">
        <rpubl:departement>
            <foaf:Organization>
                <foaf:name><xsl:value-of select="text"/></foaf:name>
            </foaf:Organization>
        </rpubl:departement>
    </xsl:template>

    <xsl:template match="bilaga">
        <rpubl:bilaga rdf:parseType="Resource">
            <dct:hasFormat rdf:parseType="Resource">
                <xsl:apply-templates select="*"/>
            </dct:hasFormat>
        </rpubl:bilaga>
    </xsl:template>
    <xsl:template match="filtyp[text()='pdf']">
        <dct:format>application/pdf</dct:format>
    </xsl:template>

    <xsl:template match="referens">
        <dct:references rdf:nodeID="_{ref_dokid}"/>
    </xsl:template>

    <xsl:template match="kategori">
        <dct:subject>
            <skos:Concept>
                <skos:prefLabel xml:lang="sv">
                    <xsl:value-of select="namn"/>
                </skos:prefLabel>
                <skos:broader>
                    <skos:Concept rdf:nodeID="_{thesaurus_id}">
                        <skos:prefLabel xml:lang="sv">
                            <xsl:value-of select="thesaurus_name"/>
                        </skos:prefLabel>
                    </skos:Concept>
                </skos:broader>
            </skos:Concept>
        </dct:subject>
    </xsl:template>

    <xsl:template match="*"/>

</xsl:stylesheet>
