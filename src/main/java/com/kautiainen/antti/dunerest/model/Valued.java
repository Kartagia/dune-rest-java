package com.kautiainen.antti.dunerest.model;

/**
 * An interface representing an object with a value.
 * @param <T> The type of the value.
 */
public interface Valued<T> {
  /**
   * The message indicating the value is immutable.
   */
  String IMMUTABLE_VALUE_MESSAGE = "Value is immutable";
  /**
   * The message of the invalid value.
   */
  String INVALID_VALUE_MESSAGE = "Invalid value";

  /**
   * Get the current value.
   * @return The current value.
   */
  T getValue();

  /**
   * Test validity of the value.
   * @param value The tested value.
   * @return True, if and only if the value is valid.
   */
  boolean validValue(T value);

  /**
   * Set the current value.
   * @param value The new value.
   * @throws UnsupportedOperationException The setting of the value is not supported.
   * @throws IllegalArgumentException The value was not accepted.
   */
  default void setValue(T value)
    throws IllegalArgumentException, UnsupportedOperationException {
    throw new UnsupportedOperationException(IMMUTABLE_VALUE_MESSAGE);
  }
}
