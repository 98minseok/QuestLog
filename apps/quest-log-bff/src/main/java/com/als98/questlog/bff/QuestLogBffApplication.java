package com.als98.questlog.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class QuestLogBffApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuestLogBffApplication.class, args);
	}

}
