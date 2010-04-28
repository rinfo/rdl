<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:owl="http://www.w3.org/2002/07/owl#"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                xmlns:skos="http://www.w3.org/2004/02/skos/core#"
                xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:coin="http://purl.org/court/def/2009/coin#"
                xmlns:str="http://exslt.org/strings"
                xmlns:func="http://exslt.org/functions"
                xmlns:gr="http://purl.org/oort/impl/xslt/grit/lib/common#"
                xmlns:self="file:."
                xmlns="http://www.w3.org/1999/xhtml"
                extension-element-prefixes="func">

    <xsl:import href="lib/grit/common.xslt"/>

    <xsl:template match="/graph">
        <html>
            <head>
                <title>CoinScheme</title>
                <link rel="stylesheet" type="text/css" href="dv.css" />
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
                    <xsl:apply-templates select="coin:base"/>
                </span>
            </p>
            <h2>Mallar</h2>
            <h3>Fullständiga URI-mallar</h3>
            <div id="bases">
                <xsl:for-each select="coin:template[coin:uriTemplate and
                            not(coin:relToBase | coin:relFromBase)]">
                    <xsl:sort select="starts-with(coin:uriTemplate, '/publ')" order="descending"/>
                    <xsl:sort select="substring-before(coin:uriTemplate, '}')"/>
                    <xsl:sort select="count(coin:component)"/>
                    <xsl:sort select="coin:uriTemplate"/>
                    <xsl:apply-templates select=".">
                        <xsl:with-param name="last" select="position() = last()"/>
                    </xsl:apply-templates>
                </xsl:for-each>
            </div>
            <h3>Relativa mallar (baserade på URI för relaterad resurs)</h3>
            <div id="containments">
                <xsl:apply-templates select="coin:template[coin:relToBase | coin:relFromBase]"/>
            </div>
            <h2>Symboluppsättningar</h2>
            <div id="tokensets">
                <!--
                <xsl:for-each select="$r[a/coin:TokenSet and
                            coin:scheme/@ref = current()/@uri]">
                    <xsl:sort select="rdfs:label"/>
                    <xsl:apply-templates select="."/>
                </xsl:for-each>
                -->
                <table class="slugs" id="{generate-id(.)}">
                    <!-- TODO: group by type/expected rel..
                    <caption><xsl:apply-templates select="rdfs:label"/></caption>
                    -->
                    <tr>
                        <th>Symbol</th>
                        <th>Resurs</th>
                        <th>Typ</th>
                    </tr>
                    <xsl:for-each select="coin:template/coin:component/coin:slugFrom">
                        <xsl:sort select="@ref"/>
                        <xsl:if test="not(preceding::*/@ref = @ref)">
                            <xsl:variable name="term" select="gr:term(@ref)"/>
                            <xsl:variable name="ns" select="substring-before(@ref, $term)"/>
                            <xsl:for-each select="$r/*[namespace-uri()=$ns and local-name() = $term]">
                                <xsl:sort select="../@uri"/>
                                <tr>
                                    <xsl:if test="position() mod 2 = 0">
                                        <xsl:attribute name="class">even</xsl:attribute>
                                    </xsl:if>
                                    <td><xsl:apply-templates select="."/></td>
                                    <td>
                                        <xsl:for-each select="parent::*">
                                            <xsl:apply-templates
                                                select="skos:prefLabel | foaf:name | rdfs:label"/>
                                        </xsl:for-each>
                                        <div>
                                            <code class="uri">&lt;<xsl:value-of select="../@uri"/>&gt;</code>
                                        </div>
                                    </td>
                                    <td><xsl:apply-templates select="../a/*"/></td>
                                </tr>
                            </xsl:for-each>
                        </xsl:if>
                    </xsl:for-each>
                </table>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="coin:template">
        <xsl:param name="last" select="false()"/>
        <div>
            <xsl:attribute name="class">
                <xsl:text>template</xsl:text>
                <xsl:if test="$last"> last</xsl:if>
            </xsl:attribute>
            <xsl:if test="coin:uriTemplate">
                <div class="uriTemplate">
                    <xsl:apply-templates select="coin:uriTemplate"/>
                </div>
            </xsl:if>
            <xsl:if test="coin:fragmentPrefix">
                <p>
                    <em>Fragment prefixat med:</em>
                    <xsl:apply-templates select="coin:fragmentPrefix"/>
                </p>
            </xsl:if>
            <xsl:if test="coin:relToBase or coin:relFromBase">
                <xsl:call-template name="relative-template"/>
            </xsl:if>
            <xsl:apply-templates select="coin:forType"/>
            <table class="variables">
                <tr>
                    <th>Variabel</th>
                    <th>Egenskap</th>
                </tr>
                <xsl:for-each select="coin:component">
                    <!-- TODO:IMPROVE: this sort expression is quite slow! -->
                    <xsl:sort select="count(str:tokenize(
                              substring-before(../coin:uriTemplate,
                                    concat('{', self:varname(.), '}')), '{}'))"/>
                    <xsl:sort select="coin:variable"/>
                    <xsl:sort select="coin:property/@ref"/>
                    <tr>
                        <td>
                            <code>
                                <xsl:value-of select="self:varname(.)"/>
                            </code>
                        </td>
                        <td>
                            <xsl:if test="coin:slugFrom">
                                <xsl:text>symbol för</xsl:text>
                                <!--
                                <xsl:value-of select="coin:slugFrom/@ref"/>
                                -->
                            </xsl:if>
                            <xsl:apply-templates select="coin:property/@ref"/>
                        </td>
                    </tr>
                </xsl:for-each>
            </table>
        </div>
    </xsl:template>

    <func:function name="self:varname">
        <xsl:param name="e"/>
        <func:result>
            <xsl:choose>
                <xsl:when test="$e/coin:variable">
                    <xsl:value-of select="$e/coin:variable"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="gr:term($e/coin:property/@ref)"/>
                </xsl:otherwise>
            </xsl:choose>
        </func:result>
    </func:function>


    <xsl:template match="coin:forType">
        <p>
            <em>Enbart för <xsl:apply-templates select="@ref"/></em>
        </p>
    </xsl:template>

    <xsl:template name="relative-template">
        <!-- TODO: unused; but reuse for relative templates! -->
        <div class="relative">
            <xsl:choose>
                <xsl:when test="coin:uriTemplate">
                    <xsl:variable name="baseRelRange"
                                select="key('rel', coin:relToBase/@ref)/rdfs:range"/>
                    <p>
                        <em>En
                            <xsl:apply-templates select="key('rel', coin:relToBase/@ref)/rdfs:domain/@ref"/>
                        som underordnas en
                        <xsl:apply-templates select="$baseRelRange/@ref"/></em>
                    </p>
                    <em>{
                        <xsl:apply-templates select="$baseRelRange/@ref"/>
                        }
                        &#8592;
                        <xsl:apply-templates select="coin:relToBase/@ref"/>
                        &#8596;
                        <xsl:apply-templates
                            select="key('rel', coin:relToBase/@ref)/owl:inverseOf/rdfs:label |
                                    key('rel', coin:relToBase/@ref)/owl:inverseOf/@ref"/>
                        &#8594;
                    </em>
                </xsl:when>
                <xsl:when test="coin:fragmentPrefix">
                    <em>{ Basresurs } &#8592;
                        <xsl:apply-templates
                            select="key('rel', coin:relFromBase/@ref)/owl:inverseOf/rdfs:label |
                                    key('rel', coin:relFromBase/@ref)/owl:inverseOf/@ref"/>
                        &#8596;
                        <xsl:apply-templates select="coin:relFromBase/@ref"/>
                        &#8594;
                    </em>
                    <xsl:apply-templates select="coin:fragmentPrefix"/>
                </xsl:when>
            </xsl:choose>
            <em>{ <xsl:apply-templates select="coin:component/coin:property/@ref"/> }</em>
        </div>
    </xsl:template>

    <xsl:template match="a/*">
        <xsl:variable name="uri" select="gr:name-to-uri(.)"/>
        <xsl:variable name="label" select="key('rel', $uri)/rdfs:label"/>
        <a href="{$uri}">
            <xsl:choose>
                <xsl:when test="$label">
                    <xsl:apply-templates select="$label"/>
                </xsl:when>
                <xsl:otherwise>
                    <code>
                        <xsl:value-of select="local-name(.)"/>
                    </code>
                </xsl:otherwise>
            </xsl:choose>
        </a>
    </xsl:template>

    <xsl:template match="@uri | @ref">
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

    <xsl:template match="coin:base | coin:uriTemplate | coin:fragmentPrefix">
        <code class="{local-name(.)}"><xsl:apply-templates/></code>
    </xsl:template>

</xsl:stylesheet>
