package com.springdemo.main.cheong_be.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_progress")
public class UserProgress {

    @Id
    private String userId; // PK로 사용 (중복 방지, 조회 편리)

    private int currentStreak;          // 현재 연속 학습일
    private LocalDate lastLearningDate; // 마지막으로 Streak이 올라간 날짜

    private boolean todayCompleted;     // 오늘 학습 완료 여부 (화면 UI용 Flag)
}