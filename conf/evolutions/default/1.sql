# Users schema

# --- !Ups

CREATE TABLE `answer` (
  `id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `time` datetime NOT NULL,
  `answer_json` longtext NOT NULL,
  `expected_output_code` bigint(20) NOT NULL,
  `accepted` tinyint(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



CREATE TABLE `assets` (
  `id` bigint(20) NOT NULL,
  `hash_code` varchar(255) NOT NULL,
  `byte_array` longblob NOT NULL,
  `content_type` varchar(255) NOT NULL,
  `filename` varchar(300) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


CREATE TABLE `batch` (
  `id` bigint(20) NOT NULL,
  `allowed_answers_per_turker` int(11) NOT NULL,
  `uuid` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



CREATE TABLE `log` (
  `id` int(11) UNSIGNED NOT NULL,
  `accesstime` datetime NOT NULL,
  `url` varchar(1024) NOT NULL DEFAULT '',
  `ip` varchar(254) NOT NULL DEFAULT '',
  `USER` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `permutations` (
  `id` bigint(20) NOT NULL,
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



CREATE TABLE `question` (
  `id` bigint(20) NOT NULL,
  `batch_id` bigint(20) NOT NULL,
  `html` longtext NOT NULL,
  `create_time` datetime NOT NULL,
  `uuid` varchar(255) NOT NULL,
  `permutation` bigint(20) NOT NULL,
  `secret` varchar(1024) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



CREATE TABLE `question2assets` (
  `id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `asset_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;



CREATE TABLE `user` (
  `id` bigint(20) NOT NULL,
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

ALTER TABLE `question`
  ADD PRIMARY KEY (`id`),
  ADD KEY `batch_id` (`batch_id`),
  ADD KEY `permutation` (`permutation`);

ALTER TABLE `question2assets`
  ADD PRIMARY KEY (`id`),
  ADD KEY `question_id` (`question_id`),
  ADD KEY `asset_id` (`asset_id`);

ALTER TABLE `user`
  ADD PRIMARY KEY (`id`);


ALTER TABLE `answer`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

ALTER TABLE `assets`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

ALTER TABLE `batch`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=37;

ALTER TABLE `log`
  MODIFY `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=40;

ALTER TABLE `permutations`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=35;

ALTER TABLE `question`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=43;

ALTER TABLE `question2assets`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=85;

ALTER TABLE `user`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

ALTER TABLE `answer`
  ADD CONSTRAINT `answer_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`),
  ADD CONSTRAINT `answer_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`);

ALTER TABLE `question`
  ADD CONSTRAINT `question_ibfk_1` FOREIGN KEY (`batch_id`) REFERENCES `batch` (`id`),
  ADD CONSTRAINT `question_ibfk_2` FOREIGN KEY (`permutation`) REFERENCES `permutations` (`id`);

ALTER TABLE `question2assets`
  ADD CONSTRAINT `question2assets_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`),
  ADD CONSTRAINT `question2assets_ibfk_2` FOREIGN KEY (`asset_id`) REFERENCES `assets` (`id`);

# --- !Downs