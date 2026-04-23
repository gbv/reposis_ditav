<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  exclude-result-prefixes="i18n mods xlink">

  <!--
    Project-specific MODS metadata presentation override.
    This keeps the metadata list in one box and orders fields to match the LOD target view.
  -->
  <xsl:import href="xslImport:modsmeta:metadata/mir-tei-viewer.xsl" />

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.modsDefaultType">
    <xsl:call-template name="printMetaDate.mods.categoryContent" />
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.report">
    <xsl:call-template name="printMetaDate.mods.categoryContent" />
  </xsl:template>

  <xsl:template name="printMetaDate.mods.categoryContent">
    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:language) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateCreated) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther[@type='received']) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[@eventType='creation']/mods:place/mods:placeTerm) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:extent) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:physicalDescription/mods:extent) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[@eventType='creation']/mods:publisher) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier[@type='shelfmark']) or
            (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url)">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch open">
          <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <table class="metaData">
            <!-- Titel / Titelübersetzungen -->
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />

            <!-- Abstract / Abstractübersetzungen -->
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract" />

            <!-- Dokumenttyp / Dokumentuntertyp -->
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification[@displayLabel='lod_document_classification']" />
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification[@displayLabel='koloe_document_classification']" />

            <!-- Sprache -->
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />

            <!-- Autor:in/Künstler:in/Interviewpartner:in ... -->
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[mods:role/mods:roleTerm[@authority='marcrelator' and @type='code' and .!='trl']]" />

            <!-- Übersetzer:in -->
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[mods:role/mods:roleTerm[@authority='marcrelator' and @type='code' and .='trl']]" />

            <!-- Erstellungsdatum -->
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[@eventType='creation']/mods:dateCreated" />

            <!-- Weitere Daten -->
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther[@type='received']">
              <xsl:with-param name="label" select="'Weitere Daten'" />
            </xsl:apply-templates>

            <!-- Entstehungsort -->
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[@eventType='creation']/mods:place/mods:placeTerm[@type='text']" />
              <xsl:with-param name="label" select="'Entstehungsort'" />
            </xsl:call-template>

            <!-- Weitere Orte, Geodaten -->
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject/mods:geographic |
                        ./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject/mods:cartographics/mods:coordinates" />
              <xsl:with-param name="label" select="'Weitere Orte, Geodaten'" />
              <xsl:with-param name="sep" select="'; '" />
            </xsl:call-template>

            <!-- Schlagworte -->
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject/mods:topic" />
              <xsl:with-param name="label" select="'Schlagworte'" />
              <xsl:with-param name="sep" select="'; '" />
              <xsl:with-param name="property" select="'keyword'" />
            </xsl:call-template>

            <!-- Seitenanzahl -->
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:extent" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:physicalDescription/mods:extent" />
              <xsl:with-param name="label" select="'Seitenanzahl'" />
            </xsl:call-template>

            <!-- Herausgeber [Herausgebendes Organ] -->
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[@eventType='creation']/mods:publisher" />
              <xsl:with-param name="label" select="'Herausgeber'" />
            </xsl:call-template>

            <!-- Archiv -->
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification[@displayLabel='lod_archives']" />

            <!-- Archivsignatur -->
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier[@type='shelfmark']" />

            <!-- Weitere projektspezifische Angaben -->
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification[not(@displayLabel='lod_document_classification' or @displayLabel='koloe_document_classification' or @displayLabel='lod_archives')]" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
            </xsl:call-template>
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
          </table>
        </div>
      </div>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
