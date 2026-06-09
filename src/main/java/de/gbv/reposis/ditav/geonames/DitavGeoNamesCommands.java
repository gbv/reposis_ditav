package de.gbv.reposis.ditav.geonames;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;

/**
 * Command line commands to manage the local GeoNames cache.
 */
@MCRCommandGroup(name = "DITAV GeoNames Commands")
public class DitavGeoNamesCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    @MCRCommand(syntax = "clear geonames cache",
        help = "Removes all locally cached GeoNames entries.")
    public static void clearAll() {
        int count = DitavGeoNamesService.getInstance().clearAll();
        LOGGER.info("Cleared {} GeoNames cache entries.", count);
    }

    @MCRCommand(syntax = "clear geonames cache for {0}",
        help = "Removes the locally cached GeoNames entry for geonameId {0}.")
    public static void clear(String geonameId) {
        boolean deleted = DitavGeoNamesService.getInstance().clear(geonameId);
        if (deleted) {
            LOGGER.info("Cleared GeoNames cache entry {}.", geonameId);
        } else {
            LOGGER.info("No GeoNames cache entry found for {}.", geonameId);
        }
    }
}
