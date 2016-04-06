# --- Created by Slick DDL

# --- !Ups

create table "USERS" ("NAME" VARCHAR(254) NOT NULL,"ID" SERIAL NOT NULL PRIMARY KEY);

# --- !Downs

drop table "USERS";

