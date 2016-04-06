--- Log schemma

--- !Ups

CREATE TABLE log (
  id INTEGER NOT NULL,
  accesstime TIMESTAMP  NOT NULL,
  url varchar(1024) NOT NULL DEFAULT '',
  ip varchar(254) NOT NULL DEFAULT '',
  USER INTEGER DEFAULT NULL
)


--- !Downs