package com.springdemo.main.cheong_be.repository;

import com.springdemo.main.cheong_be.model.LearningHistory;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LearningHistoryRepository extends MongoRepository<LearningHistory, String> {
    // 유저의 전체 학습 기록 (최신순 정렬)
    List<LearningHistory> findAllByUserIdOrderByCreatedAtDesc(String userId);

    // 이미 학습한 단어인지 체크할 때 사용
    boolean existsByUserIdAndWordId(String userId, String wordId);
}