package com.als98.questlog.be;

import org.springframework.boot.SpringApplication;

public class TestQuestLogBeApplication {

	public static void main(String[] args) {
		SpringApplication.from(QuestLogBeApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
