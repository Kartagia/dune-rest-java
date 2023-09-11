package com.kautiainen.antti.dunerest.model;

/**
 * An object with both name and level.
 * @param <T> The type of the level.
 */
public class NamedAndValuedObject<T> implements Named<T>, Valued<T> {

  /**
   * The name of the motivation.
   */
  private String name;

  /**
   * The current value of the motivation.
   */
  private T value;

  /**
   * Get the name of the motivation.
   * @return The name of the motivation.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Set the name of the motivation.
   * @param name The new name of the motivation.
   */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the current alue of the motivation.
   * @return The current value of the motivation.
   */
  @Override
  public T getValue() {
    return value;
  }

  /**
   * Test validity of the value.
   * @param value The tested value.
   * @return True, if and only if the value is valid.
   */
  @Override
  public boolean validValue(T value) {
    return true;
  }

  /**
   * Set the current value of the motivation.
   * @param value The new value of the motivation.
   * @throws IllegalArgumentException The motivaiton value was invalid.
   */
  @Override
  public void setValue(T value) throws IllegalArgumentException {
    if (validValue(value)) {
      this.value = value;
    } else {
      throw new IllegalArgumentException(INVALID_VALUE_MESSAGE);
    }
  }

  public NamedAndValuedObject(String name, T value)
    throws IllegalArgumentException {
    setName(name);
    setValue(value);
  }
}
