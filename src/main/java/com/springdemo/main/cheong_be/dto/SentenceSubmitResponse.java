package com.springdemo.main.cheong_be.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SentenceSubmitResponse {

    @Schema(description = "생성된 학습 이력 ID", example = "65c2a...")
    private String historyId;

    @Schema(description = "저장된 단어 ID", example = "65c2a...")
    private String wordId;

    @Schema(description = "유저 예문", example = "I want to be a resilient person.")
    private String userSentence;

    @Schema(description = "저장 시간")
    private LocalDateTime createdAt;
}

