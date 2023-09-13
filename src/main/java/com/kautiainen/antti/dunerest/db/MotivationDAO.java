package com.kautiainen.antti.dunerest.db;

import com.kautiainen.antti.dunerest.model.Motivation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

public class MotivationDAO implements DAO<MotivationId, Motivation> {

  private DataSource source;

  /**
   * Create a new motivation data access object using a data source.
   * @param source The data source used to get the data.
   */
  public MotivationDAO(DataSource source) {
    this.source = source;
  }

  /**
   * Get the update statement to update a motivation.
   * @param connection The connection to access the database.
   * @return The prepared statement.
   * @throws IllegalStateException The command could not be prepared.
   */
  protected PreparedStatement getUpdateMotivationStatement(
    Connection connection
  ) throws IllegalStateException {
    try {
      return connection.prepareStatement(
        "UPDATE Motivation SET name=? AND description = ? WHERE id=?;"
      );
    } catch (SQLException e) {
      throw new IllegalStateException(
        "Could not prepare the update command",
        e
      );
    }
  }

  /**
   * Get the select all statement.
   * @param connection The connection to access the database.
   * @return The prepared statement.
   * @throws IllegalStateException The command could not be prepared.
   */
  protected PreparedStatement getSelectAllMotivationsStatement(
    Connection connection
  ) throws IllegalStateException {
    return getSelectAllMotivationStatement(connection, 0);
  }

  /**
   * Get the select all statement.
   * @param connection The connection to access the database.
   * @param numberOfIds The number of alternate IDs fetches.
   * @return The prepared statement.
   * @throws IllegalStateException The command could not be prepared.
   */
  protected PreparedStatement getSelectAllMotivationStatement(
    Connection connection,
    int numberOfIds
  ) throws IllegalStateException {
    try {
      return connection.prepareStatement(
        String.format(
          "SELECT * FROM Motivation%s;",
          (
            numberOfIds > 0
              ? " WHERE " +
              String.join(
                " OR ",
                java.util.Collections.nCopies(numberOfIds, "id=?")
              )
              : ""
          )
        )
      );
    } catch (SQLException e) {
      throw new IllegalStateException(
        "Could not prepare the fetch all command",
        e
      );
    }
  }

  /**
   * Get the delete statement.
   * @param connection The connection to access the database.
   * @return The prepared statement.
   * @throws IllegalStateException The command could not be prepared.
   */
  protected PreparedStatement getDeleteMotivationStatement(
    Connection connection
  ) throws IllegalStateException {
    try {
      return connection.prepareStatement("DELETE FROM Motivation WHERE id=?;");
    } catch (SQLException e) {
      throw new IllegalStateException(
        "Could not prepare the delete command",
        e
      );
    }
  }

  /**
   * Get the create statement.
   * @param connection The connection to access the database.
   * @return The prepared statement.
   * @throws IllegalStateException The command could not be prepared.
   */
  protected PreparedStatement getCreateMotivationStatement(
    Connection connection,
    boolean insertId
  ) throws IllegalStateException {
    try {
      return connection.prepareStatement(
        "INSERT INTO Motivation (name" +
        (insertId ? "id, " : "") +
        ", description) VALUES (?" +
        (insertId ? ", ?" : "") +
        ", ?);"
      );
    } catch (SQLException e) {
      throw new IllegalStateException(
        "Could not prepare the create command",
        e
      );
    }
  }

  protected Integer fetchMotivationId(String motivationName)
    throws SQLException {
    Connection conn = getSource().getConnection();
    PreparedStatement stmt = conn.prepareStatement(
      "SELECT id FROM Motivation WHERE name=?;"
    );
    stmt.setString(1, motivationName);
    ResultSet rows = stmt.executeQuery();
    if (rows.next()) {
      Integer result = rows.getInt("id");
      rows.close();
      stmt.close();
      return result;
    } else {
      rows.close();
      stmt.close();
      return null;
    }
  }

