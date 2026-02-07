package com.springdemo.main.cheong_be.service;

import com.springdemo.main.cheong_be.dto.HomeResDto;
import com.springdemo.main.cheong_be.dto.HistoryResDto;
import com.springdemo.main.cheong_be.model.*;
import com.springdemo.main.cheong_be.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

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



    /**
     * [3] 학습 이력 조회: Pagination 적용
     */
    @Transactional(readOnly = true)
    public Page<HistoryResDto> getHistory(String userId, int page, int size) {
        // 1. Pageable 객체 생성 (날짜 내림차순 정렬 포함)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // 2. DB에서 해당 페이지의 데이터만 가져오기 (전체 조회 X)
        Page<LearningHistory> historyPage = learningHistoryRepository.findAllByUserId(userId, pageable);

        // 데이터가 아예 없으면 빈 페이지 반환
        if (historyPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 3. [최적화] 현재 페이지에 있는 '3개'의 단어 ID만 수집
        Set<String> wordIds = historyPage.getContent().stream()
                .map(LearningHistory::getWordId)
                .collect(Collectors.toSet());

        // 4. 단어 정보 가져오기 (딱 3개만 조회하므로 매우 빠름)
        Map<String, Word> wordMap = wordRepository.findAllById(wordIds).stream()
                .collect(Collectors.toMap(Word::getId, w -> w));

        // 5. 변환 (Page.map()을 사용하면 내부 콘텐츠만 싹 변환해서 다시 Page로 만들어줌)
        return historyPage.map(h -> {
            Word w = wordMap.getOrDefault(h.getWordId(), Word.builder().word("삭제된 단어").meaning("-").build());

            return HistoryResDto.builder()
                    .id(h.getId())
                    .word(w.getWord())
                    .meaning(w.getMeaning())
                    .userSentence(h.getUserSentence())
                    .aiEvaluation(h.getAiEvaluation())
                    .aiSentences(h.getAiSentences())
                    .createdAt(h.getCreatedAt())
                    .build();
        });
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