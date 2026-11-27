package com.example.MyProject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfSystemProperty(named = "runIntegrationTests", matches = "true")
class MyProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
