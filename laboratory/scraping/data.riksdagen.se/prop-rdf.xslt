<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:owl="http://www.w3.org/2002/07/owl#"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                xmlns:skos="http://www.w3.org/2004/02/skos/core#"
                xmlns:dct="http://purl.org/dc/terms/"
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

    <xsl:template match="dokument">
        <rpubl:Proposition rdf:about="{self:rpubl-uri(.)}">
            <rpubl:arsutgava><xsl:value-of select="rm"/></rpubl:arsutgava>
            <rpubl:lopnummer><xsl:value-of select="beteckning"/></rpubl:lopnummer>
            <rpubl:utkomFranTryck><xsl:value-of select="self:w3c-dt(publicerad)"/></rpubl:utkomFranTryck>
            <dct:title><xsl:value-of select="titel"/></dct:title>
        </rpubl:Proposition>
    </xsl:template>

</xsl:stylesheet>
