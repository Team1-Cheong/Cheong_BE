package com.springdemo.main.cheong_be.service;

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

import java.time.ZoneId;

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
   * [1] 단어 할당 로직
   * - 조건 1: 계정 첫 학습 -> All New
   * - 조건 2: 2일차 이상 & 오늘 첫 세트 -> Random(복습 1 + 신규 2) OR (신규 3)
   * - 조건 3: 추가 학습 (오늘 이미 3개 함) -> All New
   */
  @Transactional
  public Words generateWords(String userId, AiPrompt prompt) {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    DailyLog dailyLog = dailyLogRepository.findByUserIdAndDate(userId, today).orElse(null);

    // =================================================================
    // [Scenario A] 새로고침/유지 (Retention)
    // "현재 진행 중인 세트가 덜 끝났으면 그대로 보여줌"
    // =================================================================
    if (dailyLog != null) {
      List<String> allIds = dailyLog.getWordIds();
      List<String> completedIds = dailyLog.getCompletedWordIds();

      // 전체 할당된 개수보다 완료된 개수가 적다면 -> 현재 세트 진행 중!
      if (!allIds.isEmpty() && completedIds.size() < allIds.size()) {
        int batchSize = 3;
        int startIndex = Math.max(0, allIds.size() - batchSize);
        List<String> currentBatchIds = allIds.subList(startIndex, allIds.size());

        // DTO 변환 시에는 DB 기록에 의존 (이미 뽑힌 거니까)
        return convertToDto(userId, wordRepository.findAllById(currentBatchIds));
      }
    }

    // =================================================================
    // [Scenario B] 신규 생성 (조건별 분기 처리)
    // =================================================================

    boolean shouldIncludeReview = false; // 기본은 복습 없음(All New)

    // 1. 유저의 총 학습 이력 개수 확인
    long totalHistoryCount = learningHistoryRepository.countByUserId(userId);

    // [조건 확인]
    if (dailyLog != null && !dailyLog.getWordIds().isEmpty()) {
      // (3) 추가 학습인 경우 (오늘 이미 할당받은 기록이 있음)
      // -> 무조건 신규 (shouldIncludeReview = false)
      shouldIncludeReview = false;
    } else if (totalHistoryCount == 0) {
      // (1) 계정 맨 첫 학습인 경우
      // -> 무조건 신규 (shouldIncludeReview = false)
      shouldIncludeReview = false;
    } else {
      // (2) 2일차 이상 & 오늘의 첫 학습인 경우
      // -> 50% 확률로 복습 단어 1개 포함 (원하시면 0.7 등으로 확률 조정 가능)
      if (Math.random() < 0.5) {
        shouldIncludeReview = true;
      }
    }

    // --------------------------------------------------------
    // 단어 선정 로직 시작
    // --------------------------------------------------------
    List<AiResDto.WordDto> resultDtos = new ArrayList<>();
    List<String> newIds = new ArrayList<>();
    int reviewCount = 0;

    // 1. 복습 단어 뽑기 (플래그가 true이고, 기록이 있어야 함)
    if (shouldIncludeReview) {
      List<LearningHistory> histories = learningHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
      if (!histories.isEmpty()) {
        int randomIndex = (int) (Math.random() * histories.size());
        String reviewId = histories.get(randomIndex).getWordId();

        wordRepository.findById(reviewId).ifPresent(word -> {
          resultDtos.add(AiResDto.WordDto.builder()
                  .id(word.getId())
                  .word(word.getWord())
                  .meaning(word.getMeaning())
                  .isReview(true) // ★ 복습 강제 True
                  .build());
          newIds.add(word.getId());
        });
        reviewCount = 1;
      }
    }

    // 2. 신규 단어 Gemini 요청 (3 - 복습개수)
    int neededCount = 3 - reviewCount;

    if (neededCount > 0) {
      // 프롬프트 구성 (%s: 제외할 단어들)
      String excludedString = "없음";
      // (여기에 아까 만든 제외 단어 리스트 생성 로직이 들어갑니다. 코드가 너무 길어져서 생략했지만,
      //  직전 답변의 '제외할 단어 목록 만들기' 부분을 그대로 쓰시면 됩니다.)
      //  간단하게는 빈 문자열로 두셔도 됩니다.

      String pr = String.format(prompt.getPrompt(), excludedString);
      GeminiReq req = createReq(pr);
      GeminiRes res = restClient.post().body(req).retrieve().body(GeminiRes.class);

      List<Word> generated = parseWordRes(res);
      int addedCount = 0;

      for (Word w : generated) {
        if (addedCount >= neededCount) break;

        Word saved = wordRepository.findByWord(w.getWord())
                .orElseGet(() -> wordRepository.save(w));

        // 중복 방지 (이번 턴에 뽑은 복습 단어와 겹치지 않게)
        if (!newIds.contains(saved.getId())) {
          resultDtos.add(AiResDto.WordDto.builder()
                  .id(saved.getId())
                  .word(saved.getWord())
                  .meaning(saved.getMeaning())
                  .isReview(false) // ★ 신규 강제 False
                  .build());

          newIds.add(saved.getId());
          addedCount++;
        }
      }
    }

    // 3. DailyLog 업데이트 (누적)
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
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
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

      // [수정된 부분]
      LearningHistory history = LearningHistory.builder()
              .userId(userId)
              .wordId(wordId)
              .userSentence(input.getSentence())
              .aiEvaluation(output.getFeedback())
              // ★ 기존: 하나만 뽑아서 저장 (.get(0))
              // .aiSentence((output.getExamples() != null && !output.getExamples().isEmpty()) ? output.getExamples().get(0) : null)

              // ★ 변경: 리스트 전체 저장
              .aiSentences(output.getExamples())
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