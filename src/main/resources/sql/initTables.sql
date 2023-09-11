START TRANSACTION;

INSERT INTO Motivation(name)
  VALUES ('Duty'),
('Power'),
('Justice'),
('Truth'),
('Faith');

INSERT INTO Skill(name)
  VALUES ('Battle'),
('Communication'),
('Discipline'),
('Move'),
('Understand');

COMMIT;

