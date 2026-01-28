<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:tei="http://www.tei-c.org/ns/1.0"
    exclude-result-prefixes="tei">

  <xsl:output method="html"/>
  <xsl:template match="tei:TEI">
    <div>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match='tei:teiHeader'/>

<xsl:template match="tei:body">
  <body>
    <xsl:apply-templates/>
  </body>
</xsl:template>

<xsl:template match="tei:div">
  <div>
    <xsl:apply-templates/>
  </div>
</xsl:template>

<xsl:template match="tei:p">
  <p>
    <xsl:apply-templates/>
  </p>
</xsl:template>

<xsl:template match="tei:pb">
  <img src="{@facs}" alt="Seite {@n}"/>
</xsl:template>

<xsl:template match="tei:orgName">
  <a href="{@ref}" target="_blank">
    <xsl:apply-templates/>
  </a>
</xsl:template>

<xsl:template match="tei:placeName">
  <a href="{@ref}" target="_blank">
    <xsl:apply-templates/>
  </a>
</xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="."/>
  </xsl:template>

</xsl:stylesheet>