package com.springdemo.main.cheong_be.dto;

import java.util.List;

public class AiReqDto {
  public record GeminiReq(List<Content> contents, GenerationConfig generationConfig) {

    public record Content(List<Part> parts) {

    }

    public record Part(String text) {

    }

    public record GenerationConfig(String responseMimeType) {

    }
  }
}
