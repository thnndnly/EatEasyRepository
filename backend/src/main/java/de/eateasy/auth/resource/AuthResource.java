package de.eateasy.auth.resource;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.GoogleLoginRequest;
import de.eateasy.auth.dto.LoginRequest;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.dto.UserDto;
import de.eateasy.auth.service.AuthService;
import de.eateasy.common.exception.NotFoundException;
import de.eateasy.common.security.CurrentUser;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService;
    private final CurrentUser currentUser;
    private final boolean googleEnabled;

    public AuthResource(AuthService authService,
                        CurrentUser currentUser,
                        @ConfigProperty(name = "eateasy.google-oauth.enabled", defaultValue = "false")
                        boolean googleEnabled) {
        this.authService = authService;
        this.currentUser = currentUser;
        this.googleEnabled = googleEnabled;
    }

    @POST
    @Path("/register")
    @PermitAll
    public Response register(@Valid RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    public AuthResponse login(@Valid LoginRequest request) {
        return authService.login(request);
    }

    @POST
    @Path("/google")
    @PermitAll
    public AuthResponse google(@Valid GoogleLoginRequest request) {
        // Feature-Flag: ist Google-OAuth aus (Default), verhält sich der
        // Endpoint, als gäbe es ihn nicht (404) — analog zum Beleg-Scanner.
        if (!googleEnabled) {
            throw new NotFoundException("Google-Login ist nicht aktiviert");
        }
        return authService.loginWithGoogle(request.idToken());
    }

    @GET
    @Path("/me")
    @RolesAllowed("user")
    public UserDto me() {
        return authService.getUser(currentUser.id());
    }
}
