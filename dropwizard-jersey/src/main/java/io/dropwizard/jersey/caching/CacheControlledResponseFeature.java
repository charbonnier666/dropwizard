package io.dropwizard.jersey.caching;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.model.AnnotatedMethod;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Provider
public class CacheControlledResponseFeature implements DynamicFeature {

    private static final String STALE_WHILE_REVALIDATE = "stale-while-revalidate";

    @Override
    public void configure(final ResourceInfo resourceInfo, final FeatureContext configuration) {
        final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());

        // check to see if it has cache control annotation
        final CacheControl cc = am.getAnnotation(CacheControl.class);
        if (cc != null) {
            configuration.register(new CacheControlledResponseFilter(cc));
        }
    }

    private static class CacheControlledResponseFilter implements ContainerResponseFilter {
        private static final int ONE_YEAR_IN_SECONDS = (int) TimeUnit.DAYS.toSeconds(365);
        private String cacheResponseHeader;

        CacheControlledResponseFilter(CacheControl control) {
            final jakarta.ws.rs.core.CacheControl cacheControl = new jakarta.ws.rs.core.CacheControl();
            cacheControl.setPrivate(control.isPrivate());
            cacheControl.setNoCache(control.noCache());
            cacheControl.setNoStore(control.noStore());
            cacheControl.setNoTransform(control.noTransform());
            cacheControl.setMustRevalidate(control.mustRevalidate());
            cacheControl.setProxyRevalidate(control.proxyRevalidate());
            cacheControl.setMaxAge((int) control.maxAgeUnit().toSeconds(control.maxAge()));
            cacheControl.setSMaxAge((int) control.sharedMaxAgeUnit()
                                                 .toSeconds(control.sharedMaxAge()));
            if (control.immutable()) {
                cacheControl.setMaxAge(ONE_YEAR_IN_SECONDS);
            }

            if (control.staleWhileRevalidate() > -1) {
                cacheControl.getCacheExtension().put(
                    STALE_WHILE_REVALIDATE,
                    Long.toString(
                        control.staleWhileRevalidateUnit().toSeconds(control.staleWhileRevalidate())
                    )
                );
            }

            cacheResponseHeader = cacheControl.toString();
        }

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) throws IOException {
            if (!cacheResponseHeader.isEmpty()) {
                responseContext.getHeaders().add(HttpHeaders.CACHE_CONTROL, cacheResponseHeader);
            }

        }

    }
}
