package com.springdemo.main.cheong_be.service;

import com.springdemo.main.cheong_be.dto.AiReqDto.GeminiReq;
import com.springdemo.main.cheong_be.dto.AiResDto.GeminiRes;
import com.springdemo.main.cheong_be.dto.AiResDto.Words;
import com.springdemo.main.cheong_be.enums.AiPrompt;
import com.springdemo.main.cheong_be.model.DailyLog;
import com.springdemo.main.cheong_be.model.Word;
import com.springdemo.main.cheong_be.repository.DailyLogRepository;
import com.springdemo.main.cheong_be.repository.WordRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiService {
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  // [1] DB 저장을 위한 Repository 주입
  private final WordRepository wordRepository;
  private final DailyLogRepository dailyLogRepository;

  public AiService(RestClient.Builder builder, ObjectMapper objectMapper,
                   @Value("${gemini.api.key}") String apiKey,
                   @Value("${gemini.api.url}") String apiUrl,
                   WordRepository wordRepository,       // 생성자 주입 추가
                   DailyLogRepository dailyLogRepository // 생성자 주입 추가
  ) {
    this.objectMapper = objectMapper;
    this.wordRepository = wordRepository;
    this.dailyLogRepository = dailyLogRepository;
    this.restClient = builder
            .baseUrl(apiUrl)
            .defaultHeader("x-goog-api-key", apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
  }

  // [2] userId 파라미터 추가
  public Words generateWords(String userId, AiPrompt prompt){
    String pr = String.format(prompt.getPrompt());

    GeminiReq req = createReq(pr);

    GeminiRes res = restClient.post()
            .body(req)
            .retrieve()
            .body(GeminiRes.class);

    // AI가 준 순수한 단어 리스트 (ID 없음)
    List<Word> rawWords = parseRes(res);
    List<Word> savedWords = new ArrayList<>();
    List<String> wordIds = new ArrayList<>();

    // [3] DB 저장 로직 (ID 생성)
    for (Word rawWord : rawWords) {
      // 이미 DB에 있는 단어인지 확인 (중복 방지, 선택사항)
      // 여기서는 편의상 무조건 저장한다고 가정하거나,
      // 기존 단어가 있으면 그걸 쓰고 없으면 새로 저장하는 방식을 추천합니다.

      // 예시: 무조건 저장 (실무에선 중복 체크 권장)
      Word saved = wordRepository.save(rawWord);
      savedWords.add(saved);
      wordIds.add(saved.getId());
    }

    // [4] DailyLog 생성 및 저장 (스트릭 기능을 위해 필수!)
    DailyLog dailyLog = DailyLog.builder()
            .userId(userId)
            .date(LocalDate.now())
            .wordIds(wordIds)
            .completedWordIds(new ArrayList<>())
            .build();

    // 만약 이미 오늘 로그가 있다면 덮어쓰거나, 새로 만듭니다.
    // (기존 로그가 있어도 AI 요청을 다시 했다면 새로 갱신하는 것이 의도이므로 save)
    // 주의: 기존 로그를 찾아 업데이트 하려면 findByUserIdAndDate 로직 필요.
    // 여기서는 간단하게 기존 로그 무시하고 덮어씌우는 save로 처리합니다.
    DailyLog existingLog = dailyLogRepository.findByUserIdAndDate(userId, LocalDate.now()).orElse(null);
    if (existingLog != null) {
      dailyLog.setId(existingLog.getId()); // ID 유지하면서 내용 갱신
    }
    dailyLogRepository.save(dailyLog);

    return Words.builder()
            .words(savedWords) // ID가 있는 단어 리스트 반환
            .build();
  }

  // ... createReq, parseRes 메서드는 기존과 동일 ...
  private GeminiReq createReq(String prompt) {
    var part = new GeminiReq.Part(prompt);
    var content = new GeminiReq.Content(List.of(part));
    var config = new GeminiReq.GenerationConfig("application/json");
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