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

  </xsl:template>


</xsl:stylesheet>
