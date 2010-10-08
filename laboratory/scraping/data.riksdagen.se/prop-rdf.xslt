<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:owl="http://www.w3.org/2002/07/owl#"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                xmlns:skos="http://www.w3.org/2004/02/skos/core#"
                xmlns:dct="http://purl.org/dc/terms/"
                xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:rpubl="http://rinfo.lagrummet.se/ns/2008/11/rinfo/publ#"
                extension-element-prefixes="func"
                xmlns:func="http://exslt.org/functions"
                exclude-result-prefixes="func self"
                xmlns:self="file:.">

  <xsl:include href="common.xslt"/>

  <xsl:param name="lang">sv</xsl:param>
  <xsl:param name="use-keywords" select="true()"/>
  <xsl:param name="publisher">http://rinfo.lagrummet.se/org/regeringskansliet</xsl:param>
  <xsl:param name="category-base">tag:data.riksdagen.se,2010:concept:</xsl:param>
  <xsl:param name="cat-sep">:</xsl:param>
  <xsl:param name="id-in-list"/>

  <xsl:output method="xml" indent="yes" encoding="utf-8" omit-xml-declaration="no"/>

  <xsl:template match="/">
    <rdf:RDF>
      <xsl:apply-templates/>
    </rdf:RDF>
  </xsl:template>

  <xsl:template match="/dokumentstatus | /dokumentlista">
    <xsl:choose>
      <xsl:when test="$id-in-list">
        <xsl:apply-templates select="dokument[id=$id-in-list]"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="dokument"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="dokument[typ='prop']">
    <rpubl:Proposition rdf:about="{self:rpubl-uri(.)}">
      <xsl:call-template name="desc-body">
        <xsl:with-param name="prefix">Prop.</xsl:with-param>
      </xsl:call-template>
    </rpubl:Proposition>
  </xsl:template>

  <xsl:template match="dokument[typ='sou']">
    <rpubl:Utredningsbetankande rdf:about="{self:rpubl-uri(.)}">
      <rpubl:utrSerie rdf:resource="http://rinfo.lagrummet.se/serie/utr/sou"/>
      <xsl:call-template name="desc-body">
        <xsl:with-param name="prefix">SOU</xsl:with-param>
      </xsl:call-template>
    </rpubl:Utredningsbetankande>
  </xsl:template>

  <xsl:template match="dokument[typ='ds']">
    <rpubl:Utredningsbetankande rdf:about="{self:rpubl-uri(.)}">
      <rpubl:utrSerie rdf:resource="http://rinfo.lagrummet.se/serie/utr/ds"/>
      <xsl:call-template name="desc-body">
        <xsl:with-param name="prefix">Ds</xsl:with-param>
      </xsl:call-template>
    </rpubl:Utredningsbetankande>
  </xsl:template>

  <xsl:template name="desc-body">
    <xsl:param name="prefix"/>
    <dct:identifier>
      <xsl:value-of select="$prefix"/>
      <xsl:text> </xsl:text>
      <xsl:value-of select="rm"/>
      <xsl:text>:</xsl:text>
      <xsl:value-of select="beteckning"/>
    </dct:identifier>
      <rpubl:arsutgava><xsl:value-of select="rm"/></rpubl:arsutgava>
      <rpubl:lopnummer><xsl:value-of select="beteckning"/></rpubl:lopnummer>
      <owl:sameAs rdf:resource="{dokument_url_html}"/>
      <xsl:apply-templates/>
      <!--
      <xsl:apply-templates select="parent::dokumentstatus/dokaktivitet/*"/>
      <xsl:apply-templates select="parent::dokumentstatus/dokintressent/*"/>
      <xsl:apply-templates select="parent::dokumentstatus/dokforslag/*"/>
      -->
      <dct:publisher rdf:resource="{$publisher}"/>
      <xsl:apply-templates select="parent::dokumentstatus/dokuppgift/*"/>
      <xsl:apply-templates select="parent::dokumentstatus/dokbilaga/*"/>
      <xsl:apply-templates select="parent::dokumentstatus/dokreferens/*"/>
      <xsl:apply-templates select="parent::dokumentstatus/dokkategori/*"/>
  </xsl:template>

  <xsl:template match="titel[text()]">
    <dct:title xml:lang="{$lang}"><xsl:value-of select="."/></dct:title>
  </xsl:template>
  <xsl:template match="subtitel[text()]">
    <dct:alternative><xsl:value-of select="."/></dct:alternative>
  </xsl:template>
  <xsl:template match="publicerad[text()]">
    <rpubl:utkomFranTryck rdf:datatype="http://www.w3.org/2001/XMLSchema#xsd:date"><xsl:value-of select="substring-before(self:w3c-dt(.), 'T')"/></rpubl:utkomFranTryck>
  </xsl:template>

  <xsl:template match="uppgift[kod='inlamnatav' and text != '']">
    <rpubl:departement>
      <foaf:Organization>
        <foaf:name><xsl:value-of select="text"/></foaf:name>
      </foaf:Organization>
    </rpubl:departement>
  </xsl:template>

  <xsl:template match="bilaga[fil_url != '']">
    <rpubl:bilaga rdf:parseType="Resource">
      <dct:hasFormat>
        <rdf:Description rdf:about="{fil_url}">
          <xsl:apply-templates select="filtyp"/>
        </rdf:Description>
      </dct:hasFormat>
    </rpubl:bilaga>
  </xsl:template>
  <xsl:template match="filtyp[text()='pdf']">
    <dct:format>application/pdf</dct:format>
  </xsl:template>

  <xsl:template match="referens">
    <dct:references>
      <rdf:Description rdf:nodeID="_docid-{ref_dokid}">
        <xsl:apply-templates/>
      </rdf:Description>
    </dct:references>
  </xsl:template>
  <xsl:template match="referens/referenstyp">
        <rdfs:label xml:lang="{$lang}"><xsl:value-of select="."/></rdfs:label>
  </xsl:template>
  <xsl:template match="referens/uppgift">
        <rdfs:comment xml:lang="{$lang}"><xsl:value-of select="."/></rdfs:comment>
  </xsl:template>

  <xsl:template match="kategori">
    <xsl:if test="$use-keywords">
      <dct:subject>
        <skos:Concept rdf:about="{$category-base}{domain_id}{$cat-sep}{thesaurus_id}{$cat-sep}{id}">
          <skos:prefLabel xml:lang="{$lang}">
            <xsl:value-of select="namn"/>
          </skos:prefLabel>
          <skos:broader>
            <skos:Concept rdf:about="{$category-base}{domain_id}{$cat-sep}{thesaurus_id}">
              <skos:prefLabel xml:lang="sv">
                <xsl:value-of select="thesaurus_name"/>
              </skos:prefLabel>
              <skos:broader>
                <skos:Concept rdf:about="{$category-base}{domain_id}">
                  <skos:prefLabel xml:lang="sv">
                    <xsl:value-of select="domain_name"/>
                  </skos:prefLabel>
                </skos:Concept>
              </skos:broader>
            </skos:Concept>
          </skos:broader>
          <!--
          -->
        </skos:Concept>
      </dct:subject>
    </xsl:if>
  </xsl:template>

  <xsl:template match="*"/>

</xsl:stylesheet>
