<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:st="http://oort.to/ns/2008/09/sparqltree"
                xmlns="http://www.w3.org/1999/xhtml">

  <xsl:output method="html" indent="yes" encoding="utf-8" omit-xml-declaration="yes"
              doctype-public="-//W3C//DTD XHTML+RDFa 1.0//EN"/>

  <xsl:template match="/st:tree">
    <html xml:lang="sv">
      <head profile="http://www.w3.org/ns/rdfa/">
        <title><xsl:value-of select="ontology/title | ontology/label"/></title>
        <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
        <link rel="stylesheet" href="/css/ontology.css" />
      </head>
      <body>
        <div id="main">
            <h1>Organisationer</h1>
            <xsl:for-each select="org">
                <xsl:sort select="orgName"/>
                <h2>
                    <a href="{@uri}"><xsl:value-of select="orgName"/></a>
                </h2>
                <dl>
                    <dt>Serier</dt>
                    <dd>
                        <ul>
                            <xsl:for-each select="serie">
                                <li>
                                    <a href="{@uri}">
                                        <xsl:value-of select="serieShortname"/>
                                    </a>
                                    (<xsl:value-of select="serieName"/>)
                                    [<xsl:value-of select="serieType/serieTypeLabel"/>]
                                </li>
                            </xsl:for-each>
                        </ul>
                    </dd>
                    <!-- TODO:
                    <dt>Feeds</dt>
                    <dd>
                        <ul>
                        </ul>
                    </dd>
                    -->
                </dl>
            </xsl:for-each>
        </div>
      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>
