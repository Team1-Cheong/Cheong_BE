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
import org.springframework.data.domain.Page;
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

    @GetMapping("/get")
    public AiResDto.Words test(
            @RequestHeader(value = "X-User-Id", defaultValue = "test_user_1") String userId
    ){
        return aiService.generateWords(userId, AiPrompt.GET_THREE_WORDS);
    }

    @PostMapping("/evaluate/batch")
    public AiResDto.EvaluationRes evaluate(
            @RequestHeader(value = "X-User-Id", defaultValue = "test_user_1") String userId,
            @RequestBody AiReqDto.EvaluationReq req
    ){
        // aiService.evaluationSentence(userId, prompt, req) 순서로 호출
        return aiService.evaluationSentence(userId, AiPrompt.EVALUATE_SENTENCES, req);
    }



    @GetMapping("/history")
    public List<HistoryResDto> getHistory(
            @RequestHeader(value = "X-User-Id", defaultValue = "test_user_1") String userId
    ) {
        return learningService.getHistory(userId);
    }

}