package de.gbv.reposis.ditav;

import static de.gbv.reposis.ditav.DitavTEIAccumulator.DITAV_GENERIC_FILE_LINK_SOLR_FIELD;
import static de.gbv.reposis.ditav.DitavTEIAccumulator.DITAV_ORGANIZATION_FILE_LINK_SOLR_FIELD;
import static de.gbv.reposis.ditav.DitavTEIAccumulator.DITAV_PERSON_FILE_LINK_SOLR_FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRClassTools;
import org.mycore.test.MyCoReTest;

@MyCoReTest
class DitavTEIAccumulatorTest {

    private static final String PERS_PREFIX = "https://uri.gbv.de/terminology/lod_persons/";

    private static final String ORG_PREFIX = "https://uri.gbv.de/terminology/lod_organisations/";

    private static final Set<String> EXPECTED_PERSONS = Set.of(
        PERS_PREFIX + "d52c788b-74d5-4c5e-aa9a-22125975c436",
        PERS_PREFIX + "ec80072a-6751-40ca-8d4e-d0c0d7e87801",
        PERS_PREFIX + "94528133-e16c-4760-877c-51f2cf342f30",
        PERS_PREFIX + "b3a043f4-ae9a-40f7-93a5-6aac6dd21389",
        PERS_PREFIX + "a909b6ae-3823-4764-ab8f-fd7a135760f1",
        PERS_PREFIX + "bc444245-d428-47f5-8e28-07bd88e25987",
        PERS_PREFIX + "fe0fba0a-6d68-4106-8925-02582562d0f0");

    private static final Set<String> EXPECTED_ORGS = Set.of(
        ORG_PREFIX + "7489cfb2-d9d1-427e-8eeb-7a2cd3f35d11",
        ORG_PREFIX + "dd406ffb-30ce-4374-868b-1a55e8daf251");

    @Test
    void accumulate() throws IOException {
        Path file;
        SolrInputDocument solrDocument = new SolrInputDocument();
        try (InputStream is = MCRClassTools.getClassLoader()
            .getResourceAsStream("files/accumulator_test_file.xml")) {
            file = Files.createTempFile("acc", "test_file.xml");
            Files.copy(is, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        DitavTEIAccumulator ditavTEIAccumulator = new DitavTEIAccumulator();
        ditavTEIAccumulator.accumulate(solrDocument, file, null);

        Set<String> persons = asStringSet(solrDocument.getFieldValues(DITAV_PERSON_FILE_LINK_SOLR_FIELD));
        Set<String> orgs = asStringSet(solrDocument.getFieldValues(DITAV_ORGANIZATION_FILE_LINK_SOLR_FIELD));
        Set<String> generic = asStringSet(solrDocument.getFieldValues(DITAV_GENERIC_FILE_LINK_SOLR_FIELD));

        assertEquals(EXPECTED_PERSONS, persons, "person refs (deduped)");
        assertEquals(EXPECTED_ORGS, orgs, "org refs (deduped)");

        Set<String> expectedGeneric = new java.util.HashSet<>(EXPECTED_PERSONS);
        expectedGeneric.addAll(EXPECTED_ORGS);
        assertEquals(expectedGeneric, generic, "generic field contains persons + orgs");

        List<Object> personValues = List.copyOf(solrDocument.getFieldValues(DITAV_PERSON_FILE_LINK_SOLR_FIELD));
        assertEquals(EXPECTED_PERSONS.size(), personValues.size(),
            "no duplicate values in person field");

        Files.deleteIfExists(file);
    }

    @Test
    void skipsNonXmlFiles() throws IOException {
        SolrInputDocument doc = new SolrInputDocument();
        Path nonXml = Files.createTempFile("acc", "test_file.txt");
        try {
            Files.writeString(nonXml, "not xml");
            new DitavTEIAccumulator().accumulate(doc, nonXml, null);
            assertTrue(doc.getFieldNames().isEmpty(), "non-xml file must not add fields");
        } finally {
            Files.deleteIfExists(nonXml);
        }
    }

    private static Set<String> asStringSet(Collection<Object> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream().map(Object::toString).collect(java.util.stream.Collectors.toSet());
    }
}
