package com.springdemo.main.cheong_be.dto;

import com.springdemo.main.cheong_be.model.Word;
import java.util.List;
import lombok.Builder;

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

  public record Feedback(
      String feedback,
      List<String> examples
  ){

  }

  public record EvaluationRes(
      List<Feedback> feedbacks
  ){

  }
}
