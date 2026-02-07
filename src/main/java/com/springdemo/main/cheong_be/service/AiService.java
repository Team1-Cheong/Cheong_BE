package com.springdemo.main.cheong_be.service;

// 1. 자바 기본 유틸
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 2. 잭슨 (JSON 처리) - tools...가 아니라 com.fasterxml... 이어야 함
import com.fasterxml.jackson.core.JsonProcessingException;
import tools.jackson.databind.ObjectMapper;

// 3. 스프링 프레임워크 (@Service, @Transactional, @Value 등)
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

// 4. 우리 프로젝트 파일들 (DTO, Model, Repository, Enum)
import com.springdemo.main.cheong_be.dto.AiReqDto;
import com.springdemo.main.cheong_be.dto.AiReqDto.EvaluationReq;
import com.springdemo.main.cheong_be.dto.AiReqDto.GeminiReq;
import com.springdemo.main.cheong_be.dto.AiResDto;
import com.springdemo.main.cheong_be.dto.AiResDto.EvaluationRes;
import com.springdemo.main.cheong_be.dto.AiResDto.GeminiRes;
import com.springdemo.main.cheong_be.dto.AiResDto.Words;
import com.springdemo.main.cheong_be.enums.AiPrompt;
import com.springdemo.main.cheong_be.model.*;       // 모델 전체
import com.springdemo.main.cheong_be.repository.*;  // 리포지토리 전체

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

  @Transactional
  public Words generateWords(String userId, AiPrompt prompt){
    LocalDate today = LocalDate.now();

    DailyLog dailyLog = dailyLogRepository.findByUserIdAndDate(userId, today).orElse(null);

    if (dailyLog != null) {
      List<String> allWordIds = dailyLog.getWordIds();           // 할당받은 전체 단어 ID
      List<String> completedIds = dailyLog.getCompletedWordIds(); // 푼 단어 ID

      // "할당된 게 있는데(NotEmpty), 완료된 개수가 할당된 개수보다 적으면" -> 안 푼 게 남았다는 뜻!
      if (!allWordIds.isEmpty() && completedIds.size() < allWordIds.size()) {

        // ★ 중요: Gemini 호출 안 함! DB에서 기존 단어 찾아서 반환 (새로고침 유지)
        // 아직 안 푼 단어만 줄지, 전체를 다시 보여줄지는 선택 (여기선 전체 다시 보여줌)
        List<Word> existingWords = wordRepository.findAllById(allWordIds);
        return Words.builder().words(existingWords).build();
      }
    }

    String pr = String.format(prompt.getPrompt());

    GeminiReq req = createReq(pr);

    GeminiRes res = restClient.post()
        .body(req)
        .retrieve()
        .body(GeminiRes.class);

    List<Word> generatedWords = parseWordRes(res);

    // 단어 DB에 저장
    List<Word> savedWords = new ArrayList<>();
    List<String> newWordIds = new ArrayList<>();

    for (Word word : generatedWords) {
      if (wordRepository.findByWord(word.getWord()).isEmpty()) {
        Word saved = wordRepository.save(word);
        savedWords.add(saved);
        newWordIds.add(saved.getId());
      } else {
        // 이미 있는 단어면 ID만 가져옴
        Word existing = wordRepository.findByWord(word.getWord()).get();
        savedWords.add(existing);
        newWordIds.add(existing.getId());
      }
    }

    // DailyLog 업데이트 (없으면 생성, 있으면 추가)
    if (dailyLog == null) {
      dailyLog = DailyLog.builder()
              .userId(userId)
              .date(today)
              .wordIds(newWordIds) // 새 단어들
              .completedWordIds(new ArrayList<>())
              .build();
    } else {
      // 이미 존재하면 기존 리스트에 '추가' (Add All)
      dailyLog.getWordIds().addAll(newWordIds);
    }

    dailyLogRepository.save(dailyLog);

    return Words.builder()
        .words(savedWords)
        .build();
  }

  @Transactional
  public EvaluationRes evaluationSentence(String userId, AiPrompt prompt, EvaluationReq request) {
      // 1. Gemini 요청
      String jsonInput = objectMapper.writeValueAsString(request);
      String pr = String.format(prompt.getPrompt(), jsonInput);

      GeminiReq req = createReq(pr);
      GeminiRes res = restClient.post()
              .body(req)
              .retrieve()
              .body(GeminiRes.class);

      EvaluationRes evaluationRes = parseEvaluationRes(res);

      // 2. DB 저장 (심플하게 호출)
      saveEvaluationToHistory(userId, request, evaluationRes);

      return evaluationRes;
  }

  // ★ 수정된 저장 메서드 (심플 버전)
  // 내부 메서드: 실제 DB 저장 로직
  private void saveEvaluationToHistory(String userId, EvaluationReq request, EvaluationRes response) {
    LocalDate today = LocalDate.now();

    // 없으면 "빈 로그" 생성 (Lazy Creation)
    DailyLog dailyLog = dailyLogRepository.findByUserIdAndDate(userId, today)
            .orElseGet(() -> DailyLog.builder()
                    .userId(userId)
                    .date(today)
                    .completedWordIds(new ArrayList<>()) // 빈 리스트 초기화
                    .wordIds(new ArrayList<>())          // 할당된 단어도 일단 빈 상태
                    .build());


    // 1. DTO를 class로 바꿨으므로 getter 메서드 사용
    List<AiReqDto.UserSentence> inputs = request.getUserSentences();
    List<AiResDto.Feedback> outputs = response.getFeedbacks();

    int size = Math.min(inputs.size(), outputs.size());

    for (int i = 0; i < size; i++) {
      AiReqDto.UserSentence input = inputs.get(i);
      AiResDto.Feedback output = outputs.get(i);

      // 단어 ID 찾기
      String wordId = wordRepository.findFirstByWordContaining(input.getWord())
              .map(Word::getId)
              .orElse("unknown");

      // History 저장
      LearningHistory history = LearningHistory.builder()
              .userId(userId)
              .wordId(wordId)
              .userSentence(input.getSentence())
              .aiEvaluation(output.getFeedback())
              .aiSentence((output.getExamples() != null && !output.getExamples().isEmpty())
                      ? output.getExamples().get(0) : null)
              .build();

      learningHistoryRepository.save(history);

      // DailyLog에 완료 도장 찍기
      if (!"unknown".equals(wordId) && !dailyLog.getCompletedWordIds().contains(wordId)) {
        dailyLog.getCompletedWordIds().add(wordId);
      }
    }

    // ★ 중요: 여기서 dailyLog가 신규 생성이든 수정이든 저장이 됩니다.
    dailyLogRepository.save(dailyLog);

    updateStreak(userId, dailyLog);
  }

  // 스트릭 업데이트 (유지)
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





  private GeminiReq createReq(String prompt) {
    var part = new GeminiReq.Part(prompt);
    var content = new GeminiReq.Content(List.of(part));
    var config = new GeminiReq.GenerationConfig("application/json"); // JSON 모드 활성화
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

      if (firstBrace == -1 && lastBrace == -1) {
        throw new IllegalArgumentException("응답 처리 중 에러 발생");
      }

      String cleanJson = jsonText.substring(firstBrace, lastBrace + 1);

      return objectMapper.readValue(cleanJson, Words.class).words();

    } catch (Exception e) {
      throw new IllegalArgumentException("응답 처리 중 에러 발생");
    }
  }

  private EvaluationRes parseEvaluationRes(GeminiRes response){
    try {
      String jsonText = response.candidates().getFirst().content().parts().getFirst().text();

      int firstBrace = jsonText.indexOf("{");
      int lastBrace = jsonText.lastIndexOf("}");

      if (firstBrace == -1 && lastBrace == -1) {
        throw new IllegalArgumentException("응답 처리 중 에러 발생");
      }

      String cleanJson = jsonText.substring(firstBrace, lastBrace + 1);

      return objectMapper.readValue(cleanJson, EvaluationRes.class);

    } catch (Exception e) {
      throw new IllegalArgumentException("응답 처리 중 에러 발생");
    }
  }
}
