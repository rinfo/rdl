<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    exclude-result-prefixes="xhtml"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns="http://www.w3.org/1999/xhtml"
    version="1.0">
<xsl:template match="xhtml:html">
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:dct="http://purl.org/dc/terms/"
    xmlns="http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#">
    <Kommittedirektiv>
        <xsl:attribute name="rdf:about">
            <xsl:value-of select="xhtml:head/xhtml:meta[@name='DC.identifier']/@content" />
        </xsl:attribute>
    </Kommittedirektiv>
</rdf:RDF>
</xsl:template>
</xsl:stylesheet>
