package com.kautiainen.antti.dunerest.db;

import java.util.Collection;
import java.util.List;
import javax.sql.DataSource;

/**
 * Dao represents data access object storage.
 * This is also a CRUD implementation replacing retrieve with fetch.
 * @param <ID> The identifier type.
 * @param <DATA> The data type.
 */
public interface DAO<ID, DATA> {
  /**
   * Get the data source of the data access object.
   * @return The data source of the data access object.
   */
  DataSource getSource();

  /**
   * Create a new instance of the data in the data storage.
   * @param entry The added entry.
   * @return The data and the identifier stored into the storage.
   * @throws IllegalArgumentException The added value was invalid.
   * @throws IllegalStateException The state of the data storage prevented the operation.
   * @throws UnsupportedOperationException The operation is not supported.
   */
  default Identified<ID, DATA> create(Identified<ID, DATA> entry)
    throws IllegalArgumentException, IllegalStateException {
    throw new UnsupportedOperationException("Operation not supported");
  }

  /**
   * Fetch the data attached to the key.
   * @param key The seeked key.
   * @return A defined value indicating the given key has a value. Otherwise, an undefined
   * value is returned to indicate there was no data associated with the key.
   * @throws IllegalStateException The state of the data storage prevented the operation.
   * @throws UnsupportedOperationException The operation is not supported.
   */
  default Identified<ID, DATA> fetch(ID key) throws IllegalStateException {
    throw new UnsupportedOperationException("Operation not supported");
  }

  /**
   * Get all data entries.
   * @return The list containing all data entries of the data access object.
   * @throws IllegalStateException The storage state prevents the operation.
   * @throws UnsupportedOperationException The operation is not supported.
   * @implNote The default implementation calls {@link #fetchAll(Collection)} with
   * an undefined list.
   */
  default List<Identified<ID, DATA>> fetchAll() throws IllegalStateException {
    return fetchAll(null);
  }

  /**
   * Get all data entries whose identifiers are in the list.
   * @param identifiers The list of the wanted identifiers. If this value is undefined,
   * all identifiers are returned.
   * @return The list containing all data entries, whose identifiers are given.
   * @throws IllegalStateException The storage state prevents the operation.
   * @throws UnsupportedOperationException The operation is not supported.
   */
  default List<Identified<ID, DATA>> fetchAll(
    Collection<? extends ID> identifiers
  ) throws IllegalStateException {
    throw new UnsupportedOperationException("Operation not supported");
  }

  /**
   * Update an existing value.
   * @param value The updated value.
   * @return True, if and only if the value was updated.
   * @throws IllegalArgumentException The given value was invalid.
   * @throws IllegalStateException The storage state prevents the operation.
   * @throws UnsupportedOperationException The operation is not supported.
   */
  default boolean update(Identified<ID, DATA> value)
    throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
    throw new UnsupportedOperationException("Operation not supported");
  }

  /**
   * Update an existing value of a key.
   * If the new value has different key, the old key value is removed and the new value is
   * added.
   * @param key The altered key.
   * @param newValue The new value assosiated to the value.
   * @return True, if and only if the storage changed.
   * @throws IllegalArgumentException The given key did not have any value, or the new
   * value was invalid.
   * @throws IllegalStateException The storage state prevents the operation.
   * @throws UnsupportedOperationException The operation is not supported.
   */
  default boolean update(ID key, Identified<ID, DATA> newValue)
    throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
    throw new UnsupportedOperationException("Operation not supported");
  }

  /**
   * Delete a data entry with a key.
   * @param key The deleted key.
   * @return True, if and only if the identifier was deleted.
   * @throws IllegalStateException The storage state prevents the operation.
   * @throws UnsupportedOperationException The operation is not supported.
   */
  default boolean delete(ID key)
    throws IllegalStateException, UnsupportedOperationException {
    throw new UnsupportedOperationException("Operation not supported");
  }

  /**
   * Delete a data entry.
   * @param dataEntry The deleted data entry.
   * @return True, if and only if the data entry was deleted.
   * @throws IllegalStateException The storage state prevents the operation.
   * @throws UnsupportedOperationException The operation is not supported.
   * @implNote The default impolementation checks the data of the identfier is
   * the data of the deleted entry.
   * @implNote The default implemetnation calls {@link #delete(Object)} to remove
   * the entry.
   */
  default boolean delete(Identified<ID, DATA> dataEntry)
    throws IllegalStateException, UnsupportedOperationException {
    if (dataEntry == null) return false;
    Identified<ID, DATA> removed = fetch(dataEntry.getId());
    return (
      removed != null &&
      java.util.Objects.equals(removed.getValue(), dataEntry.getValue()) &&
      delete(dataEntry.getId())
    );
  }
}
