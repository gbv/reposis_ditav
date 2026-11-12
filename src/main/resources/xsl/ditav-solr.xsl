<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  exclude-result-prefixes="xlink mods">

  <xsl:import href="xslImport:solr-document:ditav-solr.xsl" />

  <xsl:strip-space elements="mods:*" />

  <xsl:template match="mycoreobject[./metadata/def.modsContainer/modsContainer/mods:mods]">
    <xsl:apply-imports />

    <xsl:for-each select="metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier[@type]">
      <field name="mods.identifier.type.{@type}">
        <xsl:value-of select="text()" />
      </field>
    </xsl:for-each>

    <xsl:for-each select="metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo[@xml:lang]">
      <field name="ditav.mods.title.lang.{@xml:lang}">
        <xsl:value-of select="mods:title" />
      </field>
    </xsl:for-each>

    <xsl:for-each select="metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[@eventType = 'created']/mods:agent[count(mods:role/mods:roleTerm[@authority='marcrelator' and text() = 'aut']) &gt; 0]">
      <field name="ditav.mods.origin.author.facet">
        <xsl:value-of select="mods:displayForm" />
      </field>
    </xsl:for-each>

    <xsl:for-each select="metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[@eventType = 'received']/mods:agent[count(mods:role/mods:roleTerm[@authority='marcrelator' and text() = 'rcp']) &gt; 0]">
      <field name="ditav.mods.origin.receipient.facet">
        <xsl:value-of select="mods:displayForm" />
      </field>
    </xsl:for-each>

    <xsl:for-each select="metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:agent/mods:nameIdentifier[@type='dante' and string-length(text()) &gt; 0]">
      <xsl:variable name="nameIdentifier" select="."/>

      <!-- concat https://uri.gbv.de/terminology/lod_organisations/ and >838c2acb-4a07-49d4-857c-cdbcb66b83e9 -->
      <xsl:variable name="dante_link" select="concat($nameIdentifier/@typeURI, $nameIdentifier/text())"/>

      <xsl:choose>
        <xsl:when test="contains($nameIdentifier/@typeURI, '//uri.gbv.de/terminology/lod_organisations/')">
          <field name="ditav.mods.dante_metadata_org_link">
            <xsl:value-of select="$dante_link" />
          </field>
        </xsl:when>

        <xsl:when test="contains($nameIdentifier/@typeURI, '//uri.gbv.de/terminology/lod_persons/')">
          <field name="ditav.mods.dante_metadata_pers_link">
            <xsl:value-of select="$dante_link" />
          </field>
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>

    <!-- TODO: change to @valueURI if fixed in data -->
    <!-- <mods:nameIdentifier type="dante" typeURI="https://uri.gbv.de/terminology/lod_organisations/">838c2acb-4a07-49d4-857c-cdbcb66b83e9</mods:nameIdentifier> -->
    <xsl:for-each
      select="metadata/def.modsContainer/modsContainer/mods:mods/mods:subject/mods:name[@type='personal' and count(mods:nameIdentifier[@type='dante']) &gt; 0]">
      
      <xsl:variable name="nameIdentifier" select="mods:nameIdentifier[@type='dante']"/>

      <!-- concat https://uri.gbv.de/terminology/lod_organisations/ and >838c2acb-4a07-49d4-857c-cdbcb66b83e9 -->
      <xsl:variable name="dante_link" select="concat($nameIdentifier/@typeURI, $nameIdentifier/text())"/>

      <field name="ditav.mods.dante_metadata_link">
        <xsl:value-of select="$dante_link"/>
      </field>

      <xsl:choose>
        <xsl:when test="contains($nameIdentifier/@typeURI, '//uri.gbv.de/terminology/lod_organisations/')">
          <field name="ditav.mods.dante_metadata_org_link">
            <xsl:value-of select="$dante_link" />
          </field>
        </xsl:when>

        <xsl:when test="contains($nameIdentifier/@typeURI, '//uri.gbv.de/terminology/lod_persons/')">
          <field name="ditav.mods.dante_metadata_pers_link">
            <xsl:value-of select="$dante_link" />
          </field>
        </xsl:when>
      </xsl:choose>

    </xsl:for-each>

    <!-- Index place terms from originInfo (created/received) -->
    <xsl:for-each select="metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[@eventType='created' or @eventType='received']/mods:place/mods:placeTerm">
      <field name="ditav.mods.place">
        <xsl:value-of select="text()" />
      </field>
    </xsl:for-each>

    <!-- Index displayDate from originInfo -->
    <xsl:for-each select="metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:displayDate">
      <field name="ditav.mods.displayDate">
        <xsl:value-of select="text()" />
      </field>
      
      <!-- Extract numeric year from displayDate (format: dd/mm/yyyy or similar) -->
      <xsl:variable name="dateText" select="text()" />
      <xsl:if test="string-length($dateText) &gt; 0">
        <xsl:variable name="year">
          <xsl:call-template name="extractYear">
            <xsl:with-param name="date" select="$dateText" />
          </xsl:call-template>
        </xsl:variable>
        <xsl:if test="string-length($year) &gt; 0">
          <field name="ditav.mods.year"><xsl:value-of select="$year" /></field>
        </xsl:if>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <!-- Template to extract year from displayDate -->
  <xsl:template name="extractYear">
    <xsl:param name="date" />
    
    <!-- Try to match year pattern (4 digits) -->
    <xsl:variable name="yearPattern" select="translate($date, '0123456789', '          ')" />
    <xsl:variable name="digitsOnly" select="translate($date, $yearPattern, '')" />
    
    <!-- Find first 4-digit sequence -->
    <xsl:variable name="firstYear">
      <xsl:call-template name="findFourDigitYear">
        <xsl:with-param name="text" select="$digitsOnly" />
        <xsl:with-param name="pos" select="1" />
      </xsl:call-template>
    </xsl:variable>
    
    <xsl:value-of select="$firstYear" />
  </xsl:template>

  <!-- Recursive template to find 4-digit year -->
  <xsl:template name="findFourDigitYear">
    <xsl:param name="text" />
    <xsl:param name="pos" />
    
    <xsl:if test="$pos &lt;= string-length($text) - 3">
      <xsl:variable name="candidate" select="substring($text, $pos, 4)" />
      <xsl:choose>
        <xsl:when test="translate($candidate, '0123456789', '          ') = ''">
          <!-- Found 4 digits, check if it's a reasonable year (1500-2099) -->
          <xsl:variable name="yearNum" select="number($candidate)" />
          <xsl:if test="$yearNum &gt;= 1500 and $yearNum &lt;= 2099">
            <xsl:value-of select="$candidate" />
          </xsl:if>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="findFourDigitYear">
            <xsl:with-param name="text" select="$text" />
            <xsl:with-param name="pos" select="$pos + 1" />
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
