package com.springdemo.main.cheong_be.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LearningCompleteRequest {

    @Schema(description = "유저 ID", example = "test_user_1")
    private String userId;

    @Schema(description = "단어 ID", example = "65c2a...")
    private String wordId;

    @Schema(description = "유저가 작성한 예문", example = "I want to be a resilient person.")
    private String userSentence;
}