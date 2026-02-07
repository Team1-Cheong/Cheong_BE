package com.springdemo.main.cheong_be.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class HistoryResDto {
    private String id;           // History ID

    // ★ Word ID 대신 실제 정보들이 들어갑니다
    private String word;         // 실제 단어 (예: "역설")
    private String meaning;      // 단어 뜻 (예: "겉으로는 모순되어 보이나...")

    private String userSentence; // 유저가 쓴 문장
    private String aiEvaluation; // AI 평가
    private List<String> aiSentences; // AI 예문들

    private LocalDateTime createdAt;  // 생성일
}