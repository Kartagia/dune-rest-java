package com.kautiainen.antti.utils.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;

/**
 * Class creating the database of the Dune Character Storage.
 */
public class CreateDatabase {

  /**
   * The simple identifier pattern only accepting unquoted SQL identifiers.
   */
  public static final Pattern IDENTIFIER_PATTERN = Pattern.compile(
    "\\p{L}[\\w]*",
    Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS
  );

  /**
   * Pattern matching to the list of table names. The names are stored
   * to the group <code>names</code>.
   */
  public static final Pattern TABLE_NAMES_PATTERN = Pattern.compile(
    "(?<names>" +
    "(?:" +
    IDENTIFIER_PATTERN.toString() +
    ")" +
    "(?:,\\s+" +
    IDENTIFIER_PATTERN.toString() +
    ")*" +
    ")",
    Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE
  );

  /**
   * Pattern matching to single identifier name.
   * The name is stored into the group <code>name</code>.
   */
  public static final Pattern NAME_PATTERN = Pattern.compile(
    "(?<name>" + IDENTIFIER_PATTERN.toString() + ")",
    Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE
  );

  /**
   * The SQL command pattern for dropping one or more views.
   */
  public static final Pattern DROP_VIEW_SQL_PATTERN = Pattern.compile(
    "^" +
    "drop\\s+view(?:\\s+if\\s+exists)?" +
    "\\s+" +
    TABLE_NAMES_PATTERN.toString() +
    "\\s+;\\s*$",
    Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE
  );

  public static final Pattern VALUE_PATTERN = Pattern.compile(
    "(?:\"[^\"]*\"|[+-]?\\d+(?:\\.\\d+)?)"
  );

  public static final Pattern INSERT_SQL_PATTERN = Pattern.compile(
    "^" +
    "insert into " +
    TABLE_NAMES_PATTERN +
    "\\((?<fields>" +
    NAME_PATTERN +
    "(?:,\\s+" +
    NAME_PATTERN +
    ")*" +
    ")?\\) VALUES \\((?<value>" +
    VALUE_PATTERN +
    ")\\)" +
    "$",
    Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE
  );

  /**
   * The SQL command pattern for creating a single veiw.
   */
  public static final Pattern CREATE_VIEW_SQL_PATTERN = Pattern.compile(
    "^" +
    "create" +
    "(?<replaces>\\s+or\\s+replace)?" +
    "(?<temporary>\\s+temp(?:orary)?)?" +
    "(?<recursive>\\s+recursive)?" +
    "view" +
    "\\s+" +
    NAME_PATTERN.toString() +
    "\\s+as\\s+" +
    "(?<query>select\\s+.*?)\\s*;?\\s*$",
    Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE
  );

  /**
   * The SQL command pattern for creating a table.
   */
  public static final Pattern CREATE_TABLE_SQL_PATTERN = Pattern.compile(
    "^" +
    "create" +
    "(?:" +
    "(?:\\s+" +
    "(?<location>local|global)?" +
    ")?" +
    "(?<temporary>\\s+temp(?:orary)?)?" +
    ")?" +
    "\\s+table" +
    "\\s+(?<createNew>if\\s+not\\s+exists\\s+)?" +
    "\\s+" +
    NAME_PATTERN,
    Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE
  );

  /**
   * The SQL command pattern for dropping one or more table.
   */
  public static final Pattern DROP_TABLE_SQL_PATTERN = Pattern.compile(
    "^" +
    "drop\\s+table" +
    "(?:\\s+if\\s+exists)?" +
    "\\s+" +
    TABLE_NAMES_PATTERN.toString() +
    "\\s*" +
    "(?:\\s+(?<propagation>cascade|restrict))?" +
    "\\s*;\\s*?$",
    Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE
  );

  public static final Pattern CONDITIONS_PATTERN = Pattern.compile(
    "(?<conditions>.*?)\\s*",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
  );

  /**
   * Pattern matching the where clause.
   * The condition of the clause is stored into the group <code>conditions</code>.
   */
  public static final Pattern WHERE_PATTERN = Pattern.compile(
    "^where\\s+" + CONDITIONS_PATTERN.toString() + "$",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
  );

  /**
   * Pattern matching a having clause.
   * The condition of the clause is stored into the group <code>conditions</code>.
   */
  public static final Pattern HAVING_PATTERN = Pattern.compile(
    "^having\\s+" + CONDITIONS_PATTERN.toString() + "$",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
  );

