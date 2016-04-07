# --- Created by Slick DDL

# --- !Ups

CREATE TABLE answer (
  id serial NOT NULL,
  question_id bigint NOT NULL,
  user_id bigint NOT NULL,
  time timestamp(0) NOT NULL,
  answer_json text NOT NULL,
  expected_output_code bigint NOT NULL,
  accepted boolean NOT NULL DEFAULT '0'
) ;

CREATE TABLE assets (
  id serial NOT NULL,
  hash_code varchar(255) NOT NULL,
  byte_array bytea NOT NULL,
  content_type varchar(255) NOT NULL,
  filename varchar(300) NOT NULL
) ;

CREATE TABLE batch (
  id serial NOT NULL,
  allowed_answers_per_turker int NOT NULL,
  uuid varchar(255) NOT NULL
) ;

CREATE TABLE log (
  id serial NOT NULL,
  accesstime timestamp(0) NOT NULL,
  url varchar(1024) NOT NULL DEFAULT '',
  ip varchar(254) NOT NULL DEFAULT '',
  users int DEFAULT NULL
) ;

CREATE TABLE permutations (
  id serial NOT NULL,
  create_time timestamp(0) NOT NULL,
  group_name varchar(255) NOT NULL,
  method_index varchar(255) NOT NULL,
  snippet_filename varchar(255) NOT NULL,
  pdf_path varchar(255) NOT NULL,
  method_on_top boolean NOT NULL,
  state bigint NOT NULL DEFAULT '0',
  excluded_step int DEFAULT '0',
  relative_height_top double precision NOT NULL,
  relative_height_bottom double precision NOT NULL,
  distanceMinIndexMax bigint NOT NULL DEFAULT '0'
) ;

CREATE TABLE question (
  id serial NOT NULL,
  batch_id bigint NOT NULL,
  html text NOT NULL,
  create_time timestamp(0) NOT NULL,
  uuid varchar(255) NOT NULL,
  permutation bigint NOT NULL,
  secret varchar(1024) NOT NULL
) ;

CREATE TABLE question2assets (
  id serial NOT NULL,
  question_id bigint NOT NULL,
  asset_id bigint NOT NULL
) ;

CREATE TABLE users (
  id serial NOT NULL,
  turker_id varchar(255) NOT NULL,
  first_seen_date_time timestamp(0) NOT NULL
) ;


ALTER TABLE answer
ADD PRIMARY KEY (id);

ALTER TABLE assets
ADD PRIMARY KEY (id);

ALTER TABLE batch
ADD PRIMARY KEY (id);

ALTER TABLE log
ADD PRIMARY KEY (id);

ALTER TABLE permutations
ADD PRIMARY KEY (id);

ALTER TABLE question
ADD PRIMARY KEY (id);

ALTER TABLE question2assets
ADD PRIMARY KEY (id);

ALTER TABLE users
ADD PRIMARY KEY (id);

ALTER TABLE answer
ADD CONSTRAINT answer_ibfk_1 FOREIGN KEY (question_id) REFERENCES question (id),
ADD CONSTRAINT answer_ibfk_2 FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE question
ADD CONSTRAINT question_ibfk_1 FOREIGN KEY (batch_id) REFERENCES batch (id),
ADD CONSTRAINT question_ibfk_2 FOREIGN KEY (permutation) REFERENCES permutations (id);

ALTER TABLE question2assets
ADD CONSTRAINT question2assets_ibfk_1 FOREIGN KEY (question_id) REFERENCES question (id),
ADD CONSTRAINT question2assets_ibfk_2 FOREIGN KEY (asset_id) REFERENCES assets (id);

# --- !Downs



