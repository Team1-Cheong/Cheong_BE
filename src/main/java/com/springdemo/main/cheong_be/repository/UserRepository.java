package com.springdemo.main.cheong_be.repository;

import com.springdemo.main.cheong_be.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    // 기본 CRUD만 있으면 되므로 추가 메서드 없음
}