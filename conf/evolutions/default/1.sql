# Log schemma

# --- !Ups

CREATE TABLE log (
  id int CHECK (id > 0) NOT NULL,
  accesstime timestamp(0) NOT NULL,
  url varchar(1024) NOT NULL DEFAULT '',
  ip varchar(254) NOT NULL DEFAULT '',
  USER int DEFAULT NULL
) ;


# --- !Downs