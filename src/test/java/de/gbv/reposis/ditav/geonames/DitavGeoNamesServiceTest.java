package de.gbv.reposis.ditav.geonames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.test.MyCoReTest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@MyCoReTest
// no username -> no remote lookups, the tests rely solely on the local cache
@MCRTestConfiguration(properties = @MCRTestProperty(key = "MCR.DITAV.GeoNames.Username", empty = true))
class DitavGeoNamesServiceTest {

    @Test
    void extractGeonameId() {
        assertEquals(Optional.of("554234"),
            DitavGeoNamesService.extractGeonameId("https://www.geonames.org/554234"));
        assertEquals(Optional.of("554234"),
            DitavGeoNamesService.extractGeonameId("https://sws.geonames.org/554234"));
        assertEquals(Optional.of("554234"),
            DitavGeoNamesService.extractGeonameId("https://geonames.org/554234/"));
        assertTrue(DitavGeoNamesService.extractGeonameId("https://example.org/foo").isEmpty());
        assertTrue(DitavGeoNamesService.extractGeonameId(null).isEmpty());
    }

    @Test
    void toWktPoint() {
        JsonObject entry = JsonParser.parseString("{\"name\":\"Königsberg\",\"lat\":54.71,\"lng\":20.5}")
            .getAsJsonObject();
        assertEquals(Optional.of("20.5 54.71"), DitavGeoNamesService.coordinatePair(entry));
        assertEquals(Optional.of("POINT (20.5 54.71)"), DitavGeoNamesService.toWktPoint(entry));

        JsonObject noCoords = JsonParser.parseString("{\"name\":\"Nowhere\"}").getAsJsonObject();
        assertTrue(DitavGeoNamesService.coordinatePair(noCoords).isEmpty());
        assertTrue(DitavGeoNamesService.toWktPoint(noCoords).isEmpty());
    }

    @Test
    void resolveFromCache(@TempDir Path cacheDir) throws IOException {
        MCRConfiguration2.set(DitavGeoNamesService.CACHE_DIR_PROPERTY, cacheDir.toString());
        String json = "{\"geonameId\":554234,\"name\":\"Kaliningrad\",\"lat\":54.71,\"lng\":20.5}";
        Files.writeString(cacheDir.resolve("554234.json"), json);

        DitavGeoNamesService service = DitavGeoNamesService.getInstance();
        Optional<String> resolved = service.resolve("554234");
        assertTrue(resolved.isPresent());
        assertEquals(json, resolved.get());

        Optional<JsonObject> parsed = service.resolveAsJson("554234");
        assertTrue(parsed.isPresent());
        assertEquals("Kaliningrad", parsed.get().get("name").getAsString());
    }

    @Test
    void resolveMissWithoutUsername(@TempDir Path cacheDir) {
        MCRConfiguration2.set(DitavGeoNamesService.CACHE_DIR_PROPERTY, cacheDir.toString());
        // cache miss and no username -> empty result, no remote call, no exception
        assertTrue(DitavGeoNamesService.getInstance().resolve("999999").isEmpty());
    }

    @Test
    void clearSingleEntry(@TempDir Path cacheDir) throws IOException {
        MCRConfiguration2.set(DitavGeoNamesService.CACHE_DIR_PROPERTY, cacheDir.toString());
        Files.writeString(cacheDir.resolve("554234.json"), "{}");

        DitavGeoNamesService service = DitavGeoNamesService.getInstance();
        assertTrue(service.getCached("554234").isPresent());
        assertTrue(service.clear("554234"));
        assertTrue(service.getCached("554234").isEmpty());
        assertFalse(service.clear("554234"));
    }

    @Test
    void clearAllEntries(@TempDir Path cacheDir) throws IOException {
        MCRConfiguration2.set(DitavGeoNamesService.CACHE_DIR_PROPERTY, cacheDir.toString());
        Files.writeString(cacheDir.resolve("1.json"), "{}");
        Files.writeString(cacheDir.resolve("2.json"), "{}");
        Files.writeString(cacheDir.resolve("note.txt"), "ignore");

        DitavGeoNamesService service = DitavGeoNamesService.getInstance();
        assertEquals(2, service.clearAll());
        assertTrue(Files.exists(cacheDir.resolve("note.txt")), "non-json files must be kept");
    }
}
