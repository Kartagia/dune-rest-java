package com.kautiainen.antti.dunerest.model;

/**
 * A definition of a motivation.
 */
public class MotivationDefinition implements Named {

  private String name;

  private String description = null;

  /**
   * Get the descripotion of the
   * @return
   */
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public MotivationDefinition() {}

  public MotivationDefinition(String name, String description) {
    setName(name);
    setDescription(description);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void setName(String name)
    throws UnsupportedOperationException, IllegalArgumentException {
    if (validName(name)) {
      this.name = name;
    } else {
      throw new IllegalArgumentException("Invalid name");
    }
  }

  /**
   * Test validity of the name.
   * @param name The tested name.
   * @return True, if and only if the name is valid.
   */
  public boolean validName(String name) {
    return name != null;
  }
}
