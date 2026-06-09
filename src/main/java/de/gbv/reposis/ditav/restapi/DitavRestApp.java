package de.gbv.reposis.ditav.restapi;

import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.jersey.access.MCRRequestScopeACLFilter;
import org.mycore.restapi.MCRCORSResponseFilter;
import org.mycore.restapi.MCRIgnoreClientAbortInterceptor;
import org.mycore.restapi.MCRSessionFilter;
import org.mycore.restapi.MCRTransactionFilter;

/**
 * Jersey REST application for the DITAV API.
 * <p>
 * It is registered programmatically by {@link DitavJerseyAPIDeployer} because the module JAR is
 * loaded at runtime, after the servlet container has already started (the same reason the CMS
 * module deploys its API programmatically).
 */
public class DitavRestApp extends ResourceConfig {

    public static final String RESOURCE_PACKAGES_PROPERTY = "MCR.DITAV.API.Resource.Packages";

    public DitavRestApp() {
        super();
        initAppName();
        property(ServerProperties.APPLICATION_NAME, getApplicationName());
        packages(getRestPackages());
        property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true);
        register(MCRSessionFilter.class);
        register(MCRTransactionFilter.class);
        register(DitavRestFeature.class);
        register(MCRCORSResponseFilter.class);
        register(MCRRequestScopeACLFilter.class);
        register(MCRIgnoreClientAbortInterceptor.class);
    }

    protected void initAppName() {
        setApplicationName("DITAV-API " + getVersion());
        LogManager.getLogger().info("Initialize {}", this::getApplicationName);
    }

    protected String getVersion() {
        return "1.0";
    }

    protected String[] getRestPackages() {
        return MCRConfiguration2.getOrThrow(RESOURCE_PACKAGES_PROPERTY, MCRConfiguration2::splitValue)
            .toArray(String[]::new);
    }
}
