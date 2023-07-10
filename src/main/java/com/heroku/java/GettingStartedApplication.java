package com.heroku.java;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
@Controller
public class GettingStartedApplication {

  private final DataSource dataSource;

  @Autowired
  public GettingStartedApplication(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @GetMapping("/")
  public String index() {
    return "index";
  }

  @GetMapping("/character")
  String characterView(Map<String, Object> model) {
    try (Connection connection = dataSource.getConnection()) {
      final var statement = connection.createStatement();
      //TODO: Move database creation to separate method.
      statement.executeUpdate(
        "CREATE TABLE IF NOT EXISTS motivations (" +
        "id SMALLSERIAL PRIMARY KEY, " +
        "name VARCHAR(40) NOT NULL UNIQUE, " +
        "value SMALLINT NOT NULL DEFAULT '4' " +
        ")"
      );
      //TODO: Move database creation to separate method.
      statement.executeUpdate(
        "CREATE TABLE IF NOT EXISTS skills (" +
        "id SMALLSERIAL PRIMARY KEY, " +
        "name VARCHAR(40) NOT NULL UNIQUE, " +
        ")"
      );
      statement.executeUpdate(
        "CREATE TABLE IF NOT EXISTS drives (" +
        "id SMALLSERIAL PRIMARY KEY, " +
        "name VARCHAR(40) NOT NULL UNIQUE, " +
        ")"
      );
      statement.executeUpdate(
        "CREATE TABLE IF NOT EXISTS characters (" +
        "id SMALLSERIAL PRIMARY KEY " +
        ", name VARCHAR(255) NOT NULL" +
        ", creator VARCHAR(255) NOT NULL" +
        ")"
      );
      statement.executeUpdate(
        "CREATE TABLE IF NOT EXISTS character_skills (" +
        "cid SMALLINT NOT NULL REFERENCES characters (id) ON UPDATE CASCADE ON DELETE CASCADE" +
        ", sid SMALLINT NOT NULL REFERENCES skills (id) ON UPDATE CASCADE ON DELETE CASCADE" +
        ", value SMALLINT DEFAULT 4 NOT NULL" +
        ")"
      );
      statement.executeUpdate(
        "CREATE TABLE IF NOT EXISTS character_drives (" +
        "cid SMALLINT NOT NULL REFERENCES characters (id) ON UPDATE CASCADE ON DELETE CASCADE" +
        ", did SMALLINT NOT NULL REFERENCES drives (id) ON UPDATE CASCADE ON DELETE CASCADE" +
        ", value SMALLINT DEFAULT 4 NOT NULL" +
        ")"
      );
      statement.executeUpdate(
        "CREATE TABLE IF NOT EXISTS character_drive_statements (" +
        "cid SMALLINT NOT NULL REFERENCES characters (id) ON UPDATE CASCADE ON DELETE CASCADE" +
        ", did SMALLINT NOT NULL REFERENCES drives (id) ON UPDATE CASCADE ON DELETE CASCADE" +
        ", row_id SMALLSERIAL NOT NULL" +
        ", statement VARCHAR(60) NOT NULL" +
        ", PRIMARY KEY (cid,sid,row_id)" +
        ", FOREIGN KEY character_drives (cid, did)" +
        ")"
      );

      // Performing the database operation.

      statement.executeUpdate("INSERT INTO ticks VALUES (now())");

      final var resultSet = statement.executeQuery("SELECT tick FROM ticks");
      final var output = new ArrayList<>();
      while (resultSet.next()) {
        output.add("Read from DB: " + resultSet.getTimestamp("tick"));
      }

      model.put("records", output);
      return "database";
    } catch (Throwable t) {
      model.put("message", t.getMessage());
      return "error";
    }
  }

  @GetMapping("/database")
  String database(Map<String, Object> model) {
    try (Connection connection = dataSource.getConnection()) {
      final var statement = connection.createStatement();
      statement.executeUpdate(
        "CREATE TABLE IF NOT EXISTS ticks (tick timestamp)"
      );
      statement.executeUpdate("INSERT INTO ticks VALUES (now())");

      final var resultSet = statement.executeQuery("SELECT tick FROM ticks");
      final var output = new ArrayList<>();
      while (resultSet.next()) {
        output.add("Read from DB: " + resultSet.getTimestamp("tick"));
      }

      model.put("records", output);
      return "database";
    } catch (Throwable t) {
      model.put("message", t.getMessage());
      return "error";
    }
  }

  public static void main(String[] args) {
    SpringApplication.run(GettingStartedApplication.class, args);
  }
}
