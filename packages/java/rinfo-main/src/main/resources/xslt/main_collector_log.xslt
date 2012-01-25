<!DOCTYPE xsl:stylesheet SYSTEM "collector_log.dtd">
<xsl:stylesheet version="1.0"
                xmlns="http://www.w3.org/1999/xhtml">

    <xsl:import href="collector_log.xslt"/>

    <xsl:param name="mediabase" select="'media'"/>

    <xsl:template match="/graph">
        <html lang="sv">
            <head>
                <title>RInfo Main: insamlingsrapport</title>
                <link rel="stylesheet" type="text/css" href="{$mediabase}/collector_log.css" />
            </head>
            <body>
              <h1>RInfo Main: insamlingsrapport</h1>
                <xsl:apply-templates select="resource[a/rc:Collect]"/>
                <xsl:if test="not(resource[a/rc:Collect]/iana:via)">
                    <xsl:apply-templates select="resource[a/awol:Feed]"/>
                </xsl:if>
                <xsl:apply-templates select="resource[a/rc:PageError]"/>
            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>
