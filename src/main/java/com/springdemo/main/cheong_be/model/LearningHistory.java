package com.springdemo.main.cheong_be.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "learning_histories")
public class LearningHistory {

    @Id
    private String id;

    @Indexed
    private String userId;       // 누가 학습했는지

    private String wordId;       // 어떤 단어인지 (Word 컬렉션의 id 참조)

    private String userSentence; // 유저가 작성한 예문
    private String aiEvaluation; // AI 평가 멘트
    private String aiSentence;   // AI가 제안한 더 좋은 예문

    @CreatedDate
    private LocalDateTime createdAt; // 저장된 시간 자동 생성
}