package com.springdemo.main.cheong_be.repository;

import com.springdemo.main.cheong_be.model.DailyLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyLogRepository extends MongoRepository<DailyLog, String> {
    // "특정 유저"의 "오늘 날짜" 로그 찾기
    Optional<DailyLog> findByUserIdAndDate(String userId, LocalDate date);
}
