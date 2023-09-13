package com.kautiainen.antti.dunerest.db;

import com.kautiainen.antti.dunerest.model.Motivation;
import com.kautiainen.antti.dunerest.model.Person;
import com.kautiainen.antti.dunerest.model.Skill;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.sql.DataSource;

/**
 * The data access object for persons - the characters of the game.
 */
public class PersonDao implements DAO<Integer, Person> {

  /**
   * The data source of the dao.
   */
  private DataSource source;

  @Override
  public DataSource getSource() {
    return source;
  }

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

  protected boolean addPersonMotivation(
    Connection conn,
    Integer personId,
    Integer motivationId,
    Motivation motivation
  ) throws SQLException {
    PreparedStatement addPersonMotivationStmt = conn.prepareStatement(
      "INSERT INTO PersonMotivations (person_id, motivation_id, value, statement, challenged) VALUES (?,?,?, ?, ?)"
    );

    int index = 1;
    addPersonMotivationStmt.setInt(index++, personId);
    addPersonMotivationStmt.setInt(index++, motivationId);
    addPersonMotivationStmt.setShort(
      index++,
      motivation.getValue().shortValue()
    );
    addPersonMotivationStmt.setString(index++, motivation.getStatement());
    if (addPersonMotivationStmt.executeUpdate() == 1) {
      // The adding succeeded
      return true;
    } else {
      // The adding failed
      return false;
    }
  }

  protected boolean updatePersonMotivation(
    Connection conn,
    Integer personId,
    Integer motivationId,
    Motivation motivation
  ) throws SQLException {
    PreparedStatement updateStmt = conn.prepareStatement(
      "UPDATE PersonMotivation SET value=? AND statement = ? AND  challenged = ? WHERE person_id=? AND motivation_id=?"
    );

    int index = 1;
    updateStmt.setShort(index++, motivation.getValue().shortValue());
    updateStmt.setString(index++, motivation.getStatement());
    updateStmt.setInt(index++, personId);
    updateStmt.setInt(index++, motivationId);
    if (updateStmt.executeUpdate() == 1) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Add or update a motivation.
   * @param connection The database connection used to update or add motivation.
   * @param personId The identifier of the person.
   * @param motivation The motivation added or updated.
   * @throws SQLException The operation fails due SQL error.
   */
  protected void addOrUpdateMotivation(
    Connection conn,
    Integer personId,
    Motivation motivation
  ) throws SQLException {
    // Check if the motivation exists.
    // TODO: Replace this with motivation DAO.
    PreparedStatement getIdStmt = conn.prepareStatement(
      "SELECT id FROM Motivation WHERE name=?"
    );
    PreparedStatement getPersonMotivationStmt = conn.prepareStatement(
      "SELECT * FROM PersonMotivationsView WHERE person_id = ? AND motivation_id = ?;"
    );
    PreparedStatement addNewStmt = conn.prepareStatement(
      "INSERT INTO Motivation (name) VALUES (?)"
    );
    Integer motivationId = null;
    String motivationName = motivation.getName();
    getIdStmt.setString(1, motivationName);
    ResultSet rows = getIdStmt.executeQuery();
    if (rows.next()) {
      // The motivation does exist.
      motivationId = rows.getInt("id");
      rows.close();

      // Checking if the motivation does exist.
      int index = 1;
      getPersonMotivationStmt.setInt(index++, personId);
      getPersonMotivationStmt.setInt(index++, motivationId);
      rows = getPersonMotivationStmt.executeQuery();
      if (rows.next()) {
        // Update a person motivation.
        updatePersonMotivation(conn, personId, motivationId, motivation);
      } else {
        // Add a new person motivation.
        if (!addPersonMotivation(conn, personId, motivationId, motivation)) {
          throw new SQLException("Cound not add new motivation");
        }
      }
    } else {
      // Create a new motivation.
      rows.close();
      addNewStmt.setString(1, motivationName);
      ResultSet addedIds = addNewStmt.getGeneratedKeys();
      if (addedIds.next()) {
        motivationId = addedIds.getInt("id");
      }
      if (
        !(
          (addNewStmt.executeUpdate() == 1) &&
          addPersonMotivation(conn, personId, motivationId, motivation)
        )
      ) {
        // The motivation was not added.
        throw new SQLException("Could not create a new motivation");
      }
    }
    getIdStmt.close();
    addNewStmt.close();
  }

  /**
   * Create a new person from identified.
   * @param added The added identified.
   * @return The created identified.
   * @throws SQLException The creation failed due SQL exception.
   * @throws IllegalArgumentException THe given added was invalid.
   */
  public Identified<Integer, Person> create(Identified<Integer, Person> added)
    throws IllegalStateException, IllegalArgumentException {
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
      for (Motivation motivation : added.getValue().getMotivations()) {
        addOrUpdateMotivation(conn, personId, motivation);
      }

      // TODO: Adding traits

      // TODO: Adding talents.

      // TODO: Adding assets

      return result;
    } catch (SQLException sqle) {
      throw new IllegalStateException("The data source update failed", sqle);
    } catch (IllegalArgumentException iae) {
      // The person was invalid.
      throw new IllegalArgumentException("Invalid added person", iae);
    }
  }

