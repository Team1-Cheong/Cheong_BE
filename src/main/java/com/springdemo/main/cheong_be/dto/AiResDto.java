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

  @Builder
  public record Words(List<Word> words){

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