  /**
   * Pattern matching a grouping of columns.
   * The names of the columns are stored into <code>names</code> group.
   */
  public static final Pattern GROUP_PATTERN = Pattern.compile(
    "^GROUP\\s+BY\\s+" + TABLE_NAMES_PATTERN.toString() + "\\s*$",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
  );

  /**
   * Pattern matching a single key order definition.
   * The name of the ordering column is stored into the group <code>name</code>,
   * and the optional order string is stored into the group <code>order</code>.
   */
  public static final Pattern ORDER_PATTERN = Pattern.compile(
    "^ORDER\\s+BY\\s+" +
    NAME_PATTERN.toString() +
    "(?<order>desc|asc)?" +
    "\\s*$",
    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
  );

  /**
   * List of SQL commands creating the tables of the database.
   */
  private List<String> tables = new ArrayList<>();

  /**
   * List of SQL commands creating the views of the database after tables
   *  has been created.
   */
  private List<String> views = new ArrayList<>();

  /**
   * List of SQL commands initializing the table contents after the
   *  views has been created.
   */
  private List<String> tableInitializations = new ArrayList<>();

  /**
   * Create an empty database creator without tables, views, or initializations.
   */
  public CreateDatabase() {}

  /**
   * Create database with given table and view creation and table
   * intialization.
   * @param tableCreationCommands The list of table creation commands.
   * @param viewCreationCommadns The view creation commands.
   * @param tableInitializationCOmmands The commands populating the created tables.
   */
  public CreateDatabase(
    List<String> tableCreationCommands,
    List<String> viewCreationCommands,
    List<String> tableInitializationCommands
  ) {
    if (
      validTableCreationCommands(tableCreationCommands) &&
      validViewCreationCommands(viewCreationCommands) &&
      validTableInitializationCommands(tableInitializationCommands)
    ) {
      this.tables.addAll(
          Optional
            .ofNullable(tableCreationCommands)
            .orElse(Collections.emptyList())
        );
      this.views.addAll(
          Optional
            .ofNullable(viewCreationCommands)
            .orElse(Collections.emptyList())
        );
      this.tableInitializations.addAll(
          Optional
            .ofNullable(tableInitializationCommands)
            .orElse(Collections.emptyList())
        );
    } else if (!validTableCreationCommands(tableCreationCommands)) {
      throw new IllegalArgumentException("Invalid table creation commands!");
    } else if (!validViewCreationCommands(viewCreationCommands)) {
      throw new IllegalArgumentException("Invalid view creatoin commands");
    } else {
      throw new IllegalArgumentException(
        "Invalid table initialization commands"
      );
    }
  }

  /**
   * Test validity of the database creationcommands.
   * @param tableInitializationCommands The database creation commands.
   * @return True, if and only if the databse initialization commands
   * are valid.
   */
  private boolean validTableInitializationCommands(
    List<String> tableInitializationCommands
  ) {
    return (
      tableInitializationCommands == null ||
      tableInitializationCommands
        .stream()
        .allMatch(sql ->
          (
            sql != null &&
            (
              CREATE_TABLE_SQL_PATTERN.matcher(sql).matches() ||
              Pattern.compile("").matcher(sql).matches()
            )
          )
        )
    );
  }

  public boolean validViewCreationCommands(List<String> viewCreationCommands) {
    return viewCreationCommands
      .stream()
      .allMatch((String command) ->
        (
          command != null &&
          (
            CREATE_VIEW_SQL_PATTERN.matcher(command).matches() ||
            DROP_VIEW_SQL_PATTERN.matcher(command).matches()
          )
        )
      );
  }

  public boolean validTableCreationCommands(
    List<String> tableCreationCommands
  ) {
    return tableCreationCommands
      .stream()
      .allMatch((String command) ->
        (
          command != null &&
          (
            CREATE_TABLE_SQL_PATTERN.matcher(command).matches() ||
            DROP_TABLE_SQL_PATTERN.matcher(command).matches()
          )
        )
      );
  }

  /**
   * A view definition represents a single view.
   */
  public static interface ViewDefinition {
    /**
     * The message of an invalid query name.
     */
    public static final String INVALID_QUERY_NAME_MESSAGE =
      "Invalid query name";

