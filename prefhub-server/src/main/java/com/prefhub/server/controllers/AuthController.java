package com.prefhub.server.controllers;

import com.google.inject.Inject;
import com.prefhub.server.auth.AuthService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

/**
 * Authentication controller using JAX-RS.
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {
    private final AuthService authService;

    @Inject
    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    @POST
    @Path("/register")
    public Map<String, String> register(RegisterRequest request) {
        authService.register(request.username(), request.password());
        return Map.of("message", "User registered successfully");
    }

    @POST
    @Path("/login")
    public Map<String, String> login(LoginRequest request) {
        final String token = authService.login(request.username(), request.password());
        return Map.of("token", token);
    }

    @POST
    @Path("/logout")
    public Map<String, String> logout(@HeaderParam("Authorization") String authHeader) {
        // Token validation would be done by a filter
        return Map.of("message", "Logout successful");
    }

    // Request DTOs
    public record RegisterRequest(String username, String password) {}
    public record LoginRequest(String username, String password) {}
}
