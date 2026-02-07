package com.springdemo.main.cheong_be;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.util.TimeZone;

@SpringBootApplication
@EnableMongoAuditing
public class CheongBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CheongBeApplication.class, args);
	}
	@PostConstruct
	public void init() {
		// 서버의 시간대를 "아시아/서울"로 강제 설정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		System.out.println("현재 시간: " + new java.util.Date()); // 로그로 확인용
	}
}
