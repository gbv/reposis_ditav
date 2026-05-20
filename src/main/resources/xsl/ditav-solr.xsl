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
      <field name="ditav.mods.author.facet">
        <xsl:value-of select="mods:displayForm" />
      </field>
    </xsl:for-each>

    <xsl:for-each select="metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[@eventType = 'received']/mods:agent[count(mods:role/mods:roleTerm[@authority='marcrelator' and text() = 'rcp']) &gt; 0]">
      <field name="ditav.mods.recipient.facet">
        <xsl:value-of select="mods:displayForm" />
      </field>
    </xsl:for-each>

    <xsl:for-each select="metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[@eventType = 'created']/mods:agent/mods:nameIdentifier[@type='dante' and string-length(text()) &gt; 0]">
      <xsl:variable name="nameIdentifier" select="."/>

      <!-- concat https://uri.gbv.de/terminology/lod_organisations/ and >838c2acb-4a07-49d4-857c-cdbcb66b83e9 -->
      <xsl:variable name="dante_link" select="concat($nameIdentifier/@typeURI, $nameIdentifier/text())"/>

      <xsl:choose>
        <xsl:when test="$nameIdentifier/@typeURI = 'https://uri.gbv.de/terminology/lod_organisations/'">
          <field name="ditav.mods.dante_metadata_org_link">
            <xsl:value-of select="$dante_link" />
          </field>
        </xsl:when>

        <xsl:when test="$nameIdentifier/@typeURI = 'https://uri.gbv.de/terminology/lod_persons/'">
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
        <xsl:when test="$nameIdentifier/@typeURI = 'https://uri.gbv.de/terminology/lod_organisations/'">
          <field name="ditav.mods.dante_metadata_org_link">
            <xsl:value-of select="$dante_link" />
          </field>
        </xsl:when>

        <xsl:when test="$nameIdentifier/@typeURI = 'https://uri.gbv.de/terminology/lod_persons/'">
          <field name="ditav.mods.dante_metadata_pers_link">
            <xsl:value-of select="$dante_link" />
          </field>
        </xsl:when>
      </xsl:choose>

    </xsl:for-each>
  </xsl:template>


</xsl:stylesheet>
