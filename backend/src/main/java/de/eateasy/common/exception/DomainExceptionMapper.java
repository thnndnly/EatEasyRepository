package de.eateasy.common.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    @Override
    public Response toResponse(DomainException exception) {
        Response.Status status = switch (exception) {
            case EmailAlreadyExistsException ignored -> Response.Status.CONFLICT;
            case InvalidCredentialsException ignored -> Response.Status.UNAUTHORIZED;
            case NotFoundException ignored -> Response.Status.NOT_FOUND;
            default -> Response.Status.BAD_REQUEST;
        };
        return Response.status(status)
            .entity(Map.of("error", exception.getMessage()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
