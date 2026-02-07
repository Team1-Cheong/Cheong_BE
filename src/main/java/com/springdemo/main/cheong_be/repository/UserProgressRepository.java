package com.springdemo.main.cheong_be.repository;

import com.springdemo.main.cheong_be.model.UserProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Repository
public interface UserProgressRepository extends MongoRepository<UserProgress, String> {
    // ID가 userId이므로 기본 findById로 충분함
}