    /**
     * The message of an invalid query string.
     */
    public static final String INVALID_QUERY_STRING_MESSAGE =
      "Invalid query string";

    /**
     * Get the name of the view.
     * @return The view name.
     */
    public String getViewName();

    /**
     * Get the command creating the view.
     * @return A string containing the SQL command creating the view.
     */
    public String getCreateView();

    /**
     * Get the command removing the view.
     * @return A string containing the SQL command removing the view.
     */
    public String getRemoveView();

    /**
     * Create a view definition from query.
     * @param name The name of the created view.
     * @param query The query string.
     * @return
     * @throws IllegalArgumentException Either the name or query was invalid.
     */
    static ViewDefinition create(String name, String query)
      throws IllegalArgumentException {
      if (validQueryName(name) && validQueryString(query)) {
        return new ViewDefinition() {
          @Override
          public String getViewName() {
            return name;
          }

          @Override
          public String getCreateView() {
            StringBuilder sql = new StringBuilder("CREATE OR REPLACE VIEW ");

            sql.append(getViewName());
            sql.append(" AS ");
            sql.append(query);
            return sql.toString();
          }

          @Override
          public String getRemoveView() {
            return "DROP VIEW IF EXISTS " + getViewName();
          }
        };
      } else if (validQueryName(name)) {
        // The query was invalid
        throw new IllegalArgumentException(INVALID_QUERY_STRING_MESSAGE);
      } else {
        // The query name was invalid.
        throw new IllegalArgumentException(INVALID_QUERY_NAME_MESSAGE);
      }
    }

    /**
     * Test validity of the database query name.
     * @param name The tested query name.
     */
    public static boolean validQueryName(String name) {
      return (
        name != null &&
        (
          Pattern.matches("^[a-zA-Z][\\w]*", name) ||
          Pattern.matches("\"(?<content>)(?:[^\"]+|\"\")*)\"", name)
        )
      );
    }

    /**
     * Test validity of the database query.
     * @param query The tested query.
     * @return True, if and only if the View Definition considers the query valid.
     */
    public static boolean validQueryString(String query) {
      return query != null && query.toUpperCase().startsWith("SELECT ");
    }

    /**
     * Get prefixed string containing the string representation of the entries joined with delimiter
     * with prefix, if the entries is not empty. For empty entries, an empty string is returned.
     * @param prefix The prefix added to the string representation of the entries, if the entries is not empty.
     * @param delimiter The delimiter The delimiter used to combine entries. Undefined value defaults to single space(<code>" "</code>).
     * @param undefinedValue The value used to represent undefined entry of the entries. An undefined or empty value indicates
     *  the undefined values are ignored.
     * @param entries The entries. An undefined value defaults to an empty list.
     * @return Either an empty string, or the string starting with prefix following the string representations of the
     *  entries separated with delimiter.
     */
    static String prefixedToStringOrEmpty(
      String prefix,
      String delimiter,
      Optional<?> undefinedValue,
      Iterable<?> entries
    ) {
      final StringBuilder result = new StringBuilder();

      if (entries != null) {
        final String usedDelimiter = Optional.ofNullable(delimiter).orElse(" ");
        entries.forEach(entry -> {
          if (entry == null) {
            // Undefined result.
            if (undefinedValue != null && undefinedValue.isPresent()) {
              // Adding the undefined value to the result with delimiter.
              if (!result.isEmpty()) {
                result.append(usedDelimiter);
              }
              result.append(undefinedValue.get().toString());
            }
          } else {
            // Definited result.
            if (!result.isEmpty()) {
              result.append(usedDelimiter);
            }
            result.append(entry.toString());
          }
        });
      }

      // Adding prefix before returning result, if applicable.
      if (!result.isEmpty()) {
        result.insert(0, Optional.ofNullable(prefix).orElse(""));
      }
      return result.toString();
    }

    /**
     * Serializable function implements serializable interface.
     */
    static interface SerializableFunction<SOURCE, TARGET>
      extends Function<SOURCE, TARGET>, Serializable {
      /**
       * Create a new serializable function.
       * @param <SOURCE> The source type of the function.
       * @param <TARGET> The target type of the function.
       * @param function The function implementation stored in serializable.
       * @return The serializable function performing the given function operation.
       */
      static <SOURCE, TARGET> SerializableFunction<SOURCE, TARGET> from(
        Function<? super SOURCE, ? extends TARGET> function
      ) {
        return new SerializableFunction<SOURCE, TARGET>() {
          @Override
          public TARGET apply(SOURCE source) {
            return function.apply(source);
          }
        };
      }
    }

