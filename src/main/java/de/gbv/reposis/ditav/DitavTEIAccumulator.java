package de.gbv.reposis.ditav;

import static de.gbv.reposis.ditav.DitavConstants.TEI_NAMESPACE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import java.util.stream.Stream;
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

public class DitavTEIAccumulator implements MCRSolrFileIndexAccumulator {

    public static final String DITAV_GENERIC_FILE_LINK_SOLR_FIELD = "ditav.mods.dante_file_link";
    public static final String DITAV_PERSON_FILE_LINK_SOLR_FIELD = "ditav.mods.dante_file_pers_link";
    public static final String DITAV_ORGANIZATION_FILE_LINK_SOLR_FIELD = "ditav.mods.dante_file_org_link";

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
            "https://uri.gbv.de/terminology/lod_persons/", teiDocument,
            DITAV_GENERIC_FILE_LINK_SOLR_FIELD, DITAV_PERSON_FILE_LINK_SOLR_FIELD);

        extractTEIElement(document, "orgName",
            "https://uri.gbv.de/terminology/lod_organisations/", teiDocument,
            DITAV_GENERIC_FILE_LINK_SOLR_FIELD, DITAV_ORGANIZATION_FILE_LINK_SOLR_FIELD);
    }

}
