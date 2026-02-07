package com.springdemo.main.cheong_be.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class DailyWordResponse {

    @Schema(description = "오늘 날짜", example = "2026-02-07")
    private LocalDate date;

    @Schema(description = "오늘의 할당량 ID", example = "log_12345")
    private String dailyLogId;

    private List<WordDto> words;

    @Data
    @Builder
    public static class WordDto {
        @Schema(description = "단어 ID")
        private String id;

        @Schema(description = "단어")
        private String word;

        @Schema(description = "뜻")
        private String meaning;

        @Schema(description = "복습 단어 여부")
        private boolean isReview;

        @Schema(description = "오늘 학습 완료 여부")
        private boolean isCompleted;
    }
}