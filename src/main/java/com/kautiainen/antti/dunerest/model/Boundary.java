package com.kautiainen.antti.dunerest.model;

import java.util.Comparator;

/**
 * A boundary defines a boundary.
 * An undefined boundary will not have boundary value, and is never inclusive.
 */
public interface Boundary<T> {
  /**
   * Is the boundary inclusive.
   * @return True, if and only if the boundary is inclusive.
   */
  boolean isInclusive();

  /**
   * Get the boundary value.
   * @return The value of the boundary.
   */
  T getBoundary();

  /**
   * Is the boundary defined.
   * @return True, if and only if the boundary does have boundary value.
   */
  default boolean isDefined() {
    return true;
  }

  /**
   * Create an unbounded boundary. An unbouded boundary never
   * has a bounding value. An unbounded boundary is never defined.
   * An unbouded value is never inclusive.
   * @param <TYPE> The type of the boundary value.
   * @return An undefined boundary of the given type.
   */
  static <TYPE> Boundary<TYPE> unbounded() {
    return new Boundary<>() {
      @Override
      public boolean isInclusive() {
        return false;
      }

      @Override
      public TYPE getBoundary() {
        return null;
      }

      @Override
      public boolean isDefined() {
        return false;
      }
    };
  }

  /**
   * Create a boundary with inclusive value.
   * @param <TYPE> The type of the boundary value.
   * @param inclusiveBoundary The inclusive boundary.
   * @return The inclusive boundary with given boundary value.
   * @implNote The order is assumed to contain null as a valid value.
   */
  static <TYPE> Boundary<TYPE> inclusive(TYPE inclusiveBoundary) {
    return new Boundary<>() {
      @Override
      public boolean isInclusive() {
        return true;
      }

      @Override
      public TYPE getBoundary() {
        return inclusiveBoundary;
      }
    };
  }

  /**
   * Create a boundary with exclusive value.
   * @param <TYPE> The type of the boundary value.
   * @param exclusiveBoundary The exclusive boundary.
   * @return The exclusive boundary with given boundary value.
   * @implNote The order is assumed to contain null as a valid value.
   */
  static <TYPE> Boundary<TYPE> exclusive(TYPE esclusiveBoundary) {
    return new Boundary<>() {
      @Override
      public boolean isInclusive() {
        return false;
      }

      @Override
      public TYPE getBoundary() {
        return esclusiveBoundary;
      }
    };
  }

  /**
   * Is the value within the upper boudnary using a comparator.
   * @param <TYPE> The type of the compared value.
   * @param comparator The comparator performing the comparison of the values.
   * @param value The value compared with the boundary.
   * @param boundary The upper boundary.
   * @return True, if and only if the given value is within the upper boundary.
   */
  static <TYPE> boolean withinUpperBoundary(
    Comparator<? super TYPE> comparator,
    TYPE value,
    Boundary<? extends TYPE> boundary
  ) {
    if (comparator == null || boundary == null) return false;
    try {
      return (
        (!boundary.isDefined()) ||
        comparator.compare(value, boundary.getBoundary()) <=
        (boundary.isInclusive() ? 0 : -1)
      );
    } catch (ClassCastException | NullPointerException cce) {
      // The value is not within the bounds, if it is not comparable with the comaprison.
      return false;
    }
  }

  /**
   * Is the given value within the lower boundary using a comparator.
   * @param <TYPE> The type of the compared value.
   * @param comparator The comparator performing the comparison of the values.
   * @param value The value compared with the boundary.
   * @param boundary The lower boundary.
   * @return True, if and only if the given value is within the lower boundary.
   */
  static <TYPE> boolean withinLowerBoundary(
    Comparator<? super TYPE> comparator,
    TYPE value,
    Boundary<? extends TYPE> boundary
  ) {
    if (comparator == null || boundary == null) return false;
    try {
      return (
        (!boundary.isDefined()) ||
        comparator.compare(value, boundary.getBoundary()) >=
        (boundary.isInclusive() ? 0 : 1)
      );
    } catch (ClassCastException | NullPointerException cce) {
      // The value is not within the bounds, if it is not comparable with the comaprison.
      return false;
    }
  }

  /**
   * Is the value within lower boundary using natural order.
   * @param <TYPE> The type of the tested value.
   * @param value The tested value.
   * @param boundary The lower boundary.
   * @return True, if and only if the value is within the lower boundary.
   */
  static <TYPE extends Comparable<? super TYPE>> boolean withinLowerBoundary(
    TYPE value,
    Boundary<? extends TYPE> boundary
  ) {
    if (value == null) return !boundary.isDefined();
    return (
      value != null &&
      withinLowerBoundary(
        Comparator.naturalOrder(),
        value,
        (boundary == null ? unbounded() : boundary)
      )
    );
  }

  /**
   * Is the value within upper boundary using natural order.
   * @param <TYPE> The type of the tested value.
   * @param value The tested value.
   * @param boundary The upper boundary.
   * @return True, if and only if the value is within the upper boundary.
   */
  static <TYPE extends Comparable<? super TYPE>> boolean withinUpperBoundary(
    TYPE value,
    Boundary<? extends TYPE> boundary
  ) {
    return (
      value != null &&
      withinUpperBoundary(
        Comparator.naturalOrder(),
        value,
        (boundary == null ? unbounded() : boundary)
      )
    );
  }
}
