package com.kautiainen.antti.dunerest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/**
 * Testing the database creation works.
 */
public class DatabaseTest {
  static {
    // Debugging the environment.
    System.err.println("ENVIRONEMNT DEBUG");
    System
      .getProperties()
      .keySet()
      .forEach((Object key) -> {
        System.err.printf(
          "[%s]:[%s]\n",
          key,
          System.getProperty(String.valueOf(key), "[NO VALUE GIVEN]")
        );
      });
  }

  /**
   * Get the SQL Connection to the test database.
   * @return The database connection to the test database.
   * @throws SQLException The operation fails due SQL exception.
   */
  public Connection getConnection() throws SQLException {
    String uri = "jdbc:postgresql://localhost/dune-test";
    Properties props = new Properties();
    props.setProperty("user", System.getProperty("db.test.user", "dunetest"));
    props.setProperty(
      "password",
      System.getProperty("db.test.password", "DuneTester")
    );
    return DriverManager.getConnection(uri, props);
  }

  @Test
  @Order(1)
  public void testCreateDatabase() {
    try {
      Connection conn = getConnection();
      Database db = new Database();
      db.drop(conn);
      db.create(conn);
      testTablesWerePopulated(conn, db);
    } catch (SQLException e) {
      fail("Database connection failed: ", e);
    }
  }

  /**
   * Test creation of the tables.
   * @param conn The connection to the database.
   * @param db The tested database.
   */
  public void testCreateTables(Connection conn, Database db) {
    try {
      assertTrue(
        db.createTables(conn),
        String.format("Creation of tables failed")
      );
      Statement stmt = conn.createStatement();
      ResultSet result = stmt.executeQuery("SELECT * FROM Person");
      if (result.next()) {
        fail("The table is not empty!");
      }
      result = stmt.executeQuery("SELECT * FROM Motivation");
      List<String> names = new ArrayList<>();
      while (result.next()) {
        names.add(result.getString("name"));
      }
      result.close();
      stmt.close();
    } catch (SQLException sqle) {
      fail("Database operation failed: ", sqle);
    }
  }

  /**
   * Test creation of the views.
   * @param conn The connection to the database.
   * @param db The tested database.
   */
  public void testCreateViews(Connection conn, Database db) {
    try {
      if (!db.createViews(conn)) {
        fail("Database views was not created");
      }
    } catch (SQLException sqle) {
      fail("Database operation failed: ", sqle);
    }
  }

  /**
   * Test population of the tables.
   * @param conn The connection to the database.
   * @param db The tested database.
   */
  public void testPopulateTables(Connection conn, Database db) {
    try {
      db.populateTables(conn);
      testTablesWerePopulated(conn, db);
    } catch (SQLException sqle) {
      fail("Populate failed: ", sqle);
    }
  }

  /**
   * Test that the population of the tables happened.
   * @param conn The connection used to access the database.
   * @param db The tested database definition.
   * @throws SQLException The operation failed due SQL exception.
   */
  protected void testTablesWerePopulated(Connection conn, Database db)
    throws SQLException {
    Statement stmt = conn.createStatement();
    ResultSet result = stmt.executeQuery("SELECT name FROM Motivation");
    List<String> expectedNames = new ArrayList<>(db.getDefaultMotivations());
    while (result.next()) {
      expectedNames.remove(result.getString("name"));
    }
    if (!expectedNames.isEmpty()) {
      fail(
        "Not all of the default motivations populated: " +
        expectedNames.toString()
      );
    }
    result.close();
    stmt.close();
  }

  /**
   * Test if the database tables was drropped.
   * @param conn Database connection.
   * @throws SQLException The operation fails due SQL exception.
   * @throws AssertionError The test failed.
   */
  protected void testTablesWasDropped(Connection conn, Database db)
    throws SQLException {
    Statement stmt = conn.createStatement();
    ResultSet result;
    for (String tableName : db.getTableNames()) {
      try {
        result =
          stmt.executeQuery(String.format("SELECT * FROM %s", tableName));
        if (result.next()) {
          // Table was not dropped.
          result.close();
          throw new Exception(String.format("Table %s not dropped", tableName));
        }
        result.close();
        stmt.close();
      } catch (SQLException e) {
        // The opeartion did not fail.
      } catch (Exception e) {
        fail(String.format("Table %s not dropped%n", tableName));
      }
    }
  }

  /**
   * Test dropping of the tables.
   */
  @Test
  @Order(2)
  public void testDropTables() {
    try {
      Connection conn = getConnection();
      Database db = new Database();
      db.drop(conn);
      testTablesWasDropped(conn, db);
      testCreateTables(conn, db);
      db.drop(conn);
      testTablesWasDropped(conn, db);
      conn.close();
    } catch (SQLException sqle) {
      fail("Database operation failed: ", sqle);
    }
  }
}
