package com.springdemo.main.cheong_be.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StreakResponse {

    @Schema(description = "현재 스트릭(연속 학습일)", example = "12")
    private int currentStreak;

    @Schema(description = "오늘 3개 단어 모두 완료했는지 여부", example = "true")
    private boolean isDailyGoalCompleted;

    @Schema(description = "응답 메시지", example = "오늘의 학습 완료! Streak +1 🔥")
    private String message;
}
