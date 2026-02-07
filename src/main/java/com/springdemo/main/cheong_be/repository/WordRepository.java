package com.springdemo.main.cheong_be.repository;
import com.springdemo.main.cheong_be.model.Word;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WordRepository extends MongoRepository<Word, String> {
    Optional<Word> findByWord(String word);
    Optional<Word> findFirstByWordContaining(String word);
}
