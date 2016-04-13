# --- Created by Slick DDL

# --- !Ups

CREATE TABLE `answer` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `time` datetime NOT NULL,
  `answer_json` longtext NOT NULL,
  `expected_output_code` bigint(20) NOT NULL,
  `accepted` tinyint(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `assets` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `hash_code` varchar(255) NOT NULL,
  `byte_array` longblob NOT NULL,
  `content_type` varchar(255) NOT NULL,
  `filename` varchar(300) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `batch` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `allowed_answers_per_turker` int(11) NOT NULL,
  `uuid` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `log` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accesstime` datetime NOT NULL,
  `url` varchar(1024) NOT NULL DEFAULT '',
  `ip` varchar(254) NOT NULL DEFAULT '',
  `users` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `permutations` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL,
  `group_name` varchar(255) NOT NULL,
  `method_index` varchar(255) NOT NULL,
  `snippet_filename` varchar(255) NOT NULL,
  `pdf_path` varchar(255) NOT NULL,
  `method_on_top` tinyint(1) NOT NULL,
  `state` bigint(20) NOT NULL DEFAULT '0',
  `excluded_step` int(11) DEFAULT '0',
  `relative_height_top` double(5,2) NOT NULL,
  `relative_height_bottom` double(5,2) NOT NULL,
  `distanceMinIndexMax` bigint(20) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `play_evolutions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `hash` varchar(255) NOT NULL,
  `applied_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `apply_script` mediumtext,
  `revert_script` mediumtext,
  `state` varchar(255) DEFAULT NULL,
  `last_problem` mediumtext
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `question` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_id` bigint(20) NOT NULL,
  `html` longtext NOT NULL,
  `create_time` datetime NOT NULL,
  `uuid` varchar(255) NOT NULL,
  `permutation` bigint(20) NOT NULL,
  `secret` varchar(1024) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `question2assets` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `question_id` bigint(20) NOT NULL,
  `asset_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `turker_id` varchar(255) NOT NULL,
  `first_seen_date_time` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


ALTER TABLE `answer`
  ADD PRIMARY KEY (`id`),
  ADD KEY `question_id` (`question_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `time` (`time`);

ALTER TABLE `assets`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `batch`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `log`
  ADD PRIMARY KEY (`id`),
  ADD KEY `accesstime` (`accesstime`);

ALTER TABLE `permutations`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `play_evolutions`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `question`
  ADD PRIMARY KEY (`id`),
  ADD KEY `batch_id` (`batch_id`),
  ADD KEY `permutation` (`permutation`);

ALTER TABLE `question2assets`
  ADD PRIMARY KEY (`id`),
  ADD KEY `question_id` (`question_id`),
  ADD KEY `asset_id` (`asset_id`);

ALTER TABLE `users`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `answer`
  ADD CONSTRAINT `answer_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`),
  ADD CONSTRAINT `answer_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

ALTER TABLE `question`
  ADD CONSTRAINT `question_ibfk_1` FOREIGN KEY (`batch_id`) REFERENCES `batch` (`id`),
  ADD CONSTRAINT `question_ibfk_2` FOREIGN KEY (`permutation`) REFERENCES `permutations` (`id`);

ALTER TABLE `question2assets`
  ADD CONSTRAINT `question2assets_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`),
  ADD CONSTRAINT `question2assets_ibfk_2` FOREIGN KEY (`asset_id`) REFERENCES `assets` (`id`);

# --- !Downs



