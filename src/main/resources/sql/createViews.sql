CREATE VIEW PersonMotivationsView AS
SELECT
  person_id,
  motivation_id,
  motivation.name AS motivation_name,
  challenged,
  statement,
  value
FROM
  PersonMotivations
  INNER JOIN Motivation ON person_id = id;

