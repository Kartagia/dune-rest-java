START TRANSACTION;

CREATE TABLE IF NOT EXISTS Motivation(
  id serial PRIMARY KEY,
  name varchar(255) NOT NULL UNIQUE,
  description text DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS Skill(
  id serial PRIMARY KEY,
  name varchar(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS Focus(
  id serial PRIMARY KEY,
  name varchar(255) NOT NULL UNIQUE,
  defaultSkill int DEFAULT NULL REFERENCES Skill(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Person(
  id serial PRIMARY KEY,
  name varchar(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS PersonMotivations(
  person_id int NOT NULL REFERENCES Person(id) ON UPDATE CASCADE ON DELETE CASCADE,
  motivation_id int NOT NULL REFERENCES Motivation(id) ON UPDATE CASCADE ON DELETE CASCADE,
  value smallint NOT NULL DEFAULT '4',
  statement varchar(255) DEFAULT NULL,
  challenged boolean NOT NULL DEFAULT FALSE,
  PRIMARY KEY (person_id, motivation_id)
);

CREATE TABLE IF NOT EXISTS PersonSkills(
  person_id int NOT NULL REFERENCES Person(id) ON UPDATE CASCADE ON DELETE CASCADE,
  skill_id int NOT NULL REFERENCES Skill(id) ON UPDATE CASCADE ON DELETE CASCADE,
  value smallint NOT NULL DEFAULT '4',
  PRIMARY KEY (person_id, skill_id)
);

CREATE TABLE IF NOT EXISTS PersonFocuses(
  person_id int NOT NULL REFERENCES Person(id) ON UPDATE CASCADE ON DELETE CASCADE,
  focus_id int NOT NULL REFERENCES Focus(id) ON UPDATE CASCADE ON DELETE CASCADE,
  PRIMARY KEY (person_id, focus_id)
);

COMMIT;

