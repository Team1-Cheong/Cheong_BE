package com.springdemo.main.cheong_be.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import tools.jackson.databind.ObjectMapper;

import com.springdemo.main.cheong_be.dto.AiReqDto;
import com.springdemo.main.cheong_be.dto.AiReqDto.EvaluationReq;
import com.springdemo.main.cheong_be.dto.AiReqDto.GeminiReq;
import com.springdemo.main.cheong_be.dto.AiResDto;
import com.springdemo.main.cheong_be.dto.AiResDto.EvaluationRes;
import com.springdemo.main.cheong_be.dto.AiResDto.GeminiRes;
import com.springdemo.main.cheong_be.dto.AiResDto.Words;
import com.springdemo.main.cheong_be.enums.AiPrompt;
import com.springdemo.main.cheong_be.model.*;
import com.springdemo.main.cheong_be.repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class AiService {
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final WordRepository wordRepository;
  private final DailyLogRepository dailyLogRepository;
  private final LearningHistoryRepository learningHistoryRepository;
  private final UserProgressRepository userProgressRepository;

  public AiService(RestClient.Builder builder, ObjectMapper objectMapper,
                   WordRepository wordRepository,
                   DailyLogRepository dailyLogRepository,
                   LearningHistoryRepository learningHistoryRepository,
                   UserProgressRepository userProgressRepository,
                   @Value("${gemini.api.key}") String apiKey,
                   @Value("${gemini.api.url}") String apiUrl
  ) {
    this.objectMapper = objectMapper;
    this.wordRepository = wordRepository;
    this.dailyLogRepository = dailyLogRepository;
    this.learningHistoryRepository = learningHistoryRepository;
    this.userProgressRepository = userProgressRepository;
    this.restClient = builder
            .baseUrl(apiUrl)
            .defaultHeader("x-goog-api-key", apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
  }

  /**
   * [1] 단어 할당 (새로고침 유지 + 복습 1개 + 신규 2개)
   */
  @Transactional
  public Words generateWords(String userId, AiPrompt prompt) {
    LocalDate today = LocalDate.now();
    DailyLog dailyLog = dailyLogRepository.findByUserIdAndDate(userId, today).orElse(null);

    // =================================================================
    // [Scenario A] 새로고침/유지 (기존 코드와 동일)
    // =================================================================
    if (dailyLog != null) {
      List<String> allIds = dailyLog.getWordIds();
      List<String> completedIds = dailyLog.getCompletedWordIds();

      if (!allIds.isEmpty() && completedIds.size() < allIds.size()) {
        int batchSize = 3;
        int startIndex = Math.max(0, allIds.size() - batchSize);
        List<String> currentBatchIds = allIds.subList(startIndex, allIds.size());
        return convertToDto(userId, wordRepository.findAllById(currentBatchIds));
      }
    }

    // =================================================================
    // [Scenario B] 신규 생성
    // =================================================================
    List<AiResDto.WordDto> resultDtos = new ArrayList<>();
    List<String> newIds = new ArrayList<>();

    // 1. 복습 단어 1개 뽑기
    List<LearningHistory> histories = learningHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    int reviewCount = 0;

    if (!histories.isEmpty()) {
      int randomIndex = (int) (Math.random() * histories.size());
      String reviewId = histories.get(randomIndex).getWordId();

      wordRepository.findById(reviewId).ifPresent(word -> {
        resultDtos.add(AiResDto.WordDto.builder()
                .id(word.getId())
                .word(word.getWord())
                .meaning(word.getMeaning())
                .isReview(true)
                .build());
        newIds.add(word.getId());
      });
      reviewCount = resultDtos.size();
    }

    // 2. 신규 단어 Gemini 요청
    int neededCount = 3 - reviewCount;

    if (neededCount > 0) {

      // -----------------------------------------------------------------
      // ★ [핵심] 제외할 단어 목록 만들기 (%s 채우기)
      // -----------------------------------------------------------------
      Set<String> excludedIds = new HashSet<>();

      // (1) 과거에 학습한 모든 단어 ID 가져오기
      for (LearningHistory h : histories) {
        excludedIds.add(h.getWordId());
      }

      // (2) 오늘 이미 뽑힌 단어 ID들도 추가 (중복 방지)
      if (dailyLog != null) {
        excludedIds.addAll(dailyLog.getWordIds());
      }

      // (3) ID -> 실제 단어(String)로 변환
      // Gemini는 ID를 모르므로 "사과, 바나나" 같은 글자가 필요함
      List<String> excludedWords = wordRepository.findAllById(excludedIds).stream()
              .map(Word::getWord) // 단어 문자열만 추출
              .distinct()
              .collect(Collectors.toList());

      // (4) 문자열로 합치기 (예: "사과, 바나나, 포도")
      String excludedString = String.join(", ", excludedWords);
      if (excludedString.isEmpty()) {
        excludedString = "없음"; // 처음이라 제외할 게 없을 때
      }

      // (5) 프롬프트 완성 (String.format 사용)
      String pr = String.format(prompt.getPrompt(), excludedString);
      // -----------------------------------------------------------------

      GeminiReq req = createReq(pr);
      GeminiRes res = restClient.post().body(req).retrieve().body(GeminiRes.class);

      List<Word> generated = parseWordRes(res);

      int addedCount = 0;
      for (Word w : generated) {
        if (addedCount >= neededCount) break;

        Word saved = wordRepository.findByWord(w.getWord())
                .orElseGet(() -> wordRepository.save(w));

        if (!newIds.contains(saved.getId())) {
          resultDtos.add(AiResDto.WordDto.builder()
                  .id(saved.getId())
                  .word(saved.getWord())
                  .meaning(saved.getMeaning())
                  .isReview(false)
                  .build());

          newIds.add(saved.getId());
          addedCount++;
        }
      }
    }

    // 3. DailyLog 업데이트
    if (dailyLog == null) {
      dailyLog = DailyLog.builder()
              .userId(userId)
              .date(today)
              .wordIds(newIds)
              .completedWordIds(new ArrayList<>())
              .build();
    } else {
      dailyLog.getWordIds().addAll(newIds);
    }
    dailyLogRepository.save(dailyLog);

    return Words.builder().words(resultDtos).build();
  }

  // [DTO 변환] Word -> WordDto (isReview 플래그 포함)
  private Words convertToDto(String userId, List<Word> words) {
    List<AiResDto.WordDto> dtos = words.stream().map(word -> {
      boolean isReview = learningHistoryRepository.existsByUserIdAndWordId(userId, word.getId());
      return AiResDto.WordDto.builder()
              .id(word.getId())
              .word(word.getWord())
              .meaning(word.getMeaning())
              .isReview(isReview)
              .build();
    }).collect(Collectors.toList());

    return Words.builder().words(dtos).build();
  }

  /**
   * [2] 문장 평가 및 저장
   */
  @Transactional
  public EvaluationRes evaluationSentence(String userId, AiPrompt prompt, EvaluationReq request) {
      String jsonInput = objectMapper.writeValueAsString(request);
      String pr = String.format(prompt.getPrompt(), jsonInput);

      GeminiReq req = createReq(pr);
      GeminiRes res = restClient.post().body(req).retrieve().body(GeminiRes.class);

      EvaluationRes evaluationRes = parseEvaluationRes(res);
      saveEvaluationToHistory(userId, request, evaluationRes);

      return evaluationRes;
  }

  private void saveEvaluationToHistory(String userId, EvaluationReq request, EvaluationRes response) {
    LocalDate today = LocalDate.now();
    DailyLog dailyLog = dailyLogRepository.findByUserIdAndDate(userId, today)
            .orElseGet(() -> DailyLog.builder()
                    .userId(userId)
                    .date(today)
                    .completedWordIds(new ArrayList<>())
                    .wordIds(new ArrayList<>())
                    .build());

    List<AiReqDto.UserSentence> inputs = request.getUserSentences();
    List<AiResDto.Feedback> outputs = response.getFeedbacks();
    int size = Math.min(inputs.size(), outputs.size());

    for (int i = 0; i < size; i++) {
      AiReqDto.UserSentence input = inputs.get(i);
      AiResDto.Feedback output = outputs.get(i);

      String wordId = wordRepository.findFirstByWordContaining(input.getWord())
              .map(Word::getId)
              .orElse("unknown");

      LearningHistory history = LearningHistory.builder()
              .userId(userId)
              .wordId(wordId)
              .userSentence(input.getSentence())
              .aiEvaluation(output.getFeedback())
              .aiSentence((output.getExamples() != null && !output.getExamples().isEmpty())
                      ? output.getExamples().get(0) : null)
              .build();
      learningHistoryRepository.save(history);

      if (!"unknown".equals(wordId) && !dailyLog.getCompletedWordIds().contains(wordId)) {
        dailyLog.getCompletedWordIds().add(wordId);
      }
    }
    dailyLogRepository.save(dailyLog);
    updateStreak(userId, dailyLog);
  }

  private void updateStreak(String userId, DailyLog dailyLog) {
    if (dailyLog.getCompletedWordIds().size() >= 3) {
      UserProgress progress = userProgressRepository.findById(userId)
              .orElse(UserProgress.builder().userId(userId).currentStreak(0).build());

      if (!progress.isTodayCompleted()) {
        progress.setCurrentStreak(progress.getCurrentStreak() + 1);
        progress.setTodayCompleted(true);
        progress.setLastLearningDate(LocalDate.now());
        userProgressRepository.save(progress);
      }
    }
  }

  // --- Gemini 파싱용 임시 클래스 (내부 정의) ---
  // Gemini는 "isReview" 필드를 모르고 "word", "meaning"만 줍니다.
  // 그래서 이걸로 먼저 받은 뒤, 나중에 WordDto로 바꿉니다.
  @Data
  private static class TempGeminiWords {
    private List<Word> words;
  }

  private GeminiReq createReq(String prompt) {
    var part = new GeminiReq.Part(prompt);
    var content = new GeminiReq.Content(List.of(part));
    var config = new GeminiReq.GenerationConfig("application/json");
    return new GeminiReq(List.of(content), config);
  }

  private List<Word> parseWordRes(GeminiRes response) {
    if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
      throw new IllegalArgumentException("응답 생성 중 에러 발생");
    }
    try {
      String jsonText = response.candidates().getFirst().content().parts().getFirst().text();
      int firstBrace = jsonText.indexOf("{");
      int lastBrace = jsonText.lastIndexOf("}");
      if (firstBrace == -1 && lastBrace == -1) throw new IllegalArgumentException("응답 에러");

      String cleanJson = jsonText.substring(firstBrace, lastBrace + 1);

      // ★ [핵심 수정] TempGeminiWords로 받아서 .getWords() 호출
      return objectMapper.readValue(cleanJson, TempGeminiWords.class).getWords();

    } catch (Exception e) {
      throw new RuntimeException("단어 파싱 에러: " + e.getMessage());
    }
  }

  private EvaluationRes parseEvaluationRes(GeminiRes response) {
    try {
      String jsonText = response.candidates().getFirst().content().parts().getFirst().text();
      int firstBrace = jsonText.indexOf("{");
      int lastBrace = jsonText.lastIndexOf("}");
      String cleanJson = jsonText.substring(firstBrace, lastBrace + 1);
      return objectMapper.readValue(cleanJson, EvaluationRes.class);
    } catch (Exception e) {
      throw new IllegalArgumentException("응답 처리 중 에러 발생");
    }
  }
}