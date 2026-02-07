package com.springdemo.main.cheong_be.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "daily_logs")
public class DailyLog {

    @Id
    private String id;

    @Indexed // 검색 속도 향상 (userId로 조회를 자주 하므로)
    private String userId;

    private LocalDate date; // 예: 2026-02-07

    // 오늘 할당된 단어 ID 3개
    @Builder.Default
    private List<String> wordIds = new ArrayList<>();

    // 오늘 학습을 마친 단어 ID들 (이 리스트 사이즈가 3이 되면 Streak +1)
    @Builder.Default
    private List<String> completedWordIds = new ArrayList<>();
}