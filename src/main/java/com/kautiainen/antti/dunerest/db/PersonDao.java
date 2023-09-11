package com.kautiainen.antti.dunerest.db;

import com.kautiainen.antti.dunerest.model.Motivation;
import com.kautiainen.antti.dunerest.model.Person;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.sql.DataSource;

/**
 * The data access object for persons - the characters of the game.
 */
public class PersonDao {

  /**
   * The data source of the dao.
   */
  private DataSource source;

  /**
   * Create a new DAO using given data source.
   * @param source The data source.
   */
  public PersonDao(DataSource source) {
    if (source == null) throw new IllegalArgumentException(
      "Invalid data source",
      new NullPointerException("An undefined data source is not accepted")
    );
    this.source = source;
  }

  /**
   * Setter of the prepared statement values.
   */
  public static interface ParameterSetter<TYPE>
    extends BiConsumer<PreparedStatement, TYPE> {
    /**
     * @param statement The target prepared statement the value is assigned to.
     * @param value The assigend new value.
     * @throws IllegalArgumentException The value was invalid.
     */
    @Override
    public void accept(PreparedStatement statement, TYPE value)
      throws IllegalArgumentException;
  }

  /**
   * Create a parameter setter allowing values
   * @param paramType The actual type of the accepted parameter value.
   * @param index The index of the parameter.
   * @return The parameter setter settign the parameter value.
   * @param <TYPE> The type of the parameter value.
   * @throws IllegalArgumentException Any parameter was invalid.
   */
  protected <TYPE> ParameterSetter<TYPE> createParameterSetter(
    Class<? extends TYPE> paramType,
    int index
  ) {
    if (index < 1) throw new IllegalArgumentException("Invalid index value");
    return (PreparedStatement statement, TYPE value) -> {
      if (paramType != null && !paramType.isInstance(value)) {
        throw new IllegalArgumentException("Invalid value type");
      }
      try {
        statement.setObject(index, value);
      } catch (SQLFeatureNotSupportedException feat) {
        // The data type was invalid.
        throw new IllegalArgumentException("Invalid value type", feat);
      } catch (SQLException e) {
        // The index was invalid
        throw new IllegalArgumentException("Invalid parameter index", e);
      }
    };
  }

  /**
   * Create a new pereson.
   * @return The identifier of the person.
   * @throws SQLException The operation failed due SQL exception.
   */
  public Identified<Integer, Person> create(Person person) throws SQLException {
    return create(Identified.create(null, person));
  }

