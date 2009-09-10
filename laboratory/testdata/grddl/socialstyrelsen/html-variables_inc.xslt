<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns="http://www.w3.org/1999/xhtml">

    <xsl:variable name="sosfsUri">http://rinfo.lagrummet.se/ref/fs/sosfs</xsl:variable>
    <xsl:variable name="sosfsPublBaseUri">http://rinfo.lagrummet.se/publ/sosfs/</xsl:variable>

    <xsl:variable name="fsRawTitle" select="//*[@class='sosfs']//h:h1"/>
    <xsl:variable name="fsId" select="normalize-space(
                    translate($fsRawTitle/text()[following-sibling::h:br], '&#160;', ' ') )"/>
    <xsl:variable name="fsLabel">
        <xsl:value-of select="substring-before($fsId, ' ')"/>
    </xsl:variable>
    <xsl:variable name="docTitle">
            <xsl:value-of select="normalize-space($fsRawTitle/text()[preceding-sibling::h:br])"/>
    </xsl:variable>
    <xsl:variable name="fsNummer">
        <xsl:value-of select="substring-after($fsId, ' ')"/>
    </xsl:variable>

    <xsl:variable name="base" select="concat($sosfsPublBaseUri, $fsNummer)"/>

</xsl:stylesheet>
