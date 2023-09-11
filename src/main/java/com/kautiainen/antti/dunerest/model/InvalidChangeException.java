package com.kautiainen.antti.dunerest.model;

/**
 * Exception representing that the change of a value is invalid.
 */
public class InvalidChangeException extends IllegalArgumentException {

  /**
   * Create an invalid change exception with a message.
   * @param message The message of the exception.
   */
  public InvalidChangeException(String message) {
    super(message);
  }

  /**
   * Create an ivnalid change exception with a message and a cause.
   * @param message The message of the exception.
   * @param cause The cause of the exception.
   */
  public InvalidChangeException(String message, Throwable cause) {
    super(message, cause);
  }
}
