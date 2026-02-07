package com.springdemo.main.cheong_be.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SentenceSubmitRequest {

    @Schema(description = "단어 ID", example = "65c2a...")
    private String wordId;

    @Schema(description = "단어 텍스트 (검증용)", example = "Resilient")
    private String word;

    @Schema(description = "유저가 작성한 예문", example = "I want to be a resilient person.")
    private String userSentence;
}
