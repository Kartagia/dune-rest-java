package com.kautiainen.antti.dunerest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An object with both name and level.
 * @param <T> The type of the level.
 */
public class NamedAndValuedObject<T> implements Named, Valued<T> {

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

  /**
   * Create a new named and valued object from an another named and valued object.
   * @param source The source.
   */
  public NamedAndValuedObject(NamedAndValuedObject<? extends T> source) {
    if (source == null) {
      throw new IllegalArgumentException(
        "Invalid source",
        new NullPointerException("Undefined value")
      );
    }
    setName(source.getName());
    setValue(source.getValue());
  }

  /**
   * Create a new named and valued object from a name and value.
   * @param name The name of the created named and valued object.
   * @param value The value of the created named and valued object.
   * @throws IllegalArgumentException Either the name or the value
   * was not acceptable.
   */
  @JsonCreator
  public NamedAndValuedObject(
    @JsonProperty("name") String name,
    @JsonProperty("value") T value
  ) throws IllegalArgumentException {
    setName(name);
    setValue(value);
  }
}
