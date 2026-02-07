package com.springdemo.main.cheong_be.config;

import com.springdemo.main.cheong_be.model.User;
import com.springdemo.main.cheong_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        String testUserId = "test_user_1"; // 고정 ID

        // DB에 해당 ID가 있는지 확인
        if (!userRepository.existsById(testUserId)) {
            User mockUser = User.builder()
                    .id(testUserId)
                    .name("테스트유저")
                    .email("test@example.com")
                    .build();

            userRepository.save(mockUser);
            System.out.println("✅ [DataInitializer] 테스트용 유저 생성 완료: " + testUserId);
        } else {
            System.out.println("ℹ️ [DataInitializer] 테스트용 유저가 이미 존재합니다: " + testUserId);
        }
    }
}