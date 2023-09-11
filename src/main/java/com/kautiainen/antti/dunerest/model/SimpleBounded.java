package com.kautiainen.antti.dunerest.model;

import java.util.Comparator;
import java.util.Objects;

/**
 * A simple implementation of a bounded value.
 */
public class SimpleBounded<TYPE> implements Bounded<TYPE> {

  /**
   * The comparator used to compare teh values.
   */
  private Comparator<? super TYPE> comparator;

  /**
   * The lower boundary of the bounded value. This is always defined
   * value.
   */
  private Boundary<? extends TYPE> lowerBoundary;

  /**
   * The upper boundary of hte bounded value. This is always defined
   * valeu.
   */
  private Boundary<? extends TYPE> upperBoundary;

  /**
   * Create a new bounded value.
   * @param comparator The comparator used to compare the values.
   * @param lowerBoundary The lower boundary. If the boundary is not defined, it is
   * unbounded.
   * @param upperBoundary The upper boundary. If the boundary is nto defined, it is
   * unbounded.
   */
  public SimpleBounded(
    Comparator<? super TYPE> comparator,
    Boundary<? extends TYPE> lowerBoundary,
    Boundary<? extends TYPE> upperBoundary
  ) {
    this.comparator = comparator;
    this.lowerBoundary =
      Objects.requireNonNullElse(lowerBoundary, Boundary.unbounded());
    this.upperBoundary =
      Objects.requireNonNullElse(lowerBoundary, Boundary.unbounded());
  }

  /**
   * Create a bounded value using Java default boundary.
   * @param comparator The comparator used to compare the boundary values.
   * @param lowerBoundary The inclusive lower boundary.
   * @param upperBoundary The exclusive upper boundary.
   */
  public SimpleBounded(
    Comparator<? super TYPE> comparator,
    TYPE lowerBoundary,
    TYPE upperBoundary
  ) {
    this(
      comparator,
      Boundary.inclusive(lowerBoundary),
      Boundary.exclusive(upperBoundary)
    );
  }

  /**
   * Get the comparator used to compare the values.
   * @return The comparator used to compare values. An undefined value
   * indicates there is no comparison and thus nothing is withing the bounded.
   */
  @Override
  public Comparator<? super TYPE> getComparator() {
    return this.comparator;
  }

  /**
   * Get the upper boundary of the bounded region.
   * @return The upper boundary of the bounded region. This is always defined value.
   */
  @Override
  public Boundary<? extends TYPE> getLowerBoundary() {
    return this.lowerBoundary;
  }

  /**
   * Get the lower boundary of the bounded region.
   * @return The lower boundary of the bounded region. This is always defined value.
   */
  @Override
  public Boundary<? extends TYPE> getUpperBoundary() {
    return this.upperBoundary;
  }
}
