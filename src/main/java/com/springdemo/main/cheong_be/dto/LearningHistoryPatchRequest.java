package com.springdemo.main.cheong_be.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LearningHistoryPatchRequest {

    @Schema(description = "AI가 해준 평가", example = "문법적으로 완벽합니다.")
    private String aiEvaluation;

    @Schema(description = "AI가 제공한 추천 예문", example = "She remained resilient despite the difficulties.")
    private String aiSentence;
}