  /**
   * Create a new person from identified.
   * @param added The added identified.
   * @return The created identified.
   * @throws SQLException The creation failed due SQL exception.
   * @throws IllegalArgumentException THe given added was invalid.
   */
  public Identified<Integer, Person> create(Identified<Integer, Person> added)
    throws SQLException, IllegalArgumentException {
    if (added == null || added.getValue() == null) {
      throw new IllegalArgumentException(
        "Invalid created person",
        new NullPointerException("Cannot add an undefined person")
      );
    }

    try {
      String sql;
      List<ParameterSetter<Object>> paramSetters;
      Identified<Integer, Person> result = null;
      List<Object> parameterValues = new ArrayList<>();
      if (added.getId() == null) {
        sql = "INSERT INTO Person (name) VALUES (?)";
        int paramIndex = 1;
        paramSetters =
          Arrays.asList(createParameterSetter(String.class, paramIndex++));
        parameterValues.add(added.getValue().getName());
      } else {
        sql = "INSERT INTO Person (id, name) VALUES (?,?)";
        int paramIndex = 1;
        paramSetters =
          Arrays.asList(
            createParameterSetter(Integer.class, paramIndex++),
            createParameterSetter(String.class, paramIndex++)
          );
        parameterValues.add(added.getId());
        parameterValues.add(added.getValue().getName());
        result = added;
      }

      // Starting the transaction
      Connection conn = source.getConnection();
      PreparedStatement stmt;
      try {
        stmt = conn.prepareStatement(sql);
        for (
          int i = 0;
          i < parameterValues.size() && i < paramSetters.size();
          i++
        ) {
          paramSetters.get(i).accept(stmt, parameterValues.get(i));
        }
      } catch (SQLException exception) {
        throw new Error(
          "Could not prepare the created SQL statement",
          exception
        );
      }
      if (stmt.executeUpdate() > 0) {
        // The update succeeded.
        if (result == null) {
          // We need the insert value.
          ResultSet generatedKeys = stmt.getGeneratedKeys();
          if (generatedKeys.next()) {
            result =
              Identified.create(generatedKeys.getInt(1), added.getValue());
          }
          generatedKeys.close();
        }
      } else {
        // The update did not add a row.
        return null;
      }

      final int personId;
      if (result == null || result.getId() == null) {
        // The insertion of the person failed.
        return null;
      } else {
        personId = result.getId();
      }

      // TODO: Adding skills

      // Adding motivations.
      do {
        // TODO: Replace this with motivation DAO.
        PreparedStatement getIdStmt = conn.prepareStatement(
          "SELECT id FROM Motivation WHERE name=?"
        );
        PreparedStatement addNewStmt = conn.prepareStatement(
          "INSERT INTO Motivation (name) VALUES (?)"
        );
        PreparedStatement updatePersonMotivationStmt = conn.prepareStatement(
          "UPDATE PersonMotivation SET value=? AND statement = ? AND  challenged = ? WHERE person_id=? AND motivation_id=?"
        );
        PreparedStatement addPersonMotivationStmt = conn.prepareStatement(
          "INSERT INTO PersonMotivations (person_id, motivation_id, value, statement, challenged) VALUES (?,?,?, ?, ?)"
        );

        AtomicReference<SQLException> exception = new AtomicReference<SQLException>(
          null
        );
        final AtomicReference<Integer> motivationId = new AtomicReference<Integer>(
          null
        );
        added
          .getValue()
          .getMotivations()
          .forEach(
            (Consumer<? super Motivation>) (Motivation motivation) -> {
              if (exception.get() != null) {
                // The exception means we skip the rest of the motivations.
                return;
              }
              try {
                // Getting the motivation identifier of the motivation.
                motivationId.set(null);
                getIdStmt.setString(1, motivation.getName());
                ResultSet rows = getIdStmt.executeQuery();
                if (rows.next()) {
                  // The motivation id is acquried.
                  motivationId.set(rows.getInt("id"));
                } else {
                  addNewStmt.setString(1, motivation.getName());
                  if (addNewStmt.executeUpdate() > 0) {
                    // The operation succeeded.
                    ResultSet ids = addNewStmt.getGeneratedKeys();
                    if (ids.next()) {
                      motivationId.set(ids.getInt(1));
                    } else {
                      // This is an erroneous state as an id was generated.
                    }
                  } else {
                    // The update failed.
                  }
                }

                // Testing if we do have existing person motivation.
                try {
                  // First trying to update
                  int paramIndex = 1;
                  updatePersonMotivationStmt.setShort(
                    paramIndex++,
                    (short) motivation.getValue().intValue()
                  );
                  updatePersonMotivationStmt.setString(
                    paramIndex++,
                    motivation.getName()
                  );
                  updatePersonMotivationStmt.setBoolean(
                    paramIndex++,
                    motivation.isChallenged()
                  );
                  updatePersonMotivationStmt.setInt(paramIndex++, personId);
                  updatePersonMotivationStmt.setInt(
                    paramIndex++,
                    motivationId.get()
                  );
                  if (updatePersonMotivationStmt.executeUpdate() == 0) {
                    // Update did not update value -- Throwing exception to trigger adding value.
                    throw new Exception(
                      "Update did not change a value - the value is not existing"
                    );
                  }
                } catch (Exception noSuchMotivation) {
                  // Inserting the motivation to the result.
                  int paramIndex = 1;
                  addPersonMotivationStmt.setInt(
                    paramIndex++,
                    motivationId.get()
                  );
                  addPersonMotivationStmt.setInt(paramIndex++, personId);
                  addPersonMotivationStmt.setShort(
                    paramIndex++,
                    (short) motivation.getValue().intValue()
                  );
                  addPersonMotivationStmt.setString(
                    paramIndex++,
                    motivation.getStatement()
                  );
                  addPersonMotivationStmt.setBoolean(
                    paramIndex++,
                    motivation.isChallenged()
                  );
                  if (addPersonMotivationStmt.executeUpdate() > 0) {
                    // Update succeeded.
                    // TODO: Log adding the value was added.
                  } else {
                    // Update did not add a row.
                    // TODO: Log information of this problem and set exception.
                  }
                }
              } catch (SQLException ex) {
                // The motivation table does not exist, or the change was ivnalid.
                exception.set(ex);
              }
            }
          );
        if (exception.get() != null) {
          // The operation failed. Performing rollback.
          try {
            stmt.execute("ROLLBACK");
          } catch (SQLException e) {
            // The exception is ignored.
          }
          throw exception.get();
        }
        addPersonMotivationStmt.close();
        updatePersonMotivationStmt.close();
        getIdStmt.close();
        addNewStmt.close();
      } while (false);

      // TODO: Adding traits

      // TODO: Adding talents.

      // TODO: Adding assets

      return result;
    } catch (IllegalArgumentException iae) {
      // The person was invalid.
      throw new IllegalArgumentException("Invalid added person", iae);
    }
  }
}
