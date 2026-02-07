package com.springdemo.main.cheong_be.service;

import com.springdemo.main.cheong_be.dto.DailyWordResponse;
import com.springdemo.main.cheong_be.dto.LearningCompleteRequest;
import com.springdemo.main.cheong_be.dto.LearningCompleteResponse;
import com.springdemo.main.cheong_be.model.*;
import com.springdemo.main.cheong_be.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningService {

    private final WordRepository wordRepository;
    private final DailyLogRepository dailyLogRepository;
    private final LearningHistoryRepository learningHistoryRepository;
    private final UserProgressRepository userProgressRepository;

    // 팀원분이 만든 AiService (Gemini)
    private final AiService aiService;

    /**
     * [1] 오늘의 단어 조회
     * - 오늘 날짜의 DailyLog가 없으면 새로 생성 (새 단어 2 + 복습 1)
     * - 있으면 기존 할당된 단어 목록 반환
     */
    @Transactional
    public DailyWordResponse getDailyWords(String userId) {
        LocalDate today = LocalDate.now();

        // 1. 오늘의 로그 조회 혹은 생성
        DailyLog dailyLog = dailyLogRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> createNewDailyLog(userId, today));

        // 2. 할당된 wordId들로 실제 단어 정보 조회
        List<Word> allocatedWords = wordRepository.findAllById(dailyLog.getWordIds());

        // 3. 응답 DTO 변환
        List<DailyWordResponse.WordDto> wordDtos = allocatedWords.stream()
                .map(word -> DailyWordResponse.WordDto.builder()
                        .id(word.getId())
                        .word(word.getWord())
                        .meaning(word.getMeaning())
                        // 완료 목록에 포함되어 있으면 true
                        .isCompleted(dailyLog.getCompletedWordIds().contains(word.getId()))
                        // (선택) 복습 단어 여부 로직이 필요하면 여기에 추가
                        .isReview(false)
                        .build())
                .collect(Collectors.toList());

        return DailyWordResponse.builder()
                .date(today)
                .dailyLogId(dailyLog.getId())
                .words(wordDtos)
                .build();
    }

    // 내부 메서드: 일일 할당량 생성 로직 (New 2 + Review 1)
    private DailyLog createNewDailyLog(String userId, LocalDate date) {

        // 1. 복습 단어 1개 뽑기 (과거 기록에서 중복 제거 후 추출)
        List<String> reviewedWordIds = learningHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(LearningHistory::getWordId)
                .distinct()
                .collect(Collectors.toList());

        List<String> selectedIds = new ArrayList<>();

        // 기록이 있다면 섞어서 1개 뽑기
        if (!reviewedWordIds.isEmpty()) {
            Collections.shuffle(reviewedWordIds);
            selectedIds.add(reviewedWordIds.get(0));
        }

        // 2. 부족한 개수만큼 채우기 (현재는 DB 전체 단어 중 랜덤)
        // 추후 Gemini가 생성한 단어를 Word DB에 저장 후 가져오는 로직으로 변경 가능
        List<Word> allWords = wordRepository.findAll();

        if (allWords.size() < 3) {
            // 개발용 더미데이터가 부족할 때를 위한 로그
            System.out.println("Warning: DB에 단어가 3개 미만입니다.");
        }

        Collections.shuffle(allWords);

        for (Word w : allWords) {
            if (selectedIds.size() >= 3) break;
            // 중복 방지
            if (!selectedIds.contains(w.getId())) {
                selectedIds.add(w.getId());
            }
        }

        // 3. DailyLog 저장
        DailyLog newLog = DailyLog.builder()
                .userId(userId)
                .date(date)
                .wordIds(selectedIds)
                .completedWordIds(new ArrayList<>())
                .build();

        return dailyLogRepository.save(newLog);
    }

    /**
     * [2] 통합된 학습 완료 처리 (Gemini 호출 + 저장 + 스트릭)
     */
    @Transactional
    public LearningCompleteResponse completeLearning(LearningCompleteRequest request) {
        String userId = request.getUserId();
        LocalDate today = LocalDate.now();

        // [Step 1] 서버 내부에서 Gemini API 호출 (임시 데이터)
        // AiResponse aiRes = aiService.eval(request.getUserSentence());
        String aiEvaluation = "문법적으로 자연스럽습니다! (Gemini)";
        String aiSentence = "Here is a better example... (Gemini)";

        // [Step 2] 완성된 데이터를 한 번에 저장 (History)
        LearningHistory history = LearningHistory.builder()
                .userId(userId)
                .wordId(request.getWordId())
                .userSentence(request.getUserSentence())
                .aiEvaluation(aiEvaluation)
                .aiSentence(aiSentence)
                .build();
        learningHistoryRepository.save(history);

        // [Step 3] DailyLog 업데이트
        DailyLog dailyLog = dailyLogRepository.findByUserIdAndDate(userId, today)
                .orElseThrow(() -> new RuntimeException("오늘의 학습 로그가 없습니다. (먼저 /words/daily를 호출하세요)"));

        if (!dailyLog.getCompletedWordIds().contains(request.getWordId())) {
            dailyLog.getCompletedWordIds().add(request.getWordId());
            dailyLogRepository.save(dailyLog);
        }

        // [Step 4] Streak 관리
        UserProgress progress = userProgressRepository.findById(userId)
                .orElse(UserProgress.builder()
                        .userId(userId)
                        .currentStreak(0)
                        .todayCompleted(false)
                        .build());

        boolean isGoalCompleted = dailyLog.getCompletedWordIds().size() >= 3;
        String message = "학습이 저장되었습니다.";

        if (isGoalCompleted && !progress.isTodayCompleted()) {
            LocalDate yesterday = today.minusDays(1);

            // 어제 했으면 연속 스트릭, 아니면 1일차
            if (progress.getLastLearningDate() != null && progress.getLastLearningDate().equals(yesterday)) {
                progress.setCurrentStreak(progress.getCurrentStreak() + 1);
            } else {
                progress.setCurrentStreak(1);
            }
            progress.setTodayCompleted(true);
            progress.setLastLearningDate(today);
            message = "축하합니다! 오늘의 목표 달성! Streak +1 🔥";
        }
        userProgressRepository.save(progress);

        // [Step 5] 결과 반환
        return LearningCompleteResponse.builder()
                .currentStreak(progress.getCurrentStreak())
                .isDailyGoalCompleted(isGoalCompleted)
                .message(message)
                .aiEvaluation(aiEvaluation)
                .aiSentence(aiSentence)
                .build();
    }

    /**
     * [3] 학습 이력 조회
     */
    public List<LearningHistory> getHistory(String userId) {
        return learningHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }
}