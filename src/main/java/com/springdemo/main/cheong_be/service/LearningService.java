package com.springdemo.main.cheong_be.service;

import com.springdemo.main.cheong_be.dto.DailyWordResponse;
import com.springdemo.main.cheong_be.dto.HomeResDto;
import com.springdemo.main.cheong_be.dto.HistoryResDto;
import com.springdemo.main.cheong_be.model.*;
import com.springdemo.main.cheong_be.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
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
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

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
     * [3] 학습 이력 조회
     */
    @Transactional(readOnly = true)
    public List<HistoryResDto> getHistory(String userId) {
        // 1. 히스토리 전체 조회
        List<LearningHistory> histories = learningHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        if (histories.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 최적화: 루프 돌 때마다 DB 조회하면 느리니까, ID들을 모아서 한 번에 단어를 가져옴 (Batch Fetch)
        Set<String> wordIds = histories.stream()
                .map(LearningHistory::getWordId)
                .collect(Collectors.toSet());

        // ID를 키(Key)로 하고 Word 객체를 값(Value)으로 하는 맵 생성 -> 검색 속도 O(1)
        Map<String, Word> wordMap = wordRepository.findAllById(wordIds).stream()
                .collect(Collectors.toMap(Word::getId, w -> w));

        // 3. 변환 (History + Word -> DTO)
        return histories.stream().map(h -> {
            // 맵에서 단어 찾기 (혹시 단어가 삭제되었을 경우를 대비해 getOrDefault 사용)
            Word w = wordMap.getOrDefault(h.getWordId(), Word.builder().word("삭제된 단어").meaning("-").build());

            return HistoryResDto.builder()
                    .id(h.getId())
                    .word(w.getWord())       // ★ 실제 단어 매핑
                    .meaning(w.getMeaning()) // ★ 뜻 매핑
                    .userSentence(h.getUserSentence())
                    .aiEvaluation(h.getAiEvaluation())
                    .aiSentences(h.getAiSentences()) // List<String>
                    .createdAt(h.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HomeResDto getHomeData(String userId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 1. 유저 진행 상황 (스트릭) 조회
        UserProgress progress = userProgressRepository.findById(userId)
                .orElse(UserProgress.builder()
                        .userId(userId)
                        .currentStreak(0)
                        .todayCompleted(false)
                        .build());

        // 2. 오늘의 학습 로그 조회 (오늘 몇 개 했는지)
        DailyLog dailyLog = dailyLogRepository.findByUserIdAndDate(userId, today).orElse(null);
        int todayCount = (dailyLog == null) ? 0 : dailyLog.getCompletedWordIds().size();
        boolean isGoalReached = todayCount >= 3;

        // 3. 캘린더용: 이번 달 학습 기록이 있는 날짜들 조회 (예: 최근 30일)
        // (간단하게 구현하기 위해 DailyLog에서 날짜만 뽑아옵니다)
        List<LocalDate> studiedDates = dailyLogRepository.findAllByUserId(userId).stream()
                .map(DailyLog::getDate)
                .sorted()
                .collect(Collectors.toList());

        return HomeResDto.builder()
                .userId(userId)
                .currentStreak(progress.getCurrentStreak())
                .isTodayCompleted(isGoalReached) // 3개 이상이면 완료로 침
                .todayCompletedCount(todayCount)
                .studiedDates(studiedDates)
                .build();
    }
}