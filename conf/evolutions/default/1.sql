# Log schemma

# --- !Ups

CREATE TABLE log (
  id int(11) UNSIGNED NOT NULL,
  accesstime datetime NOT NULL,
  url varchar(1024) NOT NULL DEFAULT '',
  ip varchar(254) NOT NULL DEFAULT '',
  USER int(11) DEFAULT NULL
)


# --- !Downs