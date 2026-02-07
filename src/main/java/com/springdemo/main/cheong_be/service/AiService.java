package com.springdemo.main.cheong_be.service;


import com.springdemo.main.cheong_be.dto.AiReqDto.GeminiReq;
import com.springdemo.main.cheong_be.dto.AiResDto.GeminiRes;
import com.springdemo.main.cheong_be.dto.AiResDto.Words;
import com.springdemo.main.cheong_be.enums.AiPrompt;
import com.springdemo.main.cheong_be.model.Word;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiService {
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public AiService(RestClient.Builder builder, ObjectMapper objectMapper,
      @Value("${gemini.api.key}") String apiKey,
      @Value("${gemini.api.url}") String apiUrl
  ) {
    this.objectMapper = objectMapper;
    this.restClient = builder
        .baseUrl(apiUrl)
        .defaultHeader("x-goog-api-key", apiKey)
        .defaultHeader("Content-Type", "application/json")
        .build();
  }

  public Words generateWords(AiPrompt prompt){
    String pr = String.format(prompt.getPrompt());

    GeminiReq req = createReq(pr);

    GeminiRes res = restClient.post()
        .body(req)
        .retrieve()
        .body(GeminiRes.class);

    return Words.builder()
        .words(parseRes(res))
        .build();
  }

  private GeminiReq createReq(String prompt) {
    var part = new GeminiReq.Part(prompt);
    var content = new GeminiReq.Content(List.of(part));
    var config = new GeminiReq.GenerationConfig("application/json"); // JSON 모드 활성화
    return new GeminiReq(List.of(content), config);
  }

  private List<Word> parseRes(GeminiRes response) {
    if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
      throw new IllegalArgumentException("응답 생성 중 에러 발생");
    }

    try {
      String jsonText = response.candidates().get(0).content().parts().get(0).text();

      int firstBrace = jsonText.indexOf("{");
      int lastBrace = jsonText.lastIndexOf("}");

      if (firstBrace == -1 && lastBrace == -1) {
        throw new IllegalArgumentException("응답 처리 중 에러 발생");
      }

      String cleanJson = jsonText.substring(firstBrace, lastBrace + 1);

      return objectMapper.readValue(cleanJson, Words.class).words();

    } catch (Exception e) {
      throw new IllegalArgumentException("응답 처리 중 에러 발생");
    }
  }
}
