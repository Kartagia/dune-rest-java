package com.kautiainen.antti.dunerest.model;

/**
 * A factory producing leveled and valued objects.
 */
public class NamedAndValuedFactory<TYPE> {

  /**
   * The default value of the created leveld valued factory object.
   */
  private TYPE defaultValue = null;

  /**
   * Create a new leveled and valued factory without default value.
   */
  public NamedAndValuedFactory() {}

  /**
   * Create a new leveled and valued factory with a default value.
   * @param defaultValue The default value of the created objects.
   */
  public NamedAndValuedFactory(TYPE defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * Get the default value of the factory.
   * @return The default value of the factory.
   */
  public synchronized TYPE defaultValue() {
    return this.defaultValue;
  }

  /**
   * Create a new instance of leveld and valued objects.
   * @param name The name of the created named and valued object.
   * @return The created named and valued object.
   * @throws IllegalArgumentException the creation was not possible.
   */
  public NamedAndValuedObject<TYPE> createImmutable(String name)
    throws IllegalArgumentException {
    return createImmutable(name, defaultValue());
  }

  /**
   * Create a new instance of leveld and valued objects.
   * @param name The name of the created named and valued object.
   * @param value The value of the created object.
   * @return The created named and valued object.
   * @throws IllegalArgumentException the creation was not possible.
   */
  public NamedAndValuedObject<TYPE> createImmutable(String name, TYPE value)
    throws IllegalArgumentException {
    return new NamedAndValuedObject<>(name, value) {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public TYPE getValue() {
        return value;
      }
    };
  }
}
