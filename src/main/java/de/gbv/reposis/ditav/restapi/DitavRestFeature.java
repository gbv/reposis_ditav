package de.gbv.reposis.ditav.restapi;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.jersey.feature.MCRJerseyDefaultFeature;
import org.mycore.restapi.MCREnableTransactionFilter;
import org.mycore.restapi.annotations.MCRRequireTransaction;

import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

/**
 * Jersey configuration for the DITAV API, modelled after the CMS module's feature.
 *
 * @see MCRJerseyDefaultFeature
 */
@Provider
public class DitavRestFeature extends MCRJerseyDefaultFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        Method resourceMethod = resourceInfo.getResourceMethod();
        if (requiresTransaction(resourceClass, resourceMethod)) {
            context.register(MCREnableTransactionFilter.class);
        }
        super.configure(resourceInfo, context);
    }

    protected boolean requiresTransaction(Class<?> resourceClass, Method resourceMethod) {
        return resourceClass.getAnnotation(MCRRequireTransaction.class) != null
            || resourceMethod.getAnnotation(MCRRequireTransaction.class) != null;
    }

    @Override
    protected List<String> getPackages() {
        return MCRConfiguration2.getString(DitavRestApp.RESOURCE_PACKAGES_PROPERTY)
            .map(MCRConfiguration2::splitValue)
            .orElse(Stream.empty())
            .collect(Collectors.toList());
    }

    @Override
    protected void registerSessionHookFilter(FeatureContext context) {
        // transaction handling is already implemented by MCRSessionFilter
    }

    @Override
    protected void registerTransactionFilter(FeatureContext context) {
        // transaction handling is already implemented by MCRSessionFilter
    }

    @Override
    protected void registerAccessFilter(FeatureContext context, Class<?> resourceClass, Method resourceMethod) {
        super.registerAccessFilter(context, resourceClass, resourceMethod);
    }
}
