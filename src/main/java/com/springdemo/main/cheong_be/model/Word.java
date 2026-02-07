package com.springdemo.main.cheong_be.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "words")
public class Word {

    @Id
    private String id;

    private String word;      // 예: "Resilient"
    private String meaning;   // 예: "회복력 있는"
}