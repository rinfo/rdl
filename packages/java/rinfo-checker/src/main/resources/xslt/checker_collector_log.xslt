<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:rc="http://rinfo.lagrummet.se/ns/2008/10/collector#"
                xmlns="http://www.w3.org/1999/xhtml">

    <xsl:import href="collector_log.xslt"/>

    <xsl:param name="mediabase" select="'media'"/>

    <xsl:template match="/graph">
        <html>
            <head>
                <title>RInfo Checker: insamlingskontroll</title>
                <link rel="stylesheet" type="text/css" href="{$mediabase}/collector_log.css" />
            </head>
            <body>
              <h1>RInfo Checker: insamlingskontroll</h1>
                <xsl:apply-templates select="resource[a/rc:Collect]"/>
            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>
