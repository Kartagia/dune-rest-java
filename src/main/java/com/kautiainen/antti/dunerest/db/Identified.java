package com.kautiainen.antti.dunerest.db;

/**
 * Identified represents an object identified by an identifier.
 * @param <ID> The identifier type.
 * @param <VALUE> The value type.
 */
public interface Identified<ID, VALUE> {
  /**
   * Get the value of the identifierd.
   * @return The current value of the identified.
   */
  VALUE getValue();

  /**
   * Get the identifier of the value.
   * @return The current identifier of the identified.
   */
  ID getId();

  /**
   * Is the given identifier equivalent of the identified.
   * @param identifier The tested identifier.
   * @return True, if and only if the given identified is equivalent
   * with the current one.
   */
  default boolean hasEquivalentId(Object identifier) {
    return java.util.Objects.equals(identifier, getId());
  }

  /**
   * Is the identified equivalent of the current identfied.
   * The identifiers are equivalent if they do have equal identifiers.
   * @param identified The tested identfiied.
   * @return True, if and only if the given identified is equivalent
   * of the current one.
   */
  default boolean isEquivalent(Identified<?, ? extends VALUE> identified) {
    return identified != null && hasEquivalentId(identified.getId());
  }

  /**
   * Is the identified equal to the current identified. The identified are
   * equals if they do have both equivalent identifier, and the values are
   * equals.
   * @param identified The tested identified.
   * @return True, if and only if the given identified is equal to the given
   * identified.
   */
  default boolean isEqual(Identified<?, ? extends VALUE> identified) {
    return (
      isEquivalent(identified) &&
      java.util.Objects.equals(getValue(), identified.getValue())
    );
  }

  /**
   * Create a new identified.
   * @param <ID> The type of the identifier.
   * @param <VALUE> The identified value.
   * @param identifier The identifier of the created identifier.
   * @param value The value of the created identified.
   * @return The identified with given identifier and value.
   */
  static <ID, VALUE> Identified<ID, VALUE> create(ID identifier, VALUE value) {
    return new Identified<ID, VALUE>() {
      @Override
      public VALUE getValue() {
        return value;
      }

      @Override
      public ID getId() {
        return identifier;
      }
    };
  }
}
