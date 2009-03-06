<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:st="http://oort.to/ns/2008/09/sparqltree">
  <!--
                xmlns="http://www.w3.org/1999/xhtml">
  -->

  <xsl:output method="html" indent="yes" encoding="utf-8" omit-xml-declaration="yes"/>
  <!--
              doctype-public="-//W3C//DTD XHTML+RDFa 1.0//EN"/>
  -->

  <xsl:template match="/st:tree">
    <html xml:lang="sv">
      <head profile="http://www.w3.org/ns/rdfa/">
        <title>Organisationer</title>
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
              <xsl:if test="orgAlt">
                (<xsl:value-of select="orgAlt"/>)
              </xsl:if>
            </h2>
            <div>
              <xsl:apply-templates select="@uri"/>
            </div>
            <xsl:if test="orgComment">
              <p>
                <em>(<xsl:value-of select="orgComment"/>)</em>
              </p>
            </xsl:if>
            <dl>
              <dt>Serier</dt>
              <xsl:if test="serie">
                <dd>
                  <ul>
                    <xsl:for-each select="serie">
                      <li>
                      <p>
                        <a href="{@uri}">
                          <xsl:value-of select="serieShortname"/>
                        </a>
                        <xsl:text> </xsl:text>
                        <em><xsl:value-of select="serieName"/></em>
                        <xsl:text> </xsl:text>
                        <xsl:apply-templates select="@uri"/>
                        (<xsl:value-of select="serieType/serieTypeLabel"/>)
                      </p>
                        <xsl:if test="serieComment">
                          <p>
                            <em>(<xsl:value-of select="serieComment"/>)</em>
                          </p>
                        </xsl:if>
                      </li>
                    </xsl:for-each>
                  </ul>
                </dd>
              </xsl:if>
              <xsl:if test="feed">
                <dt>Feeds</dt>
                <dd>
                  <ul>
                    <xsl:for-each select="feed">
                      <li>
                        <a href="{currentFeed/@uri}">
                          <xsl:value-of select="feedId"/>
                        </a>
                      </li>
                    </xsl:for-each>
                  </ul>
                </dd>
              </xsl:if>
            </dl>
          </xsl:for-each>
        </div>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="@uri">
    <code>
      <xsl:text>&lt;</xsl:text>
      <xsl:value-of select="."/>
      <xsl:text>&gt;</xsl:text>
    </code>
  </xsl:template>

</xsl:stylesheet>
