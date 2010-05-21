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
                xmlns:dyn="http://exslt.org/dynamic"
                xmlns:set="http://exslt.org/sets"
                xmlns:gr="http://purl.org/oort/impl/xslt/grit/lib/common#"
                xmlns:self="file:."
                xmlns="http://www.w3.org/1999/xhtml"
                extension-element-prefixes="func">

    <xsl:import href="../../resources/external/xslt/grit/lib/common.xslt"/>

    <xsl:template match="/graph">
        <html>
            <head>
                <title>CoinScheme</title>
                <link rel="stylesheet" type="text/css" href="css/dv.css" />
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
                <xsl:for-each select="coin:template[boolean(coin:relToBase | coin:relFromBase)
                              and not(coin:fragmentTemplate)]">
                    <xsl:sort select="coin:uriTemplate"/>
                    <xsl:apply-templates select=".">
                        <xsl:with-param name="last" select="position() = last()"/>
                    </xsl:apply-templates>
                </xsl:for-each>
            </div>
            <h3>Fragmentmallar (baserade på URI för relaterad resurs)</h3>
            <div id="fragments">
                <xsl:for-each select="coin:template[coin:fragmentTemplate]">
                    <xsl:sort select="coin:fragmentTemplate"/>
                    <xsl:apply-templates select=".">
                        <xsl:with-param name="last" select="position() = last()"/>
                    </xsl:apply-templates>
                </xsl:for-each>
            </div>
            <h2>Symboluppsättningar</h2>
            <xsl:call-template name="slugsets"/>
        </div>
    </xsl:template>

    <xsl:template match="coin:template">
        <xsl:param name="last" select="false()"/>
        <div>
            <xsl:attribute name="class">
                <xsl:text>cointemplate</xsl:text>
                <xsl:if test="$last"> last</xsl:if>
            </xsl:attribute>
            <xsl:if test="coin:uriTemplate">
                <div class="uriTemplate">
                    <xsl:apply-templates select="coin:uriTemplate"/>
                </div>
            </xsl:if>
            <xsl:if test="coin:fragmentTemplate">
                <p>
                    <em>Fragment (<code>#</code>) på formen:</em>
                    <xsl:text> &#160; </xsl:text>
                    <xsl:apply-templates select="coin:fragmentTemplate"/>
                </p>
            </xsl:if>
            <table class="variables">
                <tr>
                    <th>Variabel</th>
                    <th>Egenskap</th>
                </tr>
                <xsl:for-each select="coin:component">
                    <!-- TODO:IMPROVE: this sort expression is quite slow! -->
                    <xsl:sort select="count(str:tokenize(
                              substring-before(../coin:uriTemplate,
                                    concat(self:varname(.), '}')), '}'))"/>
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
            <xsl:apply-templates select="coin:forType"/>
            <xsl:if test="coin:relToBase or coin:relFromBase">
                <xsl:call-template name="relative-template"/>
            </xsl:if>
        </div>
    </xsl:template>

    <xsl:template name="relative-template">
        <!-- TODO: cleanup a bit? -->
        <div class="relative">
            <h4>Relativ relation</h4>
            <xsl:choose>
                <xsl:when test="coin:uriTemplate">
                    <xsl:variable name="baseRelRange"
                                select="key('rel', coin:relToBase/@ref)/rdfs:range"/>
                    <p>
                        <em>
                            <xsl:apply-templates select="key('rel', coin:relToBase/@ref)/rdfs:domain/@ref"/>
                            <xsl:text> som underordnas </xsl:text>
                            <xsl:apply-templates select="$baseRelRange/@ref"/>
                        </em>
                    </p>
                    <p class="rel-rule">
                        {
                        <xsl:apply-templates select="$baseRelRange/@ref"/>
                        }
                        &#8592;
                        <xsl:apply-templates select="coin:relToBase/@ref"/>
                        &#8596;
                        <xsl:apply-templates
                            select="key('rel', coin:relToBase/@ref)/owl:inverseOf/rdfs:label |
                                    key('rel', coin:relToBase/@ref)/owl:inverseOf/@ref"/>
                        &#8594;
                        { <xsl:apply-templates select="coin:component/coin:property/@ref"/> }
                    </p>
                </xsl:when>
                <xsl:when test="coin:fragmentTemplate">
                    <p class="rel-rule">{ Basresurs } &#8592;
                        <xsl:apply-templates
                            select="key('rel', coin:relFromBase/@ref)/owl:inverseOf/rdfs:label |
                                    key('rel', coin:relFromBase/@ref)/owl:inverseOf/@ref"/>
                        &#8596;
                        <xsl:apply-templates select="coin:relFromBase/@ref"/>
                        &#8594;
                        { <xsl:apply-templates select="coin:component/coin:property/@ref"/> }
                    </p>
                    <!--
                    <xsl:apply-templates select="coin:fragmentTemplate"/>
                    -->
                </xsl:when>
            </xsl:choose>
        </div>
    </xsl:template>

    <xsl:template name="slugsets">
        <div id="slugsets">
            <!--
            <xsl:for-each select="$r[a/coin:TokenSet and
                        coin:scheme/@ref = current()/@uri]">
                <xsl:sort select="rdfs:label"/>
                <xsl:apply-templates select="."/>
            </xsl:for-each>
            -->
            <xsl:variable name="slugprop-refs"
                          select="set:distinct(coin:template/coin:component/coin:slugFrom/@ref)"/>
            <xsl:variable name="slugged-resources" select="$r[*[self:is-referenced(., $slugprop-refs)]]"/>
            <xsl:variable name="slugged-type-refs"
                          select="set:distinct(dyn:map($slugged-resources/a/*,
                                                'concat(namespace-uri(.), local-name(.))'))"/>
            <xsl:for-each select="$slugged-type-refs">
                <xsl:sort select="gr:term(.)"/>
                <table class="slugs" id="{gr:term(.)}">
                    <caption>
                        <xsl:text>Symboler av typen </xsl:text>
                        <a href="{.}">
                            <xsl:apply-templates select="$r[@uri = current()]/rdfs:label"/>
                        </a>
                    </caption>
                    <tr>
                        <th class="col-symbol">Symbol</th>
                        <th class="col-resource">Resurs</th>
                    </tr>
                    <xsl:for-each select="$slugged-resources[a/*[gr:name-to-uri(.) = current()]]">
                        <xsl:sort select="local-name(a/*)"/>
                        <xsl:sort select="@uri"/>
                        <tr>
                            <xsl:if test="position() mod 2 = 0">
                                <xsl:attribute name="class">even</xsl:attribute>
                            </xsl:if>
                            <td><xsl:apply-templates select="*[self:is-referenced(., $slugprop-refs)]"/></td>
                            <td>
                                <xsl:apply-templates
                                    select="skos:prefLabel | foaf:name | rdfs:label"/>
                                <div>
                                    <code class="uri">&lt;<xsl:value-of select="@uri"/>&gt;</code>
                                </div>
                            </td>
                            <!--
                            <td><xsl:apply-templates select="a/*"/></td>
                            -->
                        </tr>
                    </xsl:for-each>
                </table>
            </xsl:for-each>
        </div>
    </xsl:template>

    <func:function name="self:is-referenced">
        <xsl:param name="e"/>
        <xsl:param name="refs"/>
        <xsl:variable name="name" select="gr:name-to-uri($e)"/>
        <func:result select="count($refs[. = $name]) > 0"/>
    </func:function>

    <xsl:template match="coin:forType">
        <p>
            <em>Enbart för <xsl:apply-templates select="@ref"/></em>
        </p>
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

    <xsl:template match="coin:base | coin:uriTemplate | coin:fragmentTemplate">
        <code class="{local-name(.)}"><xsl:apply-templates/></code>
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

</xsl:stylesheet>