    /**
     * Create a single table view definition.
     * @param tableName The table name of the queried table. If undefined, no table
     *  is involved.
     * @param columnDefinitions The column definitions of the created view.
     * @param tables The list of tables the view uses.
     * @param constraints The constraints of the query limiting the query.
     * @return Created view definition.
     */
    @SuppressWarnings("unchecked")
    static ViewDefinition create(
      String tableName,
      List<String> columnDefinitions,
      List<String> tables,
      List<String> constraints
    ) {
      StringBuilder query = new StringBuilder("SELECT ");
      final StringBuilder columns = new StringBuilder();
      final StringBuilder from = new StringBuilder();
      final StringBuilder where = new StringBuilder();
      final StringBuilder grouping = new StringBuilder();
      final StringBuilder sorting = new StringBuilder();
      final StringBuilder having = new StringBuilder();

      int index = 0;
      final int PATTERN = index++;
      final int TARGET = index++;
      final int DELIMITER = index++;
      final int VALUE_FUNCTION = index++;

      Function<String, SerializableFunction<Matcher, String>> getGroup = (String groupName) ->
        ((Matcher matcher) -> (matcher.group(groupName)));

      constraints
        .stream()
        .forEach(condition -> {
          if (condition != null) {
            Arrays
              .asList(
                (List<Object>) Arrays.asList(
                  (Object) WHERE_PATTERN,
                  where,
                  " AND ",
                  getGroup.apply("conditions")
                ),
                (List<Object>) Arrays.asList(
                  (Object) GROUP_PATTERN,
                  grouping,
                  ", ",
                  getGroup.apply("names")
                ),
                (List<Object>) Arrays.asList(
                  (Object) HAVING_PATTERN,
                  having,
                  " AND ",
                  getGroup.apply("conditions")
                ),
                (List<Object>) Arrays.asList(
                  (Object) ORDER_PATTERN,
                  sorting,
                  ", ",
                  getGroup.apply("columns")
                )
              )
              .stream()
              .filter((List<Object> target) ->
                (
                  target != null &&
                  target instanceof List<?> list &&
                  ((Pattern) list.get(PATTERN)).matcher(condition).matches()
                )
              )
              .findFirst()
              .ifPresent((List<Object> target) -> {
                StringBuilder builder = (StringBuilder) target.get(TARGET);
                if (!builder.isEmpty()) {
                  builder.append((CharSequence) (target.get(DELIMITER)));
                }
                Matcher matcher =
                  ((Pattern) target.get(PATTERN)).matcher(condition);
                builder.append(
                  (
                    (Function<Matcher, String>) target.get(VALUE_FUNCTION)
                  ).apply(matcher)
                    .toString()
                );
              });
          }
        });

      if (columnDefinitions != null) {
        query.append(String.join(", ", columnDefinitions));
      }

      query.append(columns.toString());
      if (!from.isEmpty()) {
        query.append("FROM " + String.join(", ", from));
      }
      if (!where.isEmpty()) {
        query.append("WHERE " + where.toString());
      }
      if (!grouping.isEmpty()) {
        query.append("GROUP BY " + grouping.toString());
      }
      if (!sorting.isEmpty()) {
        query.append("ORDER BY " + sorting.toString());
      }
      if (!having.isEmpty()) {}

      return new ViewDefinition() {
        @Override
        public String getViewName() {
          return tableName;
        }

        @Override
        public String getCreateView() {
          return query.toString();
        }

        @Override
        public String getRemoveView() {
          return "DROP VIEW " + getViewName();
        }
      };
    }

    /**
     * Create a single table view definition.
     * @param tableName The table name of the queried table. If undefined, no table
     *  is involved.
     * @param columnDefinitions The column definitions of the created view.
     * @param constraints The constraints of the query limiting the query.
     * @return Created view definition.
     */
    static ViewDefinition create(
      String tableName,
      List<String> columnDefinitions,
      List<String> constraints
    ) {
      return create(
        tableName,
        columnDefinitions,
        Collections.emptyList(),
        constraints
      );
    }
  }

