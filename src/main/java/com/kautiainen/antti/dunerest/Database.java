package com.kautiainen.antti.dunerest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The database of the Dune.
 */
public class Database {

  /**
   * Create tables of the Dune database.
   * @param connection The database connection used to create tables.
   * @return True, if and only if the tables were created.
   * @throws SQLException The operation failed due SQL exception.
   */
  public boolean createTables(Connection connection) throws SQLException {
    boolean result = true;
    PreparedStatement stmt;
    stmt =
      connection.prepareStatement(
        "CREATE TABLE IF NOT EXISTS Person (" +
        ", " +
        "id serial primary key" +
        ", " +
        "name varchar(255) not null" +
        ")"
      );
    stmt.executeUpdate();

    stmt =
      connection.prepareStatement(
        "CREATE TABLE IF NOT EXISTS Motivation (" +
        "id serial primary key" +
        ", " +
        "name varchar(255) not null" +
        ", " +
        "description text" +
        ");"
      );
    stmt.executeUpdate();

    stmt =
      connection.prepareStatement(
        "CREATE TABLE IF NOT EXISTS PersonMotivations (" +
        "person_id int NOT NULL references Person (id) ON UPDATE CASCADE ON DELETE CASCADE" +
        "," +
        "motivation_id int NOT NULL references Person (id) ON UPDATE CASCADE ON DELETE CASCADE" +
        "," +
        "value smallint DEFAULT '4'" +
        "," +
        "statement varchar(255)" +
        "," +
        "PRIMARY KEY (person_id, motivation_id)" +
        ")"
      );

    return result;
  }

  /**
   * Create views.
   * @param connection The database connection used to create the views.
   * @return True, if and only if all views were created.
   * @throws SQLException The construction of any view failed.
   */
  public boolean createViews(Connection connection) throws SQLException {
    return true;
  }

  /**
   * Populate the created tables with default values.
   * @param connection The database connection used to insert initial content to the database.
   * @return True, if and only if the tables were craeted.
   * @throws SQLException The population of the database failed.
   */
  public boolean populateTables(Connection connection) throws SQLException {
    AtomicBoolean result = new AtomicBoolean(false);
    PreparedStatement stmt = connection.prepareStatement(
      "INSERT INTO Motivation (name) VALUES (?)"
    );
    getDefaultMotivations()
      .forEach((String motivation) -> {
        try {
          stmt.setString(1, motivation);
          result.set(result.get() | stmt.executeUpdate() > 0);
        } catch (SQLException sqle) {}
      });
    return result.get();
  }

  /**
   * The defautl motivation names of the database.
   * @return The list of default motivations the database is populated with.
   */
  protected Iterable<String> getDefaultMotivations() {
    return Arrays.asList("Duty", "Power", "Justice", "Truth", "Faith");
  }

  /**
   * Create the database.
   * @param connection Teh creation of the database.
   * @return True, if and only if the cration of the database succeeded.
   */
  public boolean create(Connection connection) {
    try {
      connection.createStatement().execute("START TRANSACTION");
      boolean result =
        createTables(connection) &&
        createViews(connection) &&
        populateTables(connection);
      connection.createStatement().execute("COMMIT");
      return result;
    } catch (SQLException sqle) {
      // Rollback the transaction
      try {
        connection.createStatement().execute("ROLLBACK");
      } catch (SQLException ignored) {}
      return false;
    }
  }

  /**
   * Drops the Database of the dune rest.
   * @param connection The connection to the database.
   */
  public boolean drop(Connection connection) throws SQLException {
    try {
      connection.createStatement().execute("START TRANSACTION");
      connection
        .createStatement()
        .execute("DROP TABLE IF EXISTS PersonMotivations, Motivation, Person");
      connection.createStatement().execute("COMMIT");
      return true;
    } catch (SQLException sqle) {
      try {
        connection.createStatement().execute("ROLLBACK");
      } catch (SQLException ignored) {}
      return false;
    }
  }
}
