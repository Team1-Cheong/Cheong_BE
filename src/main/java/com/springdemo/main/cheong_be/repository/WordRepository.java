package com.springdemo.main.cheong_be.repository;
import com.springdemo.main.cheong_be.model.Word;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WordRepository extends MongoRepository<Word, String> {
    // 랜덤으로 단어를 가져오거나 할 때 커스텀 쿼리가 필요할 수 있음 (일단 기본 제공 메서드로 충분)
}
