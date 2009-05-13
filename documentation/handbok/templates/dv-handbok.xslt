<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:xi="http://www.w3.org/2001/XInclude"
                exclude-result-prefixes="xi">

  <xsl:param name="docdate"/>
  <xsl:param name="svnversion"/>

  <xsl:template match="/h:html">
    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="sv">
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
        <title>Handbok : <xsl:value-of select="h:head/h:title"/></title>
        <link rel="stylesheet" type="text/css" media="screen,print" href="../css/dv.css"/>
        <link rel="stylesheet" type="text/css" media="screen,print" href="../css/syntax.css"/>
      </head>
      <body>
        <div id="header">
          <img src="../logotyp.png" class="logo" alt=""/>
        </div>
        <div id="pagemeta">
          <p><span id="pagenumber"/></p>
          <table>
            <tr>
              <th>DATUM</th>
              <th>VERSION</th>
            </tr>
            <tr>
              <td id="docdate">
                <xsl:value-of select="$docdate"/>
              </td>
              <td id="svnversion">
                <xsl:value-of select="$svnversion"/>
              </td>
            </tr>
          </table>
        </div>

        <div id="body">
          <xsl:apply-templates select="h:body/node()"/>
        </div>

      </body>
    </html>
  </xsl:template>

  <xsl:template match="*|@*">
    <xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy>
  </xsl:template>

</xsl:stylesheet>
