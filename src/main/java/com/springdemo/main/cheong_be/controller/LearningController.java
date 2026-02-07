package com.springdemo.main.cheong_be.controller;

import com.springdemo.main.cheong_be.dto.*;
import com.springdemo.main.cheong_be.enums.AiPrompt;
import com.springdemo.main.cheong_be.model.LearningHistory;
import com.springdemo.main.cheong_be.service.AiService;
import com.springdemo.main.cheong_be.service.LearningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Learning API", description = "단어 학습 및 스트릭 관리 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;
    private final AiService aiService;

    // [1] 중요: AI 단어 생성 (건드리지 않음)
    @Operation(summary = "오늘의 단어 할당받기 (AI Direct)", description = "AI로부터 직접 3개의 단어를 생성받아 DB에 저장하고 반환합니다.")
    @GetMapping("/get")
    public AiResDto.Words test(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader(value = "X-User-Id", defaultValue = "test_user_1") String userId
    ) {
        // userId를 같이 넘겨줍니다!
        return aiService.generateWords(AiPrompt.GET_THREE_WORDS);
    }

    // [2] 통합된 학습 완료 처리 (Gemini 호출 + 저장 + 스트릭 갱신)
    @Operation(summary = "학습 완료 및 AI 평가 (통합)", description = "유저 예문을 받아 서버에서 AI 평가를 수행하고, 결과를 저장한 뒤 스트릭 정보를 반환합니다.")
    @PostMapping("/complete")
    public ResponseEntity<LearningCompleteResponse> completeLearning(
            @RequestBody LearningCompleteRequest request
    ) {
        // 기존: StreakResponse 반환 -> 변경: LearningCompleteResponse (AI 평가 결과 포함)
        return ResponseEntity.ok(learningService.completeLearning(request));
    }

    // [3] 히스토리 조회 (기존 유지)
    @Operation(summary = "나의 학습 기록 조회", description = "과거 학습했던 단어와 예문들을 최신순으로 봅니다.")
    @GetMapping("/history")
    public ResponseEntity<List<LearningHistory>> getHistory(
            @RequestParam String userId
    ) {
        return ResponseEntity.ok(learningService.getHistory(userId));
    }

    // ❌ 삭제된 메서드 (더 이상 필요 없음)
    // - POST /sentence (submitSentence) : completeLearning으로 통합됨
    // - PATCH /history/{id} (patchHistory) : completeLearning으로 통합됨
}