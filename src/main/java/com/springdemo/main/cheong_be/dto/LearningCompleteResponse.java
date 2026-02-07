package com.springdemo.main.cheong_be.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LearningCompleteResponse {
    // 1. 스트릭 정보
    private int currentStreak;
    private boolean isDailyGoalCompleted;
    private String message;

    // 2. AI 평가 결과 (즉시 반환)
    private String aiEvaluation;
    private String aiSentence;
}
