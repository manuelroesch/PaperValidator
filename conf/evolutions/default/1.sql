# --- Created by Slick DDL

# --- !Ups

create table "USERS" ("NAME" VARCHAR(254) NOT NULL,"ID" SERIAL NOT NULL PRIMARY KEY);
create table "LOG" ("id" SERIAL NOT NULL PRIMARY KEY, "accesstime" TIMESTAMP NOT NULL, "url" VARCHAR(1024) NOT NULL, "ip" VARCHAR(254) NOT NULL);

# --- !Downs

drop table "USERS";
drop table "LOG";

