package com.springdemo.main.cheong_be.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class HomeResDto {
    private String userId;
    private int currentStreak;      // 현재 연속 스트릭
    private boolean isTodayCompleted; // 오늘 목표(3개) 달성 여부
    private int todayCompletedCount;  // 오늘 몇 개 풀었는지 (1/3)
    private List<LocalDate> studiedDates; // 이번 달에 공부한 날짜들 (캘린더용)
}