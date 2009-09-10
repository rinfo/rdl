<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/2005/Atom"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:at="http://purl.org/atompub/tombstones/1.0"
                xmlns:le="http://purl.org/atompub/link-extensions/1.0"
                xmlns:rae="http://rinfo.lagrummet.se/ns/2009/09/atom-extensions#">

    <xsl:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no"/>

    <xsl:variable name="rinfoPublBaseUri">http://rpubl.lagrummet.se/publ</xsl:variable>

    <xsl:template match="/h:html">
        <feed xmlns="http://www.w3.org/2005/Atom"
              xmlns:le="http://purl.org/atompub/link-extensions/1.0"
              xmlns:rae="http://rinfo.lagrummet.se/ns/2009/09/atom-extensions#"
              xml:lang="sv">
            <!-- http://www.socialstyrelsen.se/sosfs -->
            <id>tag:socialstyrelsen.se,2009:rinfo</id>
            <title>Socialstyrelsens författningssamling</title>
            <updated>2009-01-23T12:42:32Z</updated>
            <author>
                <name>Socialstyrelsen</name>
                <uri>http://www.socialstyrelsen.se/</uri>
                <email>juridik@socialstyrelsen.se</email>
            </author>
            <!--
            <link rel="self" href="{}"/>
            -->
            <xsl:apply-templates select=".//h:div[@id='socextPageBody']/h:p[h:a[@href]]"/>
        </feed>
    </xsl:template>

    <xsl:template match="h:p[h:a[@href]]">
        <entry>
            <id><xsl:value-of select="$rinfoPublBaseUri"/><xsl:value-of select="h:a/@href"/></id>
            <updated></updated>
            <published></published>
            <title><xsl:value-of select="h:a"/></title>
            <summary><xsl:value-of select="normalize-space(text()[preceding-sibling::h:br])"/></summary>
            <content src="{h:a/@href}"
                     type="application/xhtml"
                     le:md5="00000000000000000000000000000000"/>
            <!--
            <link rel="alternate" href="{''}"
                  type="application/rdf+xml"
                  length="1465" le:md5="143aa16eb05095675651b5e8380aff8d"/>
            -->
            <!--
            <link rel="enclosure" href="{''}"
                  type="application/pdf"
                  rae:filename="bilaga/SOSFS-el{''}.pdf"
                  length="1168037" le:md5="00000000000000000000000000000000"/>
            -->
        </entry>
    </xsl:template>

</xsl:stylesheet>
