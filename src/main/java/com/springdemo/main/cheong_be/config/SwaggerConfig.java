package com.springdemo.main.cheong_be.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("영어 단어 학습 API")
                        .description("하루 3단어 학습 및 AI 피드백 서비스 API 명세서")
                        .version("v1.0.0"));
    }
}