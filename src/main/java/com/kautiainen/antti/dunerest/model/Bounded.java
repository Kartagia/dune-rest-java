package com.kautiainen.antti.dunerest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Comparator;

/**
 * An interface representing a bounded values.
 */
public interface Bounded<TYPE> {
  /**
   * Is the given value within the bounded value.
   * @param value The tested value.
   * @return True, if and only if the value is within the bounded value.
   */
  default boolean isWithin(TYPE value) {
    Comparator<? super TYPE> cmp = getComparator();
    return (
      cmp != null &&
      Boundary.withinLowerBoundary(cmp, value, getLowerBoundary()) &&
      Boundary.withinUpperBoundary(cmp, value, getUpperBoundary())
    );
  }

  /**
   * Get the comparator used to compare the values.
   * @return The comparator used to compare values. An undefined value
   * indicates there is no comparison and thus nothing is withing the bounded.
   */
  @JsonProperty("comparator")
  Comparator<? super TYPE> getComparator();

  /**
   * Get the upper boundary of the bounded region.
   * @return The upper boundary of the bounded region. This is always defined value.
   */
  @JsonProperty("lower")
  Boundary<? extends TYPE> getLowerBoundary();

  /**
   * Get the lower boundary of the bounded region.
   * @return The lower boundary of the bounded region. This is always defined value.
   */
  @JsonProperty("upper")
  Boundary<? extends TYPE> getUpperBoundary();

  /**
   * Create a naturally bounded value.
   * @param <TYPE> The type of the naturally bounded value.
   * @param minValue The inclusive minimal value. If undefined, the
   * minimal value is not defined.
   * @param maxValue The inclusive maximal value. If undefined, the
   * maximal value is not defined.
   * @return The naturally ordered.
   */
  public static <
    TYPE extends Comparable<? super TYPE>
  > Bounded<TYPE> naturallyBounded(TYPE minValue, TYPE maxValue) {
    Comparator<? super TYPE> comparator = Comparator.naturalOrder();
    return new SimpleBounded<TYPE>(
      comparator,
      Boundary.inclusive(minValue),
      Boundary.inclusive(maxValue)
    );
  }

  /**
   * Create a bounded value.
   * @param <TYPE>
   * @param comparator
   * @param lowerBoundary
   * @param upperBoundary
   * @return
   */
  @JsonCreator
  public static <TYPE> Bounded<TYPE> create(
    @JsonProperty("comprator") Comparator<? super TYPE> comparator,
    @JsonProperty("lower") Boundary<? extends TYPE> lowerBoundary,
    @JsonProperty("upper") Boundary<? extends TYPE> upperBoundary
  ) {
    return new SimpleBounded<TYPE>(comparator, lowerBoundary, upperBoundary);
  }
}
