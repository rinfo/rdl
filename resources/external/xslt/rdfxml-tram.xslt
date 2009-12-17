<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

    <!-- Last modified: 2009-12-11
         Copyright: Niklas Lindström [lindstream@gmail.com]
         License: BSD-style -->
    <xsl:template name="_description">
        <doas:XSLTStylesheet rdf:about="http://purl.org/oort/impl/xslt/tram/rdfxml-tram.xslt"
                             xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                             xmlns:dct="http://purl.org/dc/terms/"
                             xmlns:foaf="http://xmlns.com/foaf/0.1/"
                             xmlns:doas="http://purl.org/net/ns/doas#">
            <dct:title>Tram</dct:title>
            <dct:description xml:lang="en">Transformable RDF Markup</dct:description>
            <dct:created rdf:datatype="http://www.w3.org/2001/XMLSchema#date"
                         >2009-12-08</dct:created>
            <dct:modified rdf:datatype="http://www.w3.org/2001/XMLSchema#date"
                        >2009-12-11</dct:modified>
            <dct:license rdf:resource="http://usefulinc.com/doap/licenses/bsd"/>
            <foaf:primaryTopic rdf:resource="http://purl.org/oort/def/2009/tram"/>
            <dct:creator>
                <foaf:Person rdf:about="http://purl.org/NET/dust/foaf#self">
                    <foaf:name>Niklas Lindström</foaf:name>
                    <foaf:mbox rdf:resource="mailto:lindstream@gmail.com"/>
                </foaf:Person>
            </dct:creator>
            <dct:format>application/xslt+xml</dct:format>
        </doas:XSLTStylesheet>
    </xsl:template>

    <!-- TODO:
         - does not handle when descriptions for a resource are divided into
           several blocks (i.e. multiple elements with the same subject).

         - interpreted rdf:li (rdf:Seq, rdf:Bag, rdf:Alt)
         - explicit rdf:List..
         - @rdf:ID (to use everywhere @rdf:about is used)
         - @xml:base (to resolve about, resource and ID against)
         - inherited @xml:lang (to use *only* on the literal property itself)

         Unsupported by design:
         - @rdf:nodeID (no named bnodes in output)
    -->

    <xsl:param name="base" select="//*/@xml:base[position()=1]"/>

    <xsl:variable name="all-namespaces" select="//*/namespace::*"/>

    <xsl:key name="bnode" match="//*[@rdf:nodeID]" use="@rdf:nodeID"/>

    <xsl:template match="/">
        <graph>
            <xsl:copy-of select="$all-namespaces"/>
            <xsl:for-each select="rdf:RDF/* | *[not(self::rdf:RDF)]">
                <xsl:apply-templates mode="topresource" select="."/>
            </xsl:for-each>
        </graph>
    </xsl:template>

    <xsl:template mode="topresource" match="*">
        <resource>
            <xsl:apply-templates mode="resourcebody" select="."/>
        </resource>
        <xsl:for-each select="*[not(@rdf:parseType='XMLLiteral')]/*[@rdf:about]">
            <xsl:apply-templates mode="topresource" select="."/>
        </xsl:for-each>
        <!-- If inlined bnodes weren't kept inlined, they should be expanded here.
             select="//*[@rdf:parseType='Resource']" -->
    </xsl:template>

    <xsl:template mode="topresource"
                  match="*[@rdf:nodeID and //*[. != current() and @rdf:nodeID = current()/@rdf:nodeID]]"/>

    <xsl:template mode="topresource" match="rdf:Description[not(*)]"></xsl:template>

    <xsl:template mode="resourcebody" match="*">
        <xsl:apply-templates mode="uri" select="@rdf:about | ../@rdf:about"/>
        <xsl:variable name="elemtype" select="self::*[not(self::rdf:Description)]"/>
        <xsl:apply-templates mode="type" select="$elemtype | rdf:type"/>
        <xsl:apply-templates mode="property" select="@*|*"/>
    </xsl:template>

    <xsl:template mode="uri" match="@*">
        <xsl:attribute name="uri"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <xsl:template mode="property" match="*|@*">
        <xsl:element namespace="{namespace-uri(.)}" name="{name(.)}">
            <xsl:choose>
                <xsl:when test="@rdf:parseType='Collection'">
                    <xsl:for-each select="*">
                        <li>
                            <xsl:choose>
                                <xsl:when test="@rdf:about">
                                    <xsl:attribute name="ref"><xsl:value-of select="@rdf:about"/></xsl:attribute>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:apply-templates mode="resourcebody" select="."/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </li>
                    </xsl:for-each>
                </xsl:when>
                <xsl:when test="*/@rdf:about">
                    <xsl:attribute name="ref"><xsl:value-of select="*/@rdf:about"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="@rdf:resource">
                    <xsl:attribute name="ref"><xsl:value-of select="@rdf:resource"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="@rdf:nodeID">
                    <xsl:variable name="nodeRef" select="."/>
                    <xsl:for-each select="key('bnode', @rdf:nodeID)">
                        <!-- TODO: key should not select referencing properties.. -->
                        <xsl:if test=". != $nodeRef">
                            <xsl:apply-templates mode="resourcebody" select="."/>
                        </xsl:if>
                    </xsl:for-each>
                </xsl:when>
                <xsl:when test="@rdf:parseType='Resource'">
                    <xsl:apply-templates mode="type" select="rdf:type"/>
                    <xsl:apply-templates mode="property" select="*|@*"/>
                </xsl:when>
                <xsl:when test="@rdf:parseType='XMLLiteral'">
                    <xsl:attribute name="fmt">xml</xsl:attribute>
                    <xsl:copy-of select="node()"/>
                </xsl:when>
                <xsl:when test="@rdf:datatype">
                    <xsl:attribute name="fmt">datatype</xsl:attribute>
                    <xsl:call-template name="element-from-uri">
                        <xsl:with-param name="uri" select="@rdf:datatype"/>
                        <xsl:with-param name="body">
                            <xsl:value-of select="."/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="*[not(@rdf:about)]">
                    <xsl:apply-templates mode="resourcebody" select="*"/>
                </xsl:when>
                <!-- plain/language literals -->
                <xsl:otherwise>
                    <xsl:copy-of select="@*"/>
                    <xsl:apply-templates/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <xsl:template mode="property" match="@rdf:about | @rdf:resource | @rdf:nodeID | @rdf:ID |
                  @rdf:parseType | @rdf:datatype"/>

    <xsl:template mode="property" match="rdf:type"></xsl:template>

    <xsl:template mode="type" match="*">
        <!--<a uri="{concat(namespace-uri(.), local-name(.))}">-->
        <a><xsl:copy/></a>
    </xsl:template>

    <xsl:template mode="type" match="rdf:type">
        <a><!-- uri="{@rdf:resource}">-->
            <xsl:call-template name="element-from-uri">
                <xsl:with-param name="uri" select="@rdf:resource"/>
            </xsl:call-template>
        </a>
    </xsl:template>

    <xsl:template name="element-from-uri">
        <xsl:param name="uri"/>
        <xsl:param name="body"/>
        <xsl:variable name="ns">
            <xsl:call-template name="get-ns"><xsl:with-param name="uri"
                               select="$uri"/></xsl:call-template>
        </xsl:variable>
        <xsl:variable name="pfx">
            <xsl:call-template name="get-pfx"><xsl:with-param name="ns"
                               select="$ns"/></xsl:call-template>
        </xsl:variable>
        <xsl:variable name="leaf" select="substring-after($uri, $ns)"/>
        <xsl:variable name="name" select="concat($pfx, $leaf)"/>
        <xsl:element namespace="{$ns}" name="{$name}">
            <xsl:copy-of select="$body"/>
        </xsl:element>
    </xsl:template>

    <xsl:template name="get-ns">
        <xsl:param name="uri"/>
        <xsl:choose>
            <xsl:when test="contains($uri, '#')">
                <xsl:value-of select="concat(substring-before($uri, '#'), '#')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="last-index">
                    <xsl:call-template name="last-index-of">
                        <xsl:with-param name="string" select="$uri" />
                        <xsl:with-param name="token">
                            <xsl:choose>
                                <xsl:when test="contains($uri, '/')">/</xsl:when>
                                <xsl:otherwise>:</xsl:otherwise>
                            </xsl:choose>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:value-of select="substring($uri, 1, number($last-index))"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="get-pfx">
        <xsl:param name="ns"/>
        <xsl:param name="sep" select="':'"/>
        <xsl:variable name="pfx">
            <xsl:value-of select="local-name(ancestor-or-self::*/namespace::*[.=$ns][position()=1])"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$pfx != ''">
                <xsl:value-of select="concat($pfx, ':')"/>
            </xsl:when>
            <xsl:otherwise></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="last-index-of">
        <xsl:param name="string"/>
        <xsl:param name="token"/>
        <xsl:param name="index" select="0"/>
        <xsl:choose>
            <xsl:when test="contains($string, $token)">
                <xsl:call-template name="last-index-of">
                    <xsl:with-param name="string"
                                    select="substring-after($string, $token)"/>
                    <xsl:with-param name="token" select="$token" />
                    <xsl:with-param name="index"
                                    select="$index +
                                    string-length(substring-before($string, $token)) +
                                    string-length($token)"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise><xsl:value-of select="$index"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
