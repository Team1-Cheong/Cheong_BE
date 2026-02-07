package com.springdemo.main.cheong_be.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiAnalysisResponse {

    @Schema(description = "AI 평가 멘트", example = "자연스러운 문장입니다!")
    private String evaluation;

    @Schema(description = "AI 추천 예문", example = "However, it is resilient to scratch.")
    private String aiSentence;
}
