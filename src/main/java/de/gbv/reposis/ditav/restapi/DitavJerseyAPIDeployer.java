package de.gbv.reposis.ditav.restapi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.servlet.ServletContainer;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;

/**
 * Deploys the DITAV Jersey REST API programmatically at runtime.
 * <p>
 * This is necessary because the module JAR is added to the classpath at runtime, after the servlet
 * container has already started - the same approach the CMS module uses for its API.
 */
public class DitavJerseyAPIDeployer implements AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String SERVLET_NAME = "DitavRestAPI";

    private static final String URL_PATTERN = "/api/geonames/*";

    @Override
    public String getName() {
        return "DitavJerseyAPIDeployer";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext == null) {
            LOGGER.warn("ServletContext is null, cannot deploy DITAV REST API");
            return;
        }

        if (servletContext.getServletRegistration(SERVLET_NAME) != null) {
            LOGGER.info("DITAV REST API servlet already registered");
            return;
        }

        try {
            ServletRegistration.Dynamic servlet = servletContext.addServlet(
                SERVLET_NAME,
                new ServletContainer(new DitavRestApp()));

            if (servlet == null) {
                LOGGER.error("Failed to register DITAV REST API servlet - addServlet returned null");
                return;
            }

            servlet.addMapping(URL_PATTERN);
            servlet.setLoadOnStartup(1);
            servlet.setAsyncSupported(true);

            LOGGER.info("Successfully deployed DITAV REST API at {}", URL_PATTERN);
        } catch (Exception e) {
            LOGGER.error("Failed to deploy DITAV REST API", e);
        }
    }
}
