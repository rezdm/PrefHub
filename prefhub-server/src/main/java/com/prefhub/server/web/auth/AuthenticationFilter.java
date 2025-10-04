package com.prefhub.server.web.auth;

import com.google.inject.Inject;
import com.prefhub.server.auth.AuthService;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS filter for authenticating requests and injecting username.
 */
@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    private final AuthService authService;

    @Inject
    public AuthenticationFilter(final AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        final String path = requestContext.getUriInfo().getPath();

        // Skip authentication for auth endpoints
        if (path.startsWith("auth/")) {
            return;
        }

        // Check for Authorization header
        final String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("{\"error\":\"Missing or invalid Authorization header\"}")
                            .build()
            );
            return;
        }

        // Validate token
        final String token = authHeader.substring(7);
        final String username = authService.validateToken(token);
        if (username == null) {
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("{\"error\":\"Invalid token\"}")
                            .build()
            );
            return;
        }

        // Store username in request context for parameter injection
        requestContext.setProperty("username", username);
    }
}
