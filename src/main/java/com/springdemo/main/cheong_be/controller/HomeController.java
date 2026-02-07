package com.springdemo.main.cheong_be.controller;

import com.springdemo.main.cheong_be.dto.HomeResDto;
import com.springdemo.main.cheong_be.service.LearningService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomeController {

    private final LearningService learningService;

    @Operation(summary = "메인 홈 정보 조회", description = "스트릭, 오늘 학습 현황, 캘린더 데이터를 조회합니다.")
    @GetMapping("/home")
    public HomeResDto getHome(
            @RequestHeader(value = "X-User-Id", defaultValue = "test_user_1") String userId
    ) {
        return learningService.getHomeData(userId);
    }
}