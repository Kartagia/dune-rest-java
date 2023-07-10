package com.kautiainen.antti.dunerest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kautiainen.antti.dunerest.CreateDatabase.TableDefinition;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Testing create CreateDatabase.TableDefinitionTest
 */
public class CreateDatabase_TableDefinitionTest {

  public static final List<String> TEST_CASE_NAMES;

  public static final SortedMap<String, TableDefinition> TABLE_DEFINITION_MAP;

  public static final SortedMap<String, String> EXPECTED_CREATE_SQL;

  public static final SortedMap<String, String> EXPECTED_DROP_SQL;

  public List<String> tableDefinitionCases() {
    return TEST_CASE_NAMES;
  }

  static {
    List<String> tableNames = Arrays.asList(
      "empty",
      "Art",
      "Spell",
      "Spell Fail",
      ""
    );
    TEST_CASE_NAMES = Collections.unmodifiableList(tableNames);

    SortedMap<String, TableDefinition> result = new TreeMap<>();
    final SortedMap<String, String> sqlCreateTables = new TreeMap<>();
    final SortedMap<String, String> sqlDropTables = new TreeMap<>();
    final AtomicInteger index = new AtomicInteger(0);
    String name;

    // empty
    name = tableNames.get(index.get());
    sqlCreateTables.put(name, String.format("CREATE TABLE %s (%s);", name, ""));
    sqlDropTables.put(name, String.format("DROP TABLE %s;", name));

    result.put(
      name,
      new TableDefinition() {
        /**
         * Name of the test table.
         */
        final String name = TEST_CASE_NAMES.get(index.get());

        @Override
        public String getTableName() {
          return name;
        }

        @Override
        public String getCreateTable() {
          return sqlCreateTables.get(getTableName());
        }

        @Override
        public String getRemoveTable() {
          return sqlDropTables.get(getTableName());
        }
      }
    );
    // Art
    index.set(index.get() + 1);
    name = tableNames.get(index.get());
    sqlCreateTables.put(
      name,
      String.format(
        "CREATE TABLE %s (%s);",
        name,
        String.format(
          "CREATE TABLE %s (%s);",
          name,
          "id SERIAL PRIMARY KEY," +
          "name VARCHAR(10) UNIQUE NOT NULL," +
          "abbrev VARCHAR(3) UNIQUE NOT NULL," +
          "unique (abbrev)" +
          ");"
        )
      )
    );
    sqlDropTables.put(name, String.format("DROP TABLE %s;", name));
    result.put(
      name,
      new TableDefinition() {
        /**
         * Name of the table.
         */
        final String name = TEST_CASE_NAMES.get(index.get());

        @Override
        public String getTableName() {
          return name;
        }

        @Override
        public String getCreateTable() {
          return sqlCreateTables.get(getTableName());
        }

        @Override
        public String getRemoveTable() {
          return sqlDropTables.get(getTableName());
        }
      }
    );

    // Spell
    index.set(index.get() + 1);
    name = tableNames.get(index.get());
    sqlCreateTables.put(
      name,
      String.format(
        "CREATE TABLE %s (%s);",
        name,
        "id SERIAL PRIMARY KEY," +
        "name VARCHAR(10) UNIQUE NOT NULL," +
        "abbrev VARCHAR(3) UNIQUE NOT NULL," +
        "unique (abbrev)" +
        ");"
      )
    );
    sqlDropTables.put(name, String.format("DROP TABLE %s;", name));
    result.put(
      name,
      new TableDefinition() {
        /**
         * Name of the table.
         */
        final String name = TEST_CASE_NAMES.get(index.get());

        @Override
        public String getTableName() {
          return name;
        }

        @Override
        public String getCreateTable() {
          return sqlCreateTables.get(getTableName());
        }

        @Override
        public String getRemoveTable() {
          return sqlDropTables.get(getTableName());
        }
      }
    );

    // "Spell Fail" -- invalid.
    index.set(index.get() + 1);
    name = tableNames.get(index.get());
    result.put(name, null);
    sqlCreateTables.put(name, null);
    sqlDropTables.put(name, null);

    // "" -- invalid.
    index.set(index.get() + 1);
    name = tableNames.get(index.get());
    result.put(name, null);
    sqlCreateTables.put(name, null);
    sqlDropTables.put(name, null);

    TABLE_DEFINITION_MAP = Collections.unmodifiableSortedMap(result);
    EXPECTED_CREATE_SQL = Collections.unmodifiableSortedMap(sqlCreateTables);
    EXPECTED_DROP_SQL = Collections.unmodifiableSortedMap(sqlDropTables);
  }

  public Map<String, TableDefinition> getTableDefinitions() {
    return new HashMap<>(TABLE_DEFINITION_MAP);
  }

  @Test
  void testGetCreateTable() {
    System.out.println("Testing: getCreateTable()");
    for (String table : tableDefinitionCases()) {
      TableDefinition source = TABLE_DEFINITION_MAP.get(table);
      String expectedResult = getExpectedCreateSql(table);
      String result = null;
      if (source != null) {
        result = source.getCreateTable();
        System.out.printf("Test %s: ", table);
        try {
          assertEquals(expectedResult, result);
          System.out.printf("[%s]\n", "PASSED");
        } catch (AssertionError error) {
          System.out.printf("[%s]\n", "FAILED");
          throw error;
        }
      }
    }
  }

  protected String getExpectedCreateSql(String table) {
    return CreateDatabase_TableDefinitionTest.EXPECTED_CREATE_SQL.get(table);
  }

  protected String getExpectedDropSql(String table) {
    return CreateDatabase_TableDefinitionTest.EXPECTED_DROP_SQL.get(table);
  }

  @Test
  void testGetRemoveTable() {
    System.out.println("Testing: getRemoveTable()");
    for (String table : tableDefinitionCases()) {
      TableDefinition source = TABLE_DEFINITION_MAP.get(table);
      String expectedResult = getExpectedDropSql(table);
      String result = null;
      if (source != null) {
        result = source.getRemoveTable();
        System.out.printf("Test %s: ", table);
        try {
          assertEquals(expectedResult, result);
          System.out.printf("[%s]\n", "PASSED");
        } catch (AssertionError error) {
          System.out.printf("[%s]\n", "FAILED");
          throw error;
        }
      }
    }
  }

  @Test
  void testGetTableName() {
    System.out.println("Testing: getTableName()");

    for (String table : tableDefinitionCases()) {
      TableDefinition source = TABLE_DEFINITION_MAP.get(table);
      String expectedResult = table;
      String result = null;
      if (source != null) {
        result = source.getTableName();
        System.out.printf("Test %s: ", table);
        try {
          assertEquals(expectedResult, result);
          System.out.printf("[%s]\n", "PASSED");
        } catch (AssertionError error) {
          System.out.printf("[%s]\n", "FAILED");
          throw error;
        }
      }
    }
  }

  @Test
  void testValidTableName() {
    System.out.println("Testing: static validTableName(String)");
    for (String table : tableDefinitionCases()) {
      TableDefinition source = TABLE_DEFINITION_MAP.get(table);
      boolean expectedResult = source != null;
      boolean result = CreateDatabase.TableDefinition.validTableName(table);
      System.out.printf("Test %s: ", table);
      try {
        assertEquals(expectedResult, result);
        System.out.printf("[%s]\n", "PASSED");
      } catch (AssertionError error) {
        System.out.printf("[%s]\n", "FAILED");
        throw error;
      }
    }
  }
}
