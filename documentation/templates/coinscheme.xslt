<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:owl="http://www.w3.org/2002/07/owl#"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                xmlns:coin="http://purl.org/court/def/2009/coin#"
                xmlns:skos="http://www.w3.org/2004/02/skos/core#"
                xmlns:foaf="http://xmlns.com/foaf/0.1/"
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
                <xsl:apply-templates select="resource[a/coin:CoinScheme]"/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="resource[a/coin:CoinScheme]">
        <xsl:variable name="sep" select="coin:separator"/>
        <div class="coinscheme">
            <h1>URI-myntningsschema</h1>
            <p>
                <xsl:text>Bas-URI: </xsl:text>
                <span id="baseuri">
                    <xsl:apply-templates select="coin:root"/>
                </span>
            </p>
            <h2>Regler med förankrade bas-segment</h2>
            <div id="bases">
                <dl>
                    <xsl:for-each select="$r[a/coin:Base and
                                coin:scheme/@ref = current()/@uri]">
                        <xsl:sort select="coin:segment"/>
                        <xsl:apply-templates select="."/>
                    </xsl:for-each>
                </dl>
            </div>
            <h2>Inkapslingar (baserade på URI för relaterad resurs)</h2>
            <div id="containments">
                <xsl:for-each select="$r[a/coin:Containment and
                            coin:scheme/@ref = current()/@uri]">
                    <xsl:sort select="coin:segment | coin:fragmentPrefix"/>
                    <xsl:apply-templates select="."/>
                </xsl:for-each>
            </div>
            <h2>Symboluppsättningar</h2>
            <div id="tokensets">
                <xsl:for-each select="$r[a/coin:TokenSet and
                            coin:scheme/@ref = current()/@uri]">
                    <xsl:sort select="rdfs:label"/>
                    <xsl:apply-templates select="."/>
                </xsl:for-each>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="*[coin:contains | coin:component | coin:segment]">
        <dt>
            <xsl:apply-templates select="coin:segment"/>
            <xsl:if test="coin:segment and coin:component">/</xsl:if>
            <xsl:apply-templates select="coin:component"/>
            <xsl:if test="coin:contains">
                <xsl:text>/</xsl:text>
            </xsl:if>
        </dt>
        <dd>
            <xsl:if test="coin:forType">
                <em>(Av typen: <xsl:apply-templates select="coin:forType/@ref"/>)</em>
            </xsl:if>
            <xsl:if test="coin:contains">
                <dl>
                    <xsl:for-each select="coin:contains">
                        <xsl:sort select="coin:component[not(../coin:segment)]/@ref |
                                coin:segment"/>
                        <xsl:sort select="coin:component/@ref"/>
                        <xsl:apply-templates select="."/>
                    </xsl:for-each>
                </dl>
            </xsl:if>
        </dd>
    </xsl:template>

    <xsl:template match="*[a/coin:Containment]">
        <div class="containment">
            <xsl:choose>
                <xsl:when test="coin:segment">
                    <xsl:variable name="baseRelRange"
                                select="key('rel', coin:baseRel/@ref)/rdfs:range"/>
                    <p>
                        <em>En
                            <xsl:apply-templates select="key('rel', coin:baseRel/@ref)/rdfs:domain/@ref"/>
                        som underordnas en
                        <xsl:apply-templates select="$baseRelRange/@ref"/></em>
                    </p>
                    <em>{
                        <xsl:apply-templates select="$baseRelRange/@ref"/>
                        &#8592;
                        <xsl:apply-templates select="coin:baseRel/@ref"/>
                        &#8596;
                        <xsl:apply-templates
                            select="key('rel', coin:baseRel/@ref)/owl:inverseOf/rdfs:label |
                                    key('rel', coin:baseRel/@ref)/owl:inverseOf/@ref"/>
                        &#8594;
                    }</em>
                    <xsl:text>/</xsl:text>
                    <xsl:apply-templates select="coin:segment"/>
                    <xsl:text>/</xsl:text>
                    <em>{ <xsl:apply-templates select="coin:component/@ref"/> }</em>
                </xsl:when>
                <xsl:when test="coin:fragmentPrefix">
                    <em>{ Resurs &#8592;
                        <xsl:apply-templates
                            select="key('rel', coin:baseRev/@ref)/owl:inverseOf/rdfs:label |
                                    key('rel', coin:baseRev/@ref)/owl:inverseOf/@ref"/>
                        &#8596;
                        <xsl:apply-templates select="coin:baseRev/@ref"/>
                        &#8594;
                    } </em>
                    <xsl:apply-templates select="coin:fragmentPrefix"/>
                    <em>{ <xsl:apply-templates select="coin:component/@ref"/> }</em>
                </xsl:when>
            </xsl:choose>
        </div>
    </xsl:template>

    <xsl:template match="coin:component">
        <xsl:variable name="tokenSet" select="key('rel', ../coin:useTokenSet/@ref)"/>
        <em class="component">
            <xsl:text>{ </xsl:text>
            <xsl:if test="$tokenSet">
                <a href="#{generate-id($tokenSet)}">symbol för</a>
            </xsl:if>
            <xsl:apply-templates select="@ref"/>
            <xsl:text> }</xsl:text>
        </em>
    </xsl:template>

    <xsl:template match="@ref">
        <xsl:text> </xsl:text>
        <xsl:variable name="label" select="key('rel', .)/rdfs:label"/>
        <a href="{.}">
            <xsl:choose>
                <xsl:when test="$label">
                    <xsl:apply-templates select="$label"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates/>
                </xsl:otherwise>
            </xsl:choose>
        </a>
    </xsl:template>

    <xsl:template match="coin:root | coin:segment">
        <strong><code><xsl:apply-templates/></code></strong>
    </xsl:template>

    <xsl:template match="coin:fragmentPrefix">
        <strong>
            <code>#<xsl:apply-templates/></code>
        </strong>
    </xsl:template>

    <xsl:template match="*[a/coin:TokenSet]">
        <table id="{generate-id(.)}">
            <caption><xsl:apply-templates select="rdfs:label"/></caption>
            <tr>
                <th>Symbol</th>
                <th>Resurs</th>
            </tr>
            <xsl:for-each select="$r[coin:tokenSet/@ref = current()/@uri]">
                <xsl:sort select="rdf:value"/>
                <tr>
                    <td><xsl:apply-templates select="rdf:value"/></td>
                    <td>
                        <xsl:for-each select="key('rel', coin:represents/@ref)">
                            <xsl:apply-templates select="skos:prefLabel | foaf:name | rdf:value"/>
                        </xsl:for-each>
                    </td>
                </tr>
            </xsl:for-each>
        </table>
    </xsl:template>

</xsl:stylesheet>
