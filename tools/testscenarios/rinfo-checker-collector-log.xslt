<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                xmlns:dct="http://purl.org/dc/terms/"
                xmlns:iana="http://www.iana.org/assignments/relation/"
                xmlns:rx="http://www.w3.org/2008/09/rx#"
                xmlns:awol="http://bblfish.net/work/atom-owl/2006-06-06/#"
                xmlns:tl="http://purl.org/NET/c4dm/timeline.owl#"
                xmlns:rc="http://rinfo.lagrummet.se/ns/2008/10/collector#"
                xmlns="http://www.w3.org/1999/xhtml">

  <xsl:key name="rel" match="/graph/resource" use="@uri"/>
  <xsl:variable name="r" select="/graph/resource"/>


    <xsl:template match="/graph">
        <html>
            <head>
                <title></title>
                <link rel="stylesheet" type="text/css" href="" />
            </head>
            <body>
                <xsl:apply-templates select="resource[a/rc:Collect]"/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="*[a/rc:Collect]">
      <h1>Insamlingslogg</h1>
      <dl>
        <dt>Start:</dt>
        <dd><xsl:apply-templates select="tl:start"/></dd>
        <dt>Stopp:</dt>
        <dd><xsl:apply-templates select="tl:end"/></dd>
      </dl>
      <xsl:for-each select="iana:via">
        <xsl:apply-templates select="key('rel', ./@ref)"/>
      </xsl:for-each>
    </xsl:template>

    <xsl:template match="*[a/awol:Feed]">
      <div>
        <h2>Källa</h2>
        <dl>
          <dt>Identifierare:</dt>
          <dd><xsl:value-of select="awol:id"/></dd>
          <dt>Feed-sidans URI:</dt>
          <dd><xsl:value-of select="iana:self/@ref"/></dd>
          <dt>Uppdaterad:</dt>
          <dd><xsl:apply-templates select="awol:updated"/></dd>
        </dl>
        <table>
          <tr>
            <th>Tidpunkt</th>
            <th>Typ</th>
            <th>Objekt</th>
            <th>Givet värde</th>
            <th>Beräknat värde</th>
          </tr>
          <xsl:for-each select="//*[a/awol:Entry and awol:source/@ref = current()/@uri]">
            <xsl:apply-templates select="parent::resource" mode="trow"/>
          </xsl:for-each>
        </table>
      </div>
    </xsl:template>

    <xsl:template match="*[a/rc:ChecksumError]" mode="trow">
      <tr class="error">
        <td><xsl:apply-templates select="tl:at"/></td>
        <td>Fel MD5-summa</td>
        <td><xsl:value-of select="rc:document"/></td>
        <td><xsl:value-of select="rc:givenMd5"/></td>
        <td><xsl:value-of select="rc:computedMd5"/></td>
      </tr>
    </xsl:template>

</xsl:stylesheet>
