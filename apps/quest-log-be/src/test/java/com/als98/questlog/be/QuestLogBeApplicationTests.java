package com.als98.questlog.be;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class QuestLogBeApplicationTests {

	@Test
	void contextLoads() {
	}

}