  /**
   * Fetch motivations of a person.
   * @param conn The connection used to access the database.
   * @param identifier The identifier of the person.
   * @return The set containing the motivations of the person.
   * @throws SQLException The operation failed due SQL exception.
   */
  protected java.util.Set<Motivation> fetchMotivations(
    Connection conn,
    Integer identifier
  ) throws SQLException {
    PreparedStatement motivationStmt = conn.prepareStatement(
      "SELECT * " + "FROM PersonMotivationsView " + "WHERE person_id=?" + ";"
    );
    motivationStmt.setInt(1, identifier);
    ResultSet result = motivationStmt.executeQuery();
    Map<String, Motivation> values = new TreeMap<>();
    while (result.next()) {
      // Getting motivations.
      String name = result.getString("name");
      values.put(
        name,
        new Motivation(
          name,
          (int) result.getShort("value"),
          result.getString("statement"),
          result.getBoolean("challenged")
        )
      );
    }
    return new java.util.HashSet<>(values.values());
  }

  /**
   * Fetch skills of a person.
   * @param conn The connection used to access the database.
   * @param identifier The identifier of the person.
   * @return The set containing the skills of the person.
   * @throws SQLException The operation failed due SQL exception.
   */
  protected java.util.Set<Skill> fetchSkills(
    Connection conn,
    Integer identifier
  ) throws SQLException {
    PreparedStatement motivationStmt = conn.prepareStatement(
      "SELECT * " + "FROM PersonSkillsView " + "WHERE person_id=?" + ";"
    );
    motivationStmt.setInt(1, identifier);
    ResultSet result = motivationStmt.executeQuery();
    Map<String, Skill> values = new TreeMap<>();
    while (result.next()) {
      // Getting motivations.
      String name = result.getString("name");
      values.put(name, new Skill(name, (int) result.getShort("value")));
    }
    return new java.util.HashSet<>(values.values());
  }

  /**
   * Get the person with given identifier.
   * @param identifier The identifier of the sought character.
   * @return A defined identified, if the person with given identifier exists.
   * Otherwise, an undefined value is returned.
   */
  public Identified<Integer, Person> fetch(Integer identifier)
    throws IllegalStateException {
    try {
      if (identifier == null) return null;
      Connection conn = source.getConnection();
      PreparedStatement stmt = conn.prepareStatement(
        "SELECT (id, name) FROM Person WHERE id=?;"
      );
      stmt.setInt(1, identifier);
      ResultSet result = stmt.executeQuery();
      Integer id = null;
      Person person = new Person();
      if (result.next()) {
        // We do have a person.
        person.setName(result.getString("name"));
        id = result.getInt("id");

        // Fetching motivations.
        person.setMotivations(fetchMotivations(conn, id));

        return Identified.create(id, person);
      } else {
        // No person found.
        result.close();
        return null;
      }
    } catch (SQLException sqle) {
      throw new IllegalStateException(sqle);
    }
  }

  /**
   * Featch all persons.
   * @return The list of all persons along with their identifiers.
   * @throws SQLException The operation failed to a SQL exception.
   */
  @Override
  public List<Identified<Integer, Person>> fetchAll()
    throws IllegalStateException {
    return fetchAll(null);
  }

  /**
   * Featch all persons with given identifiers.
   * @param identifiers The list of identifiers. If undefined, all identifiers are
   * accepted.
   * @return The list of persons, whose identifiers are given. If the identifiers
   * is undefined, all persons are returned.
   * @throws SQLException The operation fails due SQL Exception.
   */
  @Override
  public List<Identified<Integer, Person>> fetchAll(
    Collection<? extends Integer> identifiers
  ) throws IllegalStateException {
    try {
      Connection conn = source.getConnection();
      PreparedStatement stmt = conn.prepareStatement(
        "SELECT (id, name) FROM Person" +
        (
          identifiers == null
            ? ""
            : " WHERE " +
            String.join(
              " OR ",
              identifiers.stream().map(id -> ("id=?")).toList()
            )
        )
      );
      ResultSet result = stmt.executeQuery();
      List<Identified<Integer, Person>> people = new ArrayList<>();
      Integer id = null;
      while (result.next()) {
        // Creating the person.
        Person person = new Person();
        // We do have a person.
        person.setName(result.getString("name"));
        id = result.getInt("id");

        // Fetching motivations.
        person.setMotivations(fetchMotivations(conn, id));

        people.add(Identified.create(id, person));
      }
      // No person found.
      result.close();
      return people;
    } catch (SQLException sqle) {
      throw new IllegalStateException(sqle);
    }
  }
}
