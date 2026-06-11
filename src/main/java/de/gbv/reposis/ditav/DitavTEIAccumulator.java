package de.gbv.reposis.ditav;

import static de.gbv.reposis.ditav.DitavConstants.TEI_NAMESPACE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.solr.index.file.MCRSolrFileIndexAccumulator;

import com.google.gson.JsonObject;

import de.gbv.reposis.ditav.geonames.DitavGeoNamesService;

public class DitavTEIAccumulator implements MCRSolrFileIndexAccumulator {

    public static final String DITAV_GENERIC_FILE_LINK_SOLR_FIELD = "ditav.mods.dante_file_link";
    public static final String DITAV_PERSON_FILE_LINK_SOLR_FIELD = "ditav.mods.dante_file_pers_link";
    public static final String DITAV_ORGANIZATION_FILE_LINK_SOLR_FIELD = "ditav.mods.dante_file_org_link";

    public static final String DITAV_GEONAMES_ID_SOLR_FIELD = "ditav.tei.geonames_id";
    public static final String DITAV_GEONAMES_LINK_SOLR_FIELD = "ditav.tei.geonames_link";
    public static final String DITAV_PLACE_SOLR_FIELD = "ditav.tei.place";
    public static final String DITAV_COORDINATES_SOLR_FIELD = "ditav.tei.coordinates";

    private static final Logger LOGGER = LogManager.getLogger();

    private static void extractTEIElement(SolrInputDocument document, String elementName,
        String refURIPrefix, Document teiDocument, String... linkFields) {
        XPathExpression<Element> persons = XPathFactory.instance().compile(
            ".//tei:" + elementName + "[contains(@ref, '" + refURIPrefix + "')]", Filters.element(), null,
            TEI_NAMESPACE);
        List<String> refs = persons.evaluate(teiDocument).stream()
            .flatMap(el -> Stream.of(el.getAttributeValue("ref").split(",")))
            .distinct()
            .toList();

        for (String refElement : refs) {
            for (String persLinkField : linkFields) {
                document.addField(persLinkField, refElement);
            }
        }
    }

    @Override
    public void accumulate(SolrInputDocument document, Path filePath, BasicFileAttributes attributes)
        throws IOException {

        if (!filePath.getFileName().toString().endsWith(".xml")) {
            return;
        }

        if (!Files.exists(filePath)) {
            return;
        }

        Document teiDocument;
        try (InputStream is = Files.newInputStream(filePath)) {
            SAXBuilder sb = new SAXBuilder(XMLReaders.NONVALIDATING);
            teiDocument = sb.build(is);
        } catch (JDOMException e) {
            throw new IOException("Error while reading " + filePath + "!", e);
        }

        extractTEIElement(document, "persName",
            "//uri.gbv.de/terminology/lod_persons/", teiDocument,
            DITAV_GENERIC_FILE_LINK_SOLR_FIELD, DITAV_PERSON_FILE_LINK_SOLR_FIELD);

        extractTEIElement(document, "orgName",
            "//uri.gbv.de/terminology/lod_organisations/", teiDocument,
            DITAV_GENERIC_FILE_LINK_SOLR_FIELD, DITAV_ORGANIZATION_FILE_LINK_SOLR_FIELD);

        extractGeoNames(document, teiDocument);
    }

    /**
     * Extracts all {@code tei:placeName} elements referencing geonames.org, ensures the
     * corresponding GeoNames entries are cached locally (fetching them from the GeoNames API on a
     * cache miss) and adds the geonameId, the reference and the resolved place name to the Solr
     * document. All resolved coordinates are combined into a single WKT geometry value (a
     * {@code POINT} for one place, a {@code GEOMETRYCOLLECTION} of points for several) so the
     * single-valued {@code location_common} field type can hold every place of the document.
     * <p>
     * GeoNames resolution is best effort: any failure is logged and must not abort indexing.
     */
    private static void extractGeoNames(SolrInputDocument document, Document teiDocument) {
        XPathExpression<Element> places = XPathFactory.instance().compile(
            ".//tei:placeName[contains(@ref, 'geonames.org')]", Filters.element(), null, TEI_NAMESPACE);

        DitavGeoNamesService service = DitavGeoNamesService.getInstance();
        Set<String> seenIds = new LinkedHashSet<>();
        List<String> coordinatePairs = new ArrayList<>();

        for (Element place : places.evaluate(teiDocument)) {
            String refAttribute = place.getAttributeValue("ref");
            if (refAttribute == null) {
                continue;
            }
            for (String ref : refAttribute.split(",")) {
                String trimmedRef = ref.trim();
                DitavGeoNamesService.extractGeonameId(trimmedRef)
                    .filter(seenIds::add)
                    .ifPresent(geonameId -> indexGeoName(document, service, geonameId, trimmedRef,
                        coordinatePairs));
            }
        }

        buildCoordinateWkt(coordinatePairs)
            .ifPresent(wkt -> document.addField(DITAV_COORDINATES_SOLR_FIELD, wkt));
    }

    private static void indexGeoName(SolrInputDocument document, DitavGeoNamesService service,
        String geonameId, String ref, List<String> coordinatePairs) {
        document.addField(DITAV_GEONAMES_ID_SOLR_FIELD, geonameId);
        document.addField(DITAV_GEONAMES_LINK_SOLR_FIELD, ref);
        try {
            service.resolveAsJson(geonameId).ifPresent(entry -> {
                if (entry.has("name")) {
                    document.addField(DITAV_PLACE_SOLR_FIELD, entry.get("name").getAsString());
                }
                DitavGeoNamesService.coordinatePair(entry).ifPresent(coordinatePairs::add);
            });
        } catch (RuntimeException e) {
            LOGGER.warn("Could not resolve GeoNames entry {}", geonameId, e);
        }
    }

    /**
     * Combines the collected {@code "lng lat"} pairs into a single WKT geometry.
     *
     * @param coordinatePairs the {@code "lng lat"} pairs of all resolved places
     * @return a single {@code POINT}/{@code GEOMETRYCOLLECTION} WKT value or empty if there are none
     */
    private static Optional<String> buildCoordinateWkt(List<String> coordinatePairs) {
        if (coordinatePairs.isEmpty()) {
            return Optional.empty();
        }
        if (coordinatePairs.size() == 1) {
            return Optional.of("POINT (" + coordinatePairs.get(0) + ")");
        }
        String points = coordinatePairs.stream()
            .map(pair -> "POINT (" + pair + ")")
            .collect(Collectors.joining(", "));
        return Optional.of("GEOMETRYCOLLECTION (" + points + ")");
    }

}
