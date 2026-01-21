package com.descope.units.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.descope.units.dto.ErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global exception handler for REST API exceptions.
 *
 * <p>This handler catches exceptions thrown by the application and converts them to appropriate
 * HTTP responses with structured error information.
 */
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Converts exceptions to HTTP responses.
   *
   * @param exception the exception to handle
   * @return the HTTP response
   */
  @Override
  public Response toResponse(Exception exception) {
    if (exception instanceof UnitNotFoundException) {
      return handleUnitNotFoundException((UnitNotFoundException) exception);
    } else if (exception instanceof ConstraintViolationException) {
      return handleConstraintViolationException((ConstraintViolationException) exception);
    } else if (exception instanceof IllegalArgumentException) {
      return handleIllegalArgumentException((IllegalArgumentException) exception);
    } else {
      return handleGenericException(exception);
    }
  }

  private Response handleUnitNotFoundException(UnitNotFoundException exception) {
    logger.warn("Unit not found: {}", exception.getUnitId());
    ErrorResponse error =
        new ErrorResponse(exception.getMessage(), Response.Status.NOT_FOUND.getStatusCode());
    return Response.status(Response.Status.NOT_FOUND).entity(error).build();
  }

  private Response handleConstraintViolationException(ConstraintViolationException exception) {
    logger.warn("Validation error: {}", exception.getMessage());
    String message = extractValidationMessage(exception);
    ErrorResponse error = new ErrorResponse(message, Response.Status.BAD_REQUEST.getStatusCode());
    return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
  }

  private Response handleIllegalArgumentException(IllegalArgumentException exception) {
    logger.warn("Illegal argument: {}", exception.getMessage());
    ErrorResponse error =
        new ErrorResponse(exception.getMessage(), Response.Status.BAD_REQUEST.getStatusCode());
    return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
  }

  private Response handleGenericException(Exception exception) {
    logger.error("Unexpected error occurred", exception);
    ErrorResponse error =
        new ErrorResponse(
            "An unexpected error occurred. Please try again later.",
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
  }

  private String extractValidationMessage(ConstraintViolationException exception) {
    return exception.getConstraintViolations().stream()
        .map(violation -> violation.getMessage())
        .findFirst()
        .orElse("Validation error");
  }
}
