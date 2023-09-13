package com.kautiainen.antti.dunerest.model;

/**
 * An object which has a name.
 */
public interface Named {
  /**
   * Get the name.
   * @return The name.
   */
  String getName();

  /**
   * Set the name.
   * @param name The new name of the named.
   * @throws UnsupportedOperationException The operation is not supported.
   * @throws IllegalArgumentException The given name is not allowed.
   */
  void setName(String name)
    throws UnsupportedOperationException, IllegalArgumentException;
}
