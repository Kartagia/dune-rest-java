package com.kautiainen.antti.dunerest.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The database of the Dune. This class allows construction, and removal
 * of the database tables, views, and default content.
 *
 * The database will parse the SQL files to determine the created tables and views
 * in order to generate the SQL commands to drop teh database.
 *
 * The databse class assumes each databse definition contains all cration of the persisting
 * tables and views can be performed before all population commands of the same SQL source.
 */
public class Database {

  /**
   * The SQL commands creating elements of each sql definition file
   * given. An undefined value indicates the file did not contain
   * SQL commands creating elements.
   */
  private List<String> createSqlCommands = new ArrayList<>();

  /**
   * The SQL commands populating tables for each SQL definition file.
   * An undefined member idnicates teh defintion did not containg any view
   * creating commands.
   */
  private List<String> populateTablesSql = new ArrayList<>();

  /**
   * The list containing an entry of SQL commands dropping tables from
   * the database creation file of the same index. If a value is undefined,
   * that sql command did not contain any creation or insert commands.
   */
  private List<String> dropTablesSql = new ArrayList<>();

  /**
   * The list of the known tables of the database in the order of construction.
   */
  private List<String> tableNames = new ArrayList<>();

  /**
   * The list of the known views of the database in the order of construction.
   */
  private List<String> viewNames = new ArrayList<>();

  /**
   * The list of the known populated tables.
   */
  private List<String> populatedTables = new ArrayList<>();

  /**
   * Create the default database loading the definition from the
   * resource file.
   */
  public Database() {
    this(
      "/sql/createTables.sql",
      "/sql/createViews.sql",
      "/sql/initTables.sql"
    );
  }

