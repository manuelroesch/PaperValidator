# --- Initial Table

# --- !Ups

CREATE TABLE `answer` (
  `id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `time` datetime NOT NULL,
  `is_related` boolean NOT NULL,
  `is_checked_before` boolean NOT NULL,
  `extra_answer` boolean NOT NULL,
  `confidence` int(11) NOT NULL,
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

CREATE TABLE `assumptions` (
  `id` int(11) UNSIGNED NOT NULL,
  `conference_id` int(11) UNSIGNED NOT NULL,
  `name` varchar(255) NOT NULL,
  `synonyms` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `batch` (
  `id` bigint(20) NOT NULL,
  `allowed_answers_per_turker` int(11) NOT NULL,
  `uuid` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `conference` (
  `id` int(11) UNSIGNED NOT NULL,
  `name` varchar(255) NOT NULL,
  `email` varchar(1024) NOT NULL,
  `secret` varchar(1024) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `conference_settings` (
  `id` int(11) UNSIGNED NOT NULL,
  `conference_id` int(11) UNSIGNED NOT NULL,
  `method2assumption_id` int(11) UNSIGNED NOT NULL,
  `flag` int(11) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `email` (
  `id` int(11) UNSIGNED NOT NULL,
  `email_address` varchar(1024) NOT NULL,
  `secret` varchar(1024) NOT NULL,
  `last_mail` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `log` (
  `id` int(11) UNSIGNED NOT NULL,
  `accesstime` datetime NOT NULL,
  `url` varchar(1024) NOT NULL DEFAULT '',
  `ip` varchar(254) NOT NULL DEFAULT '',
  `users` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `methods` (
  `id` int(11) UNSIGNED NOT NULL,
  `conference_id` int(11) UNSIGNED NOT NULL,
  `name` varchar(255) NOT NULL,
  `delta` int(11) NOT NULL,
  `synonyms` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `methods2assumptions` (
  `id` int(11) UNSIGNED NOT NULL,
  `conference_id` int(11) UNSIGNED NOT NULL,
  `method_id` int(11) UNSIGNED NOT NULL,
  `assumption_id` int(11) UNSIGNED NOT NULL,
  `question` varchar(512) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

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
  `distanceMinIndexMax` bigint(20) NOT NULL DEFAULT '0',
  `paper_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `papers` (
  `id` bigint(20) NOT NULL,
  `name` varchar(512) NOT NULL,
  `email` varchar(255) NOT NULL,
  `conference_id` int(11) UNSIGNED NOT NULL,
  `status` int(11) NOT NULL,
  `permutations` int(11) NOT NULL,
  `last_modified` datetime NOT NULL,
  `secret` varchar(1024) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `paper_methods` (
  `id` bigint(20) NOT NULL,
  `paper_id` bigint(20) NOT NULL,
  `method` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `paper_results` (
  `id` bigint(20) NOT NULL,
  `paper_id` bigint(20) NOT NULL,
  `result_type` int(11) NOT NULL,
  `descr` varchar(256) NOT NULL,
  `result` varchar(256) NOT NULL,
  `symbol` int(11) NOT NULL,
  `position` text NOT NULL
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

CREATE TABLE `users` (
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

ALTER TABLE `assumptions`
ADD PRIMARY KEY (`id`);

ALTER TABLE `batch`
ADD PRIMARY KEY (`id`);

ALTER TABLE `conference`
ADD PRIMARY KEY (`id`);

ALTER TABLE `conference_settings`
ADD PRIMARY KEY (`id`),
ADD KEY `conference_settings_ibfk_1` (`conference_id`),
ADD KEY `conference_settings_ibfk_2` (`method2assumption_id`);

ALTER TABLE `email`
ADD PRIMARY KEY (`id`);

ALTER TABLE `log`
ADD PRIMARY KEY (`id`),
ADD KEY `accesstime` (`accesstime`);

ALTER TABLE `methods`
ADD PRIMARY KEY (`id`),
ADD KEY `conference_id` (`conference_id`);

ALTER TABLE `methods2assumptions`
ADD PRIMARY KEY (`id`),
ADD KEY `method_id` (`method_id`),
ADD KEY `assumption_id` (`assumption_id`);

ALTER TABLE `permutations`
ADD PRIMARY KEY (`id`),
ADD KEY `paper_id` (`paper_id`);

ALTER TABLE `papers`
ADD PRIMARY KEY (`id`),
ADD KEY `conference_id` (`conference_id`);

ALTER TABLE `paper_methods`
ADD PRIMARY KEY (`id`),
ADD KEY `paper_id` (`paper_id`);

ALTER TABLE `paper_results`
ADD PRIMARY KEY (`id`),
ADD KEY `paper_id` (`paper_id`);

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
MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE `assets`
MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE `assumptions`
MODIFY `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT;
ALTER TABLE `batch`
MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE `conference`
MODIFY `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT;
ALTER TABLE `conference_settings`
MODIFY `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT;
ALTER TABLE `email`
MODIFY `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT;
ALTER TABLE `log`
MODIFY `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT;
ALTER TABLE `methods`
MODIFY `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT;
ALTER TABLE `methods2assumptions`
MODIFY `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT;
ALTER TABLE `papers`
MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE `paper_methods`
MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE `paper_results`
MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE `permutations`
MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE `question`
MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE `question2assets`
MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE `users`
MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `answer`
ADD CONSTRAINT `answer_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`),
ADD CONSTRAINT `answer_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

ALTER TABLE `assumptions`
ADD CONSTRAINT `assumptions_ibfk_1` FOREIGN KEY (`conference_id`) REFERENCES `conference` (`id`);

ALTER TABLE `conference_settings`
ADD CONSTRAINT `conference_settings_ibfk_1` FOREIGN KEY (`conference_id`) REFERENCES `conference` (`id`),
ADD CONSTRAINT `conference_settings_ibfk_2` FOREIGN KEY (`method2assumption_id`) REFERENCES `methods2assumptions` (`id`);

ALTER TABLE `methods`
ADD CONSTRAINT `methods_ibfk_1` FOREIGN KEY (`conference_id`) REFERENCES `conference` (`id`);

ALTER TABLE `methods2assumptions`
ADD CONSTRAINT `methods2assumptions_ibfk_1` FOREIGN KEY (`conference_id`) REFERENCES `conference` (`id`),
ADD CONSTRAINT `methods2assumptions_ibfk_2` FOREIGN KEY (`method_id`) REFERENCES `methods` (`id`),
ADD CONSTRAINT `methods2assumptions_ibfk_3` FOREIGN KEY (`assumption_id`) REFERENCES `assumptions` (`id`);

ALTER TABLE `papers`
ADD CONSTRAINT `papers_ibfk_1` FOREIGN KEY (`conference_id`) REFERENCES `conference` (`id`);

ALTER TABLE `paper_methods`
ADD CONSTRAINT `paper_methods_ibfk_1` FOREIGN KEY (`paper_id`) REFERENCES `papers` (`id`);

ALTER TABLE `paper_results`
ADD CONSTRAINT `paper_results_ibfk_1` FOREIGN KEY (`paper_id`) REFERENCES `papers` (`id`);

ALTER TABLE `permutations`
ADD CONSTRAINT `permutations_ibfk_1` FOREIGN KEY (`paper_id`) REFERENCES `papers` (`id`);

ALTER TABLE `question`
ADD CONSTRAINT `question_ibfk_1` FOREIGN KEY (`batch_id`) REFERENCES `batch` (`id`),
ADD CONSTRAINT `question_ibfk_2` FOREIGN KEY (`permutation`) REFERENCES `permutations` (`id`);

ALTER TABLE `question2assets`
ADD CONSTRAINT `question2assets_ibfk_1` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`),
ADD CONSTRAINT `question2assets_ibfk_2` FOREIGN KEY (`asset_id`) REFERENCES `assets` (`id`);

INSERT INTO `users` (`id`, `turker_id`, `first_seen_date_time`) VALUES
  (1, 'SkipUser', '2016-06-01 10:10:10');

INSERT INTO `batch` (`id`, `allowed_answers_per_turker`, `uuid`) VALUES
  (1, 1, 'SkipBatch');

# --- !Downs

