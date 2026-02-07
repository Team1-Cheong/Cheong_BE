package com.springdemo.main.cheong_be.controller;

import com.springdemo.main.cheong_be.dto.LearningCompleteRequest;
import com.springdemo.main.cheong_be.dto.DailyWordResponse;
import com.springdemo.main.cheong_be.dto.LearningHistoryPatchRequest;
import com.springdemo.main.cheong_be.dto.StreakResponse;
import com.springdemo.main.cheong_be.model.LearningHistory;
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

    @Operation(summary = "오늘의 단어 할당받기", description = "하루 3개의 단어(신규2+복습1)를 가져옵니다. 이미 존재하면 기존 목록을 반환합니다.")
    @GetMapping("/words/daily")
    // TODO: Gemini API connect
    public ResponseEntity<DailyWordResponse> getDailyWords(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader(value = "X-User-Id", defaultValue = "test_user_1") String userId
    ) {
        return ResponseEntity.ok(learningService.getDailyWords(userId));
    }

    @Operation(summary = "학습 결과 저장 (Streak 갱신)", description = "AI 평가가 완료된 예문을 저장하고, 오늘 3개를 다 채웠다면 Streak를 올립니다.")
    @PostMapping("/complete")
    public ResponseEntity<StreakResponse> completeLearning(
            @RequestBody LearningCompleteRequest request
    ) {
        // Tip: DTO에 userId가 없다면 헤더에서 꺼내서 set 해주는 로직이 필요할 수 있습니다.
        // 여기선 RequestBody에 userId가 포함되어 있다고 가정합니다.
        return ResponseEntity.ok(learningService.completeLearning(request));
    }

    @Operation(summary = "나의 학습 기록 조회", description = "과거 학습했던 단어와 예문들을 최신순으로 봅니다.")
    @GetMapping("/history")
    public ResponseEntity<List<LearningHistory>> getHistory(
            @RequestParam String userId
    ) {
        return ResponseEntity.ok(learningService.getHistory(userId));
    }

    @Operation(summary = "예문/평가 업데이트", description = "LearningHistory를 부분 업데이트합니다. (예문/AI평가/AI예문)")
    @PatchMapping("/history/{historyId}")
    public ResponseEntity<Void> patchHistory(
            @PathVariable String historyId,
            @RequestBody LearningHistoryPatchRequest request
    ) {
        learningService.patchHistory(historyId, request);
        return ResponseEntity.noContent().build();
    }
}
