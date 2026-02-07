package com.springdemo.main.cheong_be.service;

import com.springdemo.main.cheong_be.dto.LearningCompleteRequest;
import com.springdemo.main.cheong_be.dto.DailyWordResponse;
import com.springdemo.main.cheong_be.dto.LearningHistoryPatchRequest;
import com.springdemo.main.cheong_be.dto.StreakResponse;
import com.springdemo.main.cheong_be.model.DailyLog;
import com.springdemo.main.cheong_be.model.LearningHistory;
import com.springdemo.main.cheong_be.model.UserProgress;
import com.springdemo.main.cheong_be.model.Word;
import com.springdemo.main.cheong_be.repository.DailyLogRepository;
import com.springdemo.main.cheong_be.repository.LearningHistoryRepository;
import com.springdemo.main.cheong_be.repository.UserProgressRepository;
import com.springdemo.main.cheong_be.repository.WordRepository;
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

    private DailyLog createNewDailyLog(String userId, LocalDate date) {

        // [Step 1] 복습 단어 1개 뽑기 (기존 로직 유지)
        List<String> reviewedWordIds = learningHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(LearningHistory::getWordId).distinct().collect(Collectors.toList());

        List<String> selectedIds = new ArrayList<>();
        if (!reviewedWordIds.isEmpty()) {
            Collections.shuffle(reviewedWordIds); // 리스트 섞기
            selectedIds.add(reviewedWordIds.get(0)); // 첫 번째 거 뽑기
        }

        // [Step 2] 부족한 개수만큼 Gemini에게 "새 단어" 요청
        int wordsNeeded = 3 - selectedIds.size();
        if (wordsNeeded > 0) {
            // 예: GeminiService를 통해 단어 리스트(DTO)를 받아옴
            // List<WordDto> newWordsFromGemini = geminiService.getNewWords(wordsNeeded);

            // ★ 중요: Gemini가 준 단어를 DB(Word 컬렉션)에 저장해야 ID가 생김!
        /* for (WordDto dto : newWordsFromGemini) {
            // 이미 DB에 있는지 중복 체크 (선택사항)
            // Word newWord = Word.builder().word(dto.getWord()).meaning(dto.getMeaning()).build();
            // Word savedWord = wordRepository.save(newWord); // <--- 여기서 DB에 저장됨!
            // selectedIds.add(savedWord.getId());
        }
        */

            // (임시) 현재는 DB에 있는거 랜덤으로 뽑기 (Gemini 연동 전까지 유지)
            List<Word> allWords = wordRepository.findAll();
            Collections.shuffle(allWords);
            for (Word w : allWords) {
                if (selectedIds.size() >= 3) break;
                if (!selectedIds.contains(w.getId())) selectedIds.add(w.getId());
            }
        }

        // [Step 3] DailyLog 저장 (이건 잘 되어 있음)
        DailyLog newLog = DailyLog.builder()
                .userId(userId)
                .date(date)
                .wordIds(selectedIds)
                .completedWordIds(new ArrayList<>())
                .build();

        return dailyLogRepository.save(newLog); // 여기서 로그가 저장됨
    }

    /**
     * [2] 학습 완료 처리 (저장 & Streak 갱신)
     * - 프론트가 보낸(또는 Gemini가 준) 평가 결과를 DB에 저장
     * - 오늘 3개를 다 했으면 Streak +1
     */
    @Transactional
    public StreakResponse completeLearning(LearningCompleteRequest request) {
        String userId = request.getUserId();
        LocalDate today = LocalDate.now();

        // 1. 학습 이력 저장 (History)
        LearningHistory history = LearningHistory.builder()
                .userId(userId)
                .wordId(request.getWordId())
                .userSentence(request.getUserSentence())
                .aiEvaluation(request.getAiEvaluation()) // 이미 AI가 평가한 값
                .aiSentence(request.getAiSentence())     // 이미 AI가 준 값
                .build();
        learningHistoryRepository.save(history);

        // 2. DailyLog 업데이트 (완료 도장 찍기)
        DailyLog dailyLog = dailyLogRepository.findByUserIdAndDate(userId, today)
                .orElseThrow(() -> new RuntimeException("오늘의 학습 로그가 없습니다."));

        if (!dailyLog.getCompletedWordIds().contains(request.getWordId())) {
            dailyLog.getCompletedWordIds().add(request.getWordId());
            dailyLogRepository.save(dailyLog);
        }

        // 3. Streak 관리 (UserProgress)
        UserProgress progress = userProgressRepository.findById(userId)
                .orElse(UserProgress.builder()
                        .userId(userId)
                        .currentStreak(0)
                        .todayCompleted(false)
                        .build());

        // 오늘 목표 달성 여부 확인
        boolean isGoalCompleted = dailyLog.getCompletedWordIds().size() >= 3;
        String message = "학습이 저장되었습니다.";

        // "오늘 처음으로" 목표를 달성한 경우에만 Streak 증가
        if (isGoalCompleted && !progress.isTodayCompleted()) {
            // 어제 학습했는지 확인 (연속 학습 로직)
            LocalDate yesterday = today.minusDays(1);
            if (progress.getLastLearningDate() != null && progress.getLastLearningDate().equals(yesterday)) {
                progress.setCurrentStreak(progress.getCurrentStreak() + 1);
            } else {
                // 어제 안했으면 Streak 1부터 다시 시작 (또는 정책에 따라 유지)
                progress.setCurrentStreak(1);
            }

            progress.setTodayCompleted(true);
            progress.setLastLearningDate(today);
            message = "오늘의 목표 달성! Streak가 올라갑니다! 🔥";
        }

        userProgressRepository.save(progress);

        return StreakResponse.builder()
                .currentStreak(progress.getCurrentStreak())
                .isDailyGoalCompleted(isGoalCompleted)
                .message(message)
                .build();
    }

    /**
     * [3] 학습 이력 조회
     */
    public List<LearningHistory> getHistory(String userId) {
        return learningHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void patchHistory(String historyId, LearningHistoryPatchRequest request) {
        LearningHistory history = learningHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("해당 학습 이력이 없습니다."));

        if (request.getUserSentence() != null) history.setUserSentence(request.getUserSentence());
        if (request.getAiEvaluation() != null) history.setAiEvaluation(request.getAiEvaluation());
        if (request.getAiSentence() != null) history.setAiSentence(request.getAiSentence());

        learningHistoryRepository.save(history);
    }

}