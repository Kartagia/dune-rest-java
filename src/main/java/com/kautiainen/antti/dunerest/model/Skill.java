package com.kautiainen.antti.dunerest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Comparator;

public class Skill
  extends NamedAndValuedObject<Integer>
  implements Bounded<Number> {

  @JsonCreator
  public Skill(
    @JsonProperty("name") String name,
    @JsonProperty("value") Integer value
  ) {
    super(name, value);
  }

  public boolean validValue(Number value) {
    return value == null || isWithin(value);
  }

  @Override
  public boolean validValue(Integer value) {
    return value == null || isWithin(value);
  }

  /**
   * Test validity of an integer value.
   * @param value The tested value.
   * @return True, if and only if the given integer value is a valid value.
   */
  public boolean validValue(int value) {
    return validValue(Integer.valueOf(value));
  }

  /**
   * The comparator used to compare the values.
   */
  @SuppressWarnings("unchecked")
  public static final Comparator<Number> COMPARATOR = (
    Comparator<Number>
    & java.io.Serializable
  ) (Number a, Number b) ->
    (
      a == null
        ? (b == null ? 0 : -1)
        : (b == null ? 1 : b.intValue() - a.intValue())
    );

  @Override
  public Comparator<Number> getComparator() {
    return COMPARATOR;
  }

  @Override
  @JsonIgnore
  public Boundary<Integer> getLowerBoundary() {
    return Boundary.inclusive(4);
  }

  @Override
  @JsonIgnore
  public Boundary<Integer> getUpperBoundary() {
    return Boundary.inclusive(8);
  }
}
