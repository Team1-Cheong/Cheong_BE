package com.springdemo.main.cheong_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class CheongBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CheongBeApplication.class, args);
	}

}