  /**
   * Pattern matching to the PostgreSQL create table command header.
   * The pattern captures the table name into group "table".
   */
  public static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
    "^\\s*CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+(?<table>\\w+)\\s*\\(\\s*$",
    Pattern.UNICODE_CASE |
    Pattern.UNICODE_CHARACTER_CLASS |
    Pattern.CASE_INSENSITIVE
  );

  /**
   * Pattern matching to the PostgreSQL create view command header.
   * The pattern captures the view name into group "view".
   */
  public static final Pattern CREATE_VIEW_PATTERN = Pattern.compile(
    "^\\s*CREATE(?:\\s+OR\\s+REPLACE)\\s+VIEW\\s+(?<view>\\w+)\\s+AS\\s+",
    Pattern.UNICODE_CASE |
    Pattern.UNICODE_CHARACTER_CLASS |
    Pattern.CASE_INSENSITIVE
  );

  /**
   * Drop the table pattern.
   * The pattern captures the table name into group "table".
   */
  public static final Pattern DROP_TABLE_PATTERN = Pattern.compile(
    "^\\s*DROP\\sTABLE(?:\\s+IF\\s+EXISTS)?\\s+(?<table>\\w+)\\s+",
    Pattern.UNICODE_CASE |
    Pattern.UNICODE_CHARACTER_CLASS |
    Pattern.CASE_INSENSITIVE
  );

  /**
   * Drop the view pattern.
   * The pattern captures the view name into group "view".
   */
  public static final Pattern DROP_VIEW_PATTERN = Pattern.compile(
    "^\\s*DROP\\sVIEW(?:\\s+IF\\s+EXISTS)?\\s+(?<view>\\w+)\\s+",
    Pattern.UNICODE_CASE |
    Pattern.UNICODE_CHARACTER_CLASS |
    Pattern.CASE_INSENSITIVE
  );

  /**
   * The regular expression matching to the insert pattern.
   * The pattern returns the target table in group "table", and the inserted
   * fields as group "fields".
   */
  public static final Pattern INSERT_ROWS_PATTERN = Pattern.compile(
    "^\\s*INSERT\\s+INTO\\s+(?<table>\\w+)\\s*\\((?<fields>\\w+(?:,\\s*\\w+)*)\\)",
    Pattern.UNICODE_CASE |
    Pattern.UNICODE_CHARACTER_CLASS |
    Pattern.CASE_INSENSITIVE
  );

  /**
   * Class representing a pattern entry of the SQL types.
   */
  public static class PatternEntry {

    private Pattern pattern;
    private String memberGroup;
    private Consumer<String> addMemberNames;
    private boolean isCreatePattern = false;

    /**
     * Create a pattern entry matching a pattern with a capturing
     * group, whose values are stored into member naems.
     * @param pattern The pattern. If undefined, the entry does nothing.
     * @param memberGroup The member group. If undefined, the pattern entry
     * does not store member names.
     * @param memberNames The member names list into which the matching
     * members are stored. if undefined, the entries cannot be stored.
     */
    public PatternEntry(
      Pattern pattern,
      String memberGroup,
      List<String> memberNames
    ) {
      this(pattern, memberGroup, memberNames, true);
    }

    /**
     * Create a pattern entry matching a pattern with a capturing
     * group, whose values are stored into member naems.
     * @param pattern The pattern. If undefined, the entry does nothing.
     * @param memberGroup The member group. If undefined, the pattern entry
     * does not store member names.
     * @param memberNames The member names list into which the matching
     * members are stored. if undefined, the entries cannot be stored.
     * @param isCreatePattern Is the created pattern create pattern.
     */
    public PatternEntry(
      Pattern pattern,
      String memberGroup,
      List<String> memberNames,
      boolean isCreatePattern
    ) {
      this(
        pattern,
        memberGroup,
        memberNames == null
          ? null
          : (String name) -> {
            if (name != null && !memberNames.contains(name)) {
              memberNames.add(name);
            }
          },
        isCreatePattern
      );
    }

    /**
     * Create a pattern entry for a patter with a member group using given consumer
     * to handle the matched member group on successful match.
     * @param pattern The pattern matching to a start of a SQL command.
     * @param memberGroup The member group name. If undefined, no member group exist.
     * @param addMemberNames The consumer peforming the adding of a member group value
     * to the member groups.
     * @param isCreatePattern Is the pattern a patttern expressing a create command.
     */
    public PatternEntry(
      Pattern pattern,
      String memberGroup,
      Consumer<String> addMemberNames,
      boolean isCreatePattern
    ) {
      this.pattern = pattern;
      this.memberGroup = memberGroup;
      this.isCreatePattern = isCreatePattern;
      this.addMemberNames = addMemberNames;
    }

    /**
     * Does the given line match with the pattern.
     * The matching member group will be added to the member
     * names, if it is defined.
     * @param line The matched line.
     * @return True, if and only if the line matches the pattern.
     */
    public boolean matches(String line) {
      if (this.pattern == null || line == null) {
        return false;
      }
      Matcher matcher = this.pattern.matcher(line);
      if (matcher.matches()) {
        addMemberName(matcher);
        return true;
      } else {
        return false;
      }
    }

    /**
     * Add member name tot eh member names.
     * @param matcher The matcher matching the pattern.
     * @return True, if and only if a member name was added
     * to the member names.
     */
    public boolean addMemberName(Matcher matcher) {
      if (
        memberGroup != null &&
        addMemberNames != null &&
        matcher != null &&
        matcher.matches()
      ) {
        try {
          String name = matcher.group(memberGroup);
          if (name == null) {
            return false;
          } else {
            this.addMemberNames.accept(name);
            return true;
          }
        } catch (IllegalStateException | IllegalArgumentException ex) {
          // The group did nto exist.
          return false;
        }
      } else {
        return false;
      }
    }

    /**
     * Is the current pattern entry create pattern entry.
     * @return True, if and only if the pattern entry matches to a SQL command
     * creating something.
     */
    public boolean isCreatePattern() {
      return this.isCreatePattern;
    }
  }

  /**
   * The Patterns matching various SQL command start rows.
   * @return The list of Patterns matching an SQL command start.
   */
  public List<Pattern> getPatterns() {
    return Arrays.asList(
      CREATE_TABLE_PATTERN,
      CREATE_VIEW_PATTERN,
      INSERT_ROWS_PATTERN
    );
  }

  /**
   * Get the SQL pattern entries used to parse the SQL table definition.
   * @return The list of apttern entries in the order of testing.
   */
  public List<PatternEntry> getSqlPatterns() {
    return Arrays.asList(
      new PatternEntry(CREATE_TABLE_PATTERN, "table", this.tableNames),
      new PatternEntry(CREATE_VIEW_PATTERN, "view", this.viewNames),
      new PatternEntry(
        INSERT_ROWS_PATTERN,
        "table",
        this.populatedTables,
        false
      )
    );
  }

  /**
   * Read the database definition and create entry for create, drop, and populate
   * SQL arrays for it.
   * @param reader The reader used to acquire the SQL commands.
   * @param logger The logger used to log the operations.
   * @throws IOException The operation fails due Input/Output exception.
   */
  private void readDatabaseDefinition(
    BufferedReader reader,
    PrintStream logger
  ) throws IOException {
    StringBuilder createCommands = new StringBuilder();
    StringBuilder insertCommands = new StringBuilder();
    StringBuilder currentBuilder = createCommands;
    List<String> oldTableNames = new ArrayList<>(tableNames);
    List<String> oldViewNames = new ArrayList<>(viewNames);
    List<String> dropCommands = new ArrayList<>();
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        // Checking if it contains create table command.
        for (PatternEntry pattern : getSqlPatterns()) {
          if (pattern.matches(line)) {
            if (pattern.isCreatePattern()) {
              currentBuilder = createCommands;
            } else {
              currentBuilder = insertCommands;
            }
          }
        }
        // Appending the command to the SQL commands.
        currentBuilder.append(line);
        currentBuilder.append("\n");
      }
      reader.close();
    } catch (IOException ioe) {
      // The reading of the resource failed due IO Exception.
      log(System.err, "IOException: %s", ioe);
    }
    log(
      logger,
      "%n====================%nCreate Tables: %s%n===============%n",
      String.join(
        ", ",
        this.tableNames.stream()
          .filter(name -> (!oldTableNames.contains(name)))
          .toList()
      )
    );
    log(
      logger,
      "%n====================%nCreate Views: %s%n===============%n",
      String.join(
        ", ",
        this.viewNames.stream()
          .filter(name -> (!oldViewNames.contains(name)))
          .toList()
      )
    );
    log(logger, "SQL Commands:%n%s%n", createCommands.toString());
    log(logger, "====================================%n");

    //////////////////////////////////////////
    // Creating the entries to the SQL lists.
    //////////////////////////////////////////

    // Add create commands to the create command list, if any command was found.
    // Othwerise, add an undefined value.
    this.createSqlCommands.add(
        createCommands.toString().isBlank() ? null : createCommands.toString()
      );

    // Add populating commands to the populate command list, if any populate command
    // was found. Otherwise, adding an undefiend value.
    this.populateTablesSql.add(
        insertCommands.toString().isBlank() ? null : insertCommands.toString()
      );

    // Generating drop tables sql.
    this.tableNames.forEach(tableName -> {
        if (!oldTableNames.contains(tableName)) {
          dropCommands.add(
            String.format("DROP TABLE IF EXISTS %s CASCADE;%n", tableName)
          );
        }
      });
    this.viewNames.forEach(viewName -> {
        if (!oldViewNames.contains(viewName)) {
          dropCommands.add(
            String.format("DROP VIEW IF EXISTS %s CASCADE;%n", viewName)
          );
        }
      });
    this.dropTablesSql.add(
        dropCommands.isEmpty() ? null : String.join("", dropCommands)
      );
  }

  /**
   * Create a database using the definitions acquired from the given resources.
   * @param resourceNames The SQL command resource file names.
   */
  public Database(String... resourceNames) {
    PrintStream logger = System.err;

    // Loading the SQL structures and building the list of tables and views.
    for (String resourceName : resourceNames) {
      InputStream stream = getClass().getResourceAsStream(resourceName);
      if (stream == null) {
        log(logger, "%nCould not read resource %s%n", resourceName);
      } else {
        log(logger, "Reading create tables resources from %s%n", resourceName);
        BufferedReader reader = new BufferedReader(
          new InputStreamReader(stream)
        );
        try {
          readDatabaseDefinition(reader, logger);
        } catch (IOException e) {
          log(
            logger,
            "Reading database defintion of resource %s failed due %s",
            resourceName,
            e
          );
        }
      }
    }
  }

  /**
   * Create a database using given list of URIs as database definition.
   * @param sqlCommandFiles The SQL command URI list.
   */
  public Database(java.net.URI... sqlCommandFiles) {
    PrintStream logger = System.err;
    for (java.net.URI uri : sqlCommandFiles) {
      if (uri != null) {
        try {
          BufferedReader reader = new BufferedReader(
            new InputStreamReader(uri.toURL().openStream())
          );
          readDatabaseDefinition(reader, logger);
        } catch (IOException ioe) {
          log(
            logger,
            "Reading database definition of URI %s failed due %s",
            uri.toString(),
            ioe.toString()
          );
        }
      }
    }
  }

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
    return this.tableNames;
  }

  /**
   * Get the view names of the database.
   * @return The list of the view names of the database.
   */
  public List<String> getViewNames() {
    return this.viewNames;
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
    // Wait to continue
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
   * Populate database tables.
   * @param index The index of the initialization file.
   * @param connection The connetion to the database.
   * @param logger The logger logging the results.
   * @return True, if and only if the tables was populated.
   * @throws SQLException The population failed due SQL exception.
   */
  public boolean populate(int index, Connection connection, PrintStream logger)
    throws SQLException {
    if ((index >= 0) && (index < this.populateTablesSql.size())) {
      String sql = this.populateTablesSql.get(index);
      if (sql != null) {
        Statement stmt = connection.createStatement();
        stmt.execute(sql);
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * Create database entities.
   * @param index The index of the initialization file.
   * @param connection The connetion to the database.
   * @param logger The logger logging the results.
   * @return True, if and only if something was created.
   * @throws SQLException The creation failed due SQL exception.
   */
  public boolean create(int index, Connection connection, PrintStream logger)
    throws SQLException {
    if (index >= 0 && index < this.createSqlCommands.size()) {
      String sql = this.createSqlCommands.get(index);
      log(logger, "Creating from source %d%n", index);
      log(
        logger,
        "%n==CREATE COMMANDS==================%n%s%n=================%n",
        sql
      );
      if (sql != null) {
        Statement stmt = connection.createStatement();
        stmt.execute(sql);
        return populate(index, connection, logger) || true;
      } else {
        return false;
      }
    } else {
      return false;
    }
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
  public boolean create(Connection connection, PrintStream logger) {
    try {
      boolean result = false;
      connection.createStatement().execute("START TRANSACTION");
      drop(connection, System.err);
      for (int i = 0; i < this.createSqlCommands.size(); i++) {
        result |= create(i, connection, logger);
      }
      connection.createStatement().execute("COMMIT");
      return result;
    } catch (SQLException sqle) {
      // Rollback the transaction
      log(
        logger,
        "%n%n===============================%nCreation failed: %s%n",
        sqle
      );
      try {
        connection.createStatement().execute("ROLLBACK");
      } catch (SQLException ignored) {}
      return false;
    }
  }

  /**
   * Drops the Database of the dune rest.
   * @param connection The connection to the database.
   * @param logger The logger used to log the operations.
   */
  public boolean drop(Connection connection, PrintStream logger)
    throws SQLException {
    try {
      connection.createStatement().execute("START TRANSACTION");
      Statement dropStmt = connection.createStatement();
      String sqlFormat = "DROP TABLE IF EXISTS %s CASCADE;";
      log(
        logger,
        "%nDROPPING TABLES: %s%n",
        String.join(", ", this.getTableNames())
      );
      // TODO: Reverse the order of dropping tables dropping from last to first.
      for (String tableName : getTableNames()) {
        String sql = String.format(sqlFormat, tableName);
        log(logger, "Dropping table %s: %s%n", tableName, sql);
        if (dropStmt.executeUpdate(sql) >= 0) {
          log(System.err, "Table %s dropped%n", tableName);
        } else {
          log(System.err, "Table %s not dropped%n", tableName);
        }
      }

      log(
        logger,
        "%nDROPPING VIEWS: %s%n",
        String.join(", ", this.getViewNames())
      );
      sqlFormat = "DROP TABLE IF EXISTS %s CASCADE;";
      // TODO: Reverse the order of dropping views dropping from last to first.
      for (String viewName : getViewNames()) {
        String sql = String.format(sqlFormat, viewName);
        log(logger, "Dropping view %s: %s%n", viewName, sql);
        if (dropStmt.executeUpdate(sql) > 0) {
          log(System.err, "View %s dropped%n", viewName);
        } else {
          log(System.err, "View %s not dropped%n", viewName);
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
