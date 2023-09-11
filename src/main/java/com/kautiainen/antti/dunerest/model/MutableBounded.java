package com.kautiainen.antti.dunerest.model;

import java.util.Comparator;

/**
 * Bounded value whose boundaries and the order may be changed.
 */
public interface MutableBounded<TYPE> extends Bounded<TYPE> {
  /**
   * The message indicating the changing of the comparator is not allowed.
   */
  public static final String IMMUTABLE_COMPARATOR_MESSAGE =
    "Changing comparator is not allowed";
  /**
   * The message indicating the changing of the lower boundary is not allowed.
   */
  public static final String IMMUTABLE_LOWER_BOUNDARY_MESSAGE =
    "Changing lower boundary is not allowed";
  /**
   * The message indicating the changing of the upper boundary is not allowed.
   */
  public static final String IMMUTABLE_UPPER_BOUNDARY_MESSAGE =
    "Changing upper boundary is not allowed";

  /**
   * Set the comparator of the bounded.
   * @param comparator The new comparator.
   * @throws IllegalArgumentException The given comparator was not accepted.
   * @throws UnsupportedOperationException The operation is not supported.
   */
  default void setComparator(Comparator<? super TYPE> comparator)
    throws IllegalArgumentException, UnsupportedOperationException {
    throw new UnsupportedOperationException(IMMUTABLE_COMPARATOR_MESSAGE);
  }

  /**
   * Set the lower boundary.
   * @param lowerBoundary The new lower boundary.
   * @throws IllegalArgumentException The new boundary was not accepted.
   * @throws UnsupportedOperationException The opeartion is not supported.
   */
  default void setLowerBoundary(Boundary<? extends TYPE> lowerBoundary)
    throws IllegalArgumentException, UnsupportedOperationException {
    throw new UnsupportedOperationException(IMMUTABLE_LOWER_BOUNDARY_MESSAGE);
  }

  /**
   * Set the upper boundary.
   * @param upperBoundary The new upper boundary.
   * @throws IllegalArgumentException The new boundary was not accepted.
   * @throws UnsupportedOperationException The opeartion is not supported.
   */
  default void setUpperBoundary(Boundary<? extends TYPE> upperBoundary)
    throws IllegalArgumentException, UnsupportedOperationException {
    throw new UnsupportedOperationException(IMMUTABLE_UPPER_BOUNDARY_MESSAGE);
  }
}
