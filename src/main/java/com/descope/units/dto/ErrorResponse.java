package com.descope.units.dto;

import java.time.Instant;

/**
 * Response DTO for error responses.
 *
 * <p>This DTO provides structured error information to API clients.
 */
public class ErrorResponse {

  private String message;
  private int status;
  private String timestamp;

  /** Default constructor for JSON serialization. */
  public ErrorResponse() {
    this.timestamp = Instant.now().toString();
  }

  /**
   * Constructs an ErrorResponse with the specified message and status.
   *
   * @param message the error message
   * @param status the HTTP status code
   */
  public ErrorResponse(String message, int status) {
    this.message = message;
    this.status = status;
    this.timestamp = Instant.now().toString();
  }

  /**
   * Returns the error message.
   *
   * @return the error message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the error message.
   *
   * @param message the error message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Returns the HTTP status code.
   *
   * @return the status code
   */
  public int getStatus() {
    return status;
  }

  /**
   * Sets the HTTP status code.
   *
   * @param status the status code
   */
  public void setStatus(int status) {
    this.status = status;
  }

  /**
   * Returns the error timestamp.
   *
   * @return the timestamp
   */
  public String getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the error timestamp.
   *
   * @param timestamp the timestamp
   */
  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }
}
