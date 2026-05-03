package de.eateasy.auth.resource;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.LoginRequest;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.dto.UserDto;
import de.eateasy.auth.service.AuthService;
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

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthResource(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
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

    @GET
    @Path("/me")
    @RolesAllowed("user")
    public UserDto me() {
        return authService.getCurrentUser(currentUser.id());
    }
}