  /**
   * The table definition.
   */
  public static interface TableDefinition {
    /**
     * Check validity of the table name.
     * @param name The tested table name.
     * @return True, if and only if the tabel name is valid table name.
     */
    public static boolean validTableName(String name) {
      return validDatabaseIdentifier(name);
    }

    /**
     * Get the name of the table.
     * @return The table name.
     */
    public String getTableName();

    /**
     * Get the SQL command creating the table.
     * @return A string containing the SQL command creating the table.
     */
    public String getCreateTable();

    /**
     * Get the command removing the table.
     * @return A string containing the SQL command removing the table.
     */
    public String getRemoveTable();
  }

  /**
   * Get the table creation commands.
   * @return The list of SQL commands creating the tables.
   */
  public List<String> getTables() {
    return this.tables;
  }

  /**
   * Test validity of a database identifier.
   * @param name The tested name.
   * @return True, if and only if the given identifier is valid database identified.
   */
  protected static boolean validDatabaseIdentifier(String name) {
    return name != null && IDENTIFIER_PATTERN.matcher(name).matches();
  }

  /**
   * Get the view creating commands.
   * @return The list of SQL commands initializing the views.
   */
  public List<String> getViews() {
    return this.views;
  }

  /**
   * Get the table initializations.
   * @return The list containing SQL commands initializing table content.
   */
  public List<String> getTableInitializations() {
    return this.tableInitializations;
  }

  /**
   * Create database to the given data source.
   * @param dataSource The data source into which the database will be created.
   * @return True, if and only if the creation of the database succeeded.
   */
  public boolean createDatabase(DataSource dataSource) {
    List<String> rollback = new ArrayList<>();
    try (Connection connection = dataSource.getConnection()) {
      final Statement statement = connection.createStatement();
      final AtomicBoolean isBatch = new AtomicBoolean(
        connection.getMetaData().supportsBatchUpdates()
      );
      final AtomicReference<Exception> exception = new AtomicReference<Exception>(
        null
      );
      Consumer<? super String> executor = executeStatement(
        statement,
        isBatch,
        exception
      );
      getTables().forEach(executor);
      if (exception.get() != null) throw exception.get(); else if (
        isBatch.get()
      ) {
        statement.executeBatch();
      }
      getTableInitializations().forEach(executor);
      if (exception.get() != null) throw exception.get(); else if (
        isBatch.get()
      ) {
        statement.executeBatch();
      }
      getViews().forEach(executor);
      if (exception.get() != null) {
        throw exception.get();
      }
      if (isBatch.get()) {
        statement.executeBatch();
      }
      return true;
    } catch (Exception e) {
      // Exception occured. Rollback.
      rollback.forEach((String sql) -> {
        try (Connection connection = dataSource.getConnection()) {
          Statement statement = connection.createStatement();
          statement.executeUpdate(sql);
        } catch (SQLException sqle) {
          //TODO: Debut telling rollback failed.
        }
      });
      return false;
    }
  }

  /**
   * Get the consumer excecuting the statement given to it as SQL string.
   *
   * The created consumer would would impolement following details:
   *
   * As long the batch mode is possible, the statement is added to the batch
   * list of the statement. If the bactching failes, the batch mode is set to
   * false.
   *
   * If any error occurs, or the given exception is set to non-null value, the
   * rest of the statements are ignored.
   *
   * @param statement The statement executing the statement.
   * @param isBatch The atomic boolean determining whether the batch execution is used or not.
   * @param exception The atomic exception used to determine the exception of the current execution.
   * @return The SQL string consumer consuming a SQL String and executing it.
   */
  private Consumer<? super String> executeStatement(
    final Statement statement,
    final AtomicBoolean isBatch,
    final AtomicReference<Exception> exception
  ) {
    return tableSql -> {
      if (exception.get() == null) {
        if (isBatch.get()) {
          try {
            // Executing the batch.
            statement.addBatch(tableSql);
          } catch (SQLException e) {
            // Batch execution was not accepted
            isBatch.set(false);
            exception.set(e);
          }
        } else {
          try {
            statement.execute(tableSql);
          } catch (SQLException e) {
            exception.set(e);
          }
        }
      }
    };
  }
}
