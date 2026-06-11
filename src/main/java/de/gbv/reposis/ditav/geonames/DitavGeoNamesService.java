package de.gbv.reposis.ditav.geonames;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Resolves GeoNames entries for a given geonameId and caches the raw GeoNames JSON
 * response locally below the MyCoRe data directory.
 * <p>
 * A lookup first checks the local cache. On a cache miss the entry is fetched from the
 * GeoNames API (only if a username is configured) and stored in the cache afterwards.
 * <p>
 * Configuration:
 * <ul>
 *   <li>{@code MCR.DITAV.GeoNames.Username} - GeoNames API username (required for remote lookups)</li>
 *   <li>{@code MCR.DITAV.GeoNames.BaseURL} - GeoNames JSON endpoint, defaults to {@value #DEFAULT_BASE_URL}</li>
 *   <li>{@code MCR.DITAV.GeoNames.CacheDir} - optional cache directory, defaults to
 *       {@code <MCR.datadir>/geonames-cache}</li>
 * </ul>
 */
public final class DitavGeoNamesService {

    public static final String USERNAME_PROPERTY = "MCR.DITAV.GeoNames.Username";

    public static final String BASE_URL_PROPERTY = "MCR.DITAV.GeoNames.BaseURL";

    public static final String CACHE_DIR_PROPERTY = "MCR.DITAV.GeoNames.CacheDir";

    public static final String DEFAULT_BASE_URL = "https://www.geonames.org/getJSON";

    private static final String CACHE_DIR_NAME = "geonames-cache";

    private static final Logger LOGGER = LogManager.getLogger();

    /** A valid geonameId consists of digits only. */
    private static final Pattern GEONAME_ID_PATTERN = Pattern.compile("\\d+");

    /** Matches the numeric geonameId in refs like {@code https://(www.|sws.)geonames.org/554234}. */
    private static final Pattern REF_ID_PATTERN = Pattern.compile("geonames\\.org/(\\d+)");

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    /**
     * One lock per geonameId, so concurrent fetches of <em>different</em> ids run in parallel
     * while concurrent fetches of the <em>same</em> id are serialized and hit the cache after the
     * first one stored it.
     */
    private final KeyedLocks fetchLocks = new KeyedLocks();

    private DitavGeoNamesService() {
    }

    public static DitavGeoNamesService getInstance() {
        return LazyInstanceHolder.INSTANCE;
    }

    /**
     * Extracts the numeric geonameId from a GeoNames reference URL.
     *
     * @param ref a reference like {@code https://www.geonames.org/554234}
     * @return the geonameId or an empty optional if the ref does not contain one
     */
    public static Optional<String> extractGeonameId(String ref) {
        if (ref == null) {
            return Optional.empty();
        }
        Matcher matcher = REF_ID_PATTERN.matcher(ref);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /**
     * Extracts the {@code "lng lat"} coordinate pair (WKT axis order) from a GeoNames JSON entry.
     *
     * @param entry the parsed GeoNames JSON
     * @return the coordinate pair or an empty optional if no usable coordinates are present
     */
    public static Optional<String> coordinatePair(JsonObject entry) {
        if (entry == null || !entry.has("lat") || !entry.has("lng")) {
            return Optional.empty();
        }
        try {
            double lat = entry.get("lat").getAsDouble();
            double lng = entry.get("lng").getAsDouble();
            return Optional.of(lng + " " + lat);
        } catch (NumberFormatException | IllegalStateException e) {
            LOGGER.warn("GeoNames entry has invalid lat/lng: {}", entry);
            return Optional.empty();
        }
    }

    /**
     * Builds a WKT {@code POINT (lng lat)} string from a GeoNames JSON entry, matching the
     * coordinate format used for the {@code location_common} Solr field type in reposis_common.
     *
     * @param entry the parsed GeoNames JSON
     * @return the WKT point or an empty optional if no usable coordinates are present
     */
    public static Optional<String> toWktPoint(JsonObject entry) {
        return coordinatePair(entry).map(pair -> "POINT (" + pair + ")");
    }

    /**
     * @return the directory holding the cached GeoNames JSON files
     */
    public Path getCacheDirectory() {
        Optional<String> configured = MCRConfiguration2.getString(CACHE_DIR_PROPERTY).filter(s -> !s.isBlank());
        if (configured.isPresent()) {
            return Paths.get(configured.get());
        }
        return Paths.get(MCRConfiguration2.getStringOrThrow("MCR.datadir"), CACHE_DIR_NAME);
    }

    /**
     * Resolves the raw GeoNames JSON for the given geonameId, using the local cache first and
     * fetching from the GeoNames API on a cache miss.
     *
     * @param geonameId the numeric geonameId
     * @return the raw JSON or an empty optional if it could not be resolved
     */
    public Optional<String> resolve(String geonameId) {
        Optional<String> cached = getCached(geonameId);
        if (cached.isPresent()) {
            return cached;
        }
        return fetchAndStore(geonameId);
    }

    /**
     * Like {@link #resolve(String)} but returns the parsed JSON object.
     */
    public Optional<JsonObject> resolveAsJson(String geonameId) {
        return resolve(geonameId).flatMap(DitavGeoNamesService::parse);
    }

    /**
     * @return the cached raw JSON for the given geonameId without contacting the GeoNames API
     */
    public Optional<String> getCached(String geonameId) {
        Path file = cacheFile(geonameId);
        if (Files.isRegularFile(file)) {
            try {
                return Optional.of(Files.readString(file, UTF_8));
            } catch (IOException e) {
                LOGGER.warn("Could not read GeoNames cache file {}", file, e);
            }
        }
        return Optional.empty();
    }

    /**
     * Removes the cache entry for a single geonameId.
     *
     * @return {@code true} if a cache file existed and was deleted
     */
    public boolean clear(String geonameId) {
        Path file = cacheFile(geonameId);
        try {
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                LOGGER.info("Removed GeoNames cache entry {}", geonameId);
            }
            return deleted;
        } catch (IOException e) {
            LOGGER.warn("Could not delete GeoNames cache file {}", file, e);
            return false;
        }
    }

    /**
     * Removes all cached GeoNames entries.
     *
     * @return the number of deleted cache files
     */
    public int clearAll() {
        Path dir = getCacheDirectory();
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        List<Path> cacheFiles;
        try (Stream<Path> files = Files.list(dir)) {
            cacheFiles = files.filter(p -> p.getFileName().toString().endsWith(".json")).toList();
        } catch (IOException e) {
            LOGGER.warn("Could not list GeoNames cache directory {}", dir, e);
            return 0;
        }
        int count = 0;
        for (Path file : cacheFiles) {
            try {
                if (Files.deleteIfExists(file)) {
                    count++;
                }
            } catch (IOException e) {
                LOGGER.warn("Could not delete GeoNames cache file {}", file, e);
            }
        }
        LOGGER.info("Removed {} GeoNames cache entries", count);
        return count;
    }

    private Path cacheFile(String geonameId) {
        if (geonameId == null || !GEONAME_ID_PATTERN.matcher(geonameId).matches()) {
            throw new MCRException("Invalid geonameId: " + geonameId);
        }
        return getCacheDirectory().resolve(geonameId + ".json");
    }

    private Optional<String> fetchAndStore(String geonameId) {
        // lock per geonameId: different ids fetch in parallel, the same id is fetched only once
        try (KeyedLocks.LockHandle handle = fetchLocks.acquire(geonameId)) {
            return doFetchAndStore(geonameId);
        }
    }

    private Optional<String> doFetchAndStore(String geonameId) {
        // re-check the cache: another thread may have fetched it while we waited for the lock
        Optional<String> cached = getCached(geonameId);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<String> username = MCRConfiguration2.getString(USERNAME_PROPERTY).filter(s -> !s.isBlank());
        if (username.isEmpty()) {
            LOGGER.warn("No GeoNames username configured ({}); skipping remote lookup for {}",
                USERNAME_PROPERTY, geonameId);
            return Optional.empty();
        }

        String baseUrl = MCRConfiguration2.getString(BASE_URL_PROPERTY).filter(s -> !s.isBlank())
            .orElse(DEFAULT_BASE_URL);
        String uri = baseUrl + "?geonameId=" + URLEncoder.encode(geonameId, UTF_8)
            + "&username=" + URLEncoder.encode(username.get(), UTF_8);

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
            if (response.statusCode() != 200) {
                LOGGER.warn("GeoNames API returned status {} for geonameId {}", response.statusCode(), geonameId);
                return Optional.empty();
            }
            String body = response.body();
            Optional<JsonObject> json = parse(body);
            if (json.isEmpty() || json.get().has("status")) {
                // GeoNames signals errors (e.g. quota exceeded) with a "status" object
                LOGGER.warn("GeoNames API error for geonameId {}: {}", geonameId, body);
                return Optional.empty();
            }
            store(geonameId, body);
            return Optional.of(body);
        } catch (IOException e) {
            LOGGER.warn("Could not fetch GeoNames entry {} from {}", geonameId, baseUrl, e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while fetching GeoNames entry {}", geonameId, e);
            return Optional.empty();
        }
    }

    private void store(String geonameId, String json) {
        Path file = cacheFile(geonameId);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json, UTF_8);
            LOGGER.info("Cached GeoNames entry {} at {}", geonameId, file);
        } catch (IOException e) {
            LOGGER.warn("Could not write GeoNames cache file {}", file, e);
        }
    }

    private static Optional<JsonObject> parse(String json) {
        try {
            JsonElement element = JsonParser.parseString(json);
            if (element.isJsonObject()) {
                return Optional.of(element.getAsJsonObject());
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Could not parse GeoNames JSON: {}", json, e);
        }
        return Optional.empty();
    }

    private static final class LazyInstanceHolder {
        private static final DitavGeoNamesService INSTANCE = new DitavGeoNamesService();
    }
}