  @Override
  public Identified<MotivationId, Motivation> create(
    Identified<MotivationId, Motivation> entry
  ) throws IllegalArgumentException, IllegalStateException {
    if (
      entry == null ||
      entry.getId() == null ||
      entry.getId().getPersonId() == null
    ) {
      throw new IllegalArgumentException("Invalid motivation entry");
    }
    try {
      Motivation value = entry.getValue();
      Identified<MotivationId, Motivation> result = null;
      MotivationId id = new MotivationId();
      MotivationId entryId = entry.getId();
      id.setPersonId(entryId.getPersonId());
      Integer motivationId = fetchMotivationId(value.getName());
      if (motivationId == null) {
        // Setting the motivation id to the motivation idetnifier of the key.
        motivationId = entryId.getMotivationId();
        // Creating new motivation definition for the motivation.
        PreparedStatement stmt = getCreateMotivationStatement(
          getSource().getConnection(),
          motivationId != null
        );
        int index = 1;
        stmt.setString(index++, value.getName());
        stmt.setString(index++, (String) null);
        if (stmt.executeUpdate() > 0) {
          // Composing the result.
          ResultSet createdIds = stmt.getGeneratedKeys();
          if (createdIds.next()) {
            motivationId = createdIds.getInt("id");
          }
          createdIds.close();
        }
        stmt.close();
      } else if (
        entryId.getMotivationId() != null &&
        !Objects.equals(motivationId, entryId.getMotivationId())
      ) {
        // The motivation identifier of the key is not the motivation identifier of the motivation name.
        throw new IllegalArgumentException(
          "Invalid motivaiton identifier",
          new IllegalArgumentException(
            "The motivation identifier does not match the motivation name."
          )
        );
      }
      // Setting the motivation identifier.
      if (motivationId == null) {
        throw new IllegalStateException("Adding a new motivation failed");
      } else {
        id.setMotivationId(motivationId);
      }

      // Adding the motivation to the person motivations.
      PreparedStatement stmt = getSource()
        .getConnection()
        .prepareCall(
          "UPDATE PersonMotivations SET value = ? AND statement = ? AND challenged = ?" +
          " WHERE person_id = ? AND motivation_id = ?;"
        );
      int index = 1;
      stmt.setShort(index++, value.getValue().shortValue());
      stmt.setString(index++, value.getStatement());
      stmt.setBoolean(index++, value.isChallenged());
      stmt.setInt(index++, id.getPersonId());
      stmt.setInt(index++, id.getMotivationId());
      if (stmt.executeUpdate() > 0) {
        // The action succeeded.
        stmt.close();
        return fetch(id);
      } else {
        // Inserting the new person motivation entry.
        stmt.close();
        stmt =
          getSource()
            .getConnection()
            .prepareCall(
              "INSERT INTO PersonMotivations (value, statement, challenged, person_id, motivation_id) " +
              "VALUES (?,?,?, ?, ?);"
            );
        index = 1;
        stmt.setShort(index++, value.getValue().shortValue());
        stmt.setString(index++, value.getStatement());
        stmt.setBoolean(index++, value.isChallenged());
        stmt.setInt(index++, id.getPersonId());
        stmt.setInt(index++, id.getMotivationId());

        if (stmt.executeUpdate() > 0) {
          // Getting the result.
          stmt.close();
          return fetch(id);
        } else {
          // Insert failed.
          throw new IllegalStateException(
            "Inserting the person motivation failed"
          );
        }
      }
    } catch (SQLException sqle) {
      throw new IllegalStateException("Database error", sqle);
    }
  }

  @Override
  public boolean delete(MotivationId key)
    throws IllegalStateException, UnsupportedOperationException {
    // TODO Auto-generated method stub
    return DAO.super.delete(key);
  }

  @Override
  public boolean delete(Identified<MotivationId, Motivation> dataEntry)
    throws IllegalStateException, UnsupportedOperationException {
    // TODO Auto-generated method stub
    return DAO.super.delete(dataEntry);
  }

  @Override
  public Identified<MotivationId, Motivation> fetch(MotivationId key)
    throws IllegalStateException {
    // TODO Auto-generated method stub
    return DAO.super.fetch(key);
  }

  @Override
  public List<Identified<MotivationId, Motivation>> fetchAll(
    Collection<? extends MotivationId> identifiers
  ) throws IllegalStateException {
    try {
      PreparedStatement stmt = getSelectAllMotivationStatement(
        getSource().getConnection(),
        (identifiers == null ? 0 : identifiers.size())
      );
      List<Identified<MotivationId, Motivation>> result = new ArrayList<>();
      ResultSet rows = stmt.executeQuery();
      while (rows.next()) {
        MotivationId id = new MotivationId(
          rows.getInt("person_id"),
          rows.getInt("motivation_id")
        );
        Motivation motivation = new Motivation(
          rows.getString("name"),
          null,
          rows.getString("description")
        );

        result.add(Identified.create(id, motivation));
      }

      return result;
    } catch (SQLException sqle) {
      throw new IllegalStateException("Database error", sqle);
    }
  }

  @Override
  public DataSource getSource() {
    return source;
  }

  @Override
  public boolean update(
    MotivationId key,
    Identified<MotivationId, Motivation> newValue
  )
    throws IllegalArgumentException, IllegalStateException, UnsupportedOperationException {
    // TODO Auto-generated method stub
    return DAO.super.update(key, newValue);
  }
}
