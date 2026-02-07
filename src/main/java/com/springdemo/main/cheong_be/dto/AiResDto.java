package com.springdemo.main.cheong_be.dto;

import com.springdemo.main.cheong_be.model.Word;
import java.util.List;
import lombok.Builder;
import lombok.Data;

public class AiResDto {
  public record GeminiRes(List<Candidate> candidates) {

    public record Candidate(Content content) {

    }

    public record Content(List<Part> parts) {

    }

    public record Part(String text) {

    }
  }

  @Data
  @Builder
  public static class Words {
    private List<WordDto> words; // 여기가 핵심!
  }

  @Data
  @Builder
  public static class WordDto {
    private String id;
    private String word;
    private String meaning;
    private boolean isReview; // ★ Flag 추가! (true면 복습, false면 신규)
  }

  @Data
  public static class EvaluationRes {
    private List<Feedback> feedbacks;
  }

  @Data
  public static class Feedback {
    private String feedback;
    private List<String> examples;
  }
}
