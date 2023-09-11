package com.kautiainen.antti.dunerest.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The database of the Dune. This class allows construction, and removal
 * of the database tables, views, and default content.
 */
public class Database {

  /**
   * Log a mesasge to a stream.
   * @param logger The stream into which the message is logged.
   * @param format The format used to create the message.
   * @param args The format parameter values.
   */
  public void log(java.io.PrintStream logger, String format, Object... args) {
    if (logger != null) {
      logger.printf(format, args);
    }
  }

  /**
   * Log a mesasge to a writer
   * @param logger The stream into which the message is logged.
   * @param format The format used to create the message.
   * @param args The format parameter values.
   */
  public void log(java.io.PrintWriter logger, String format, Object... args) {
    if (logger != null) {
      logger.printf(format, args);
    }
  }

  /**
   * Get table names of the database.
   * @return The list of the table names in the database.
   */
  public List<String> getTableNames() {
    return Arrays.asList("Motivation", "Person", "PersonMotivation");
  }

  /**
   * Get the view names of the database.
   * @return The list of the view names of the database.
   */
  public List<String> getViewNames() {
    return Arrays.asList();
  }

  /**
   * Create tables of the Dune database.
   * @param connection The database connection used to create tables.
   * @return True, if and only if the tables were created.
   * @throws SQLException The operation failed due SQL exception.
   */
  public boolean createTables(Connection connection) throws SQLException {
    return createTables(connection, System.err);
  }

  /**
   * Create tables of the Dune database.
   * @param connection The database connection used to create tables.
   * @param logger THe stream into which logging reports are printed.
   * @return True, if and only if the tables were created.
   * @throws SQLException The operation failed due SQL exception.
   */
  public boolean createTables(
    Connection connection,
    java.io.PrintStream logger
  ) throws SQLException {
    log(logger, "%n%nCreating tables:%n");
    boolean result = true;
    String tableName;
    PreparedStatement stmt;
    tableName = "Person";
    stmt =
      connection.prepareStatement(
        "CREATE TABLE IF NOT EXISTS Person (" +
        "id serial primary key" +
        ", " +
        "name varchar(255) not null" +
        ")"
      );
    stmt.executeUpdate();
    log(logger, "Table %s created%n", tableName);

    tableName = "Motivation";
    stmt =
      connection.prepareStatement(
        "CREATE TABLE IF NOT EXISTS Motivation (" +
        "id serial primary key" +
        ", " +
        "name varchar(255) not null unique" +
        ", " +
        "description text DEFAULT null" +
        ");"
      );
    stmt.executeUpdate();
    log(logger, "Table %s created%n", tableName);

    tableName = "PersonMotivations";
    stmt =
      connection.prepareStatement(
        "CREATE TABLE IF NOT EXISTS PersonMotivations (" +
        "person_id int NOT NULL references Person (id) ON UPDATE CASCADE ON DELETE CASCADE" +
        "," +
        "motivation_id int NOT NULL references Person (id) ON UPDATE CASCADE ON DELETE CASCADE" +
        "," +
        "value smallint DEFAULT '4'" +
        "," +
        "statement varchar(255) DEFAULT null" +
        ", " +
        "challenged boolean DEFAULT false" +
        "," +
        "PRIMARY KEY (person_id, motivation_id)" +
        ")"
      );
    stmt.executeUpdate();
    log(logger, "Table %s created%n", tableName);

    return result;
  }

  /**
   * Create views.
   * @param connection The database connection used to create the views.
   * @return True, if and only if all views were created.
   * @throws SQLException The construction of any view failed.
   */
  public boolean createViews(Connection connection) throws SQLException {
    log(System.err, "%n%nCreating views:%n");
    return true;
  }

  /**
   * Populate the created tables with default values.
   * @param connection The database connection used to insert initial content to the database.
   * @return True, if and only if the tables were craeted.
   * @throws SQLException The population of the database failed.
   */
  public boolean populateTables(Connection connection) throws SQLException {
    return populateTables(connection, System.err);
  }

  /**
   * Populate the created tables with default values.
   * @param connection The database connection used to insert initial content to the database.
   * @return True, if and only if the tables were craeted.
   * @throws SQLException The population of the database failed.
   */
  public boolean populateTables(
    Connection connection,
    java.io.PrintStream logger
  ) throws SQLException {
    log(logger, "%n%nPopulating tables:%n");
    AtomicBoolean result = new AtomicBoolean(false);
    PreparedStatement stmt = connection.prepareStatement(
      "INSERT INTO Motivation (name) VALUES (?)"
    );
    getDefaultMotivations()
      .forEach((String motivation) -> {
        log(logger, "Adding motivation %s%n", motivation);
        try {
          stmt.setString(1, motivation);
          result.set(result.get() | stmt.executeUpdate() > 0);
          log(logger, "Added motivation %s%n", motivation);
        } catch (SQLException sqle) {}
      });
    return result.get();
  }

  /**
   * The defautl motivation names of the database.
   * @return The list of default motivations the database is populated with.
   */
  public Collection<String> getDefaultMotivations() {
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
      for (String tableName : getTableNames()) {
        if (
          connection
            .createStatement()
            .executeUpdate("DROP TABLE IF EXISTS " + tableName) >
          0
        ) {
          log(System.err, "Table %s dropped%n", tableName);
        } else {
          log(System.err, "Table %s not dropped%n", tableName);
        }
      }
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
