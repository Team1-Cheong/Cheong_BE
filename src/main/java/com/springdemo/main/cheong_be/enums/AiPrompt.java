package com.springdemo.main.cheong_be.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AiPrompt {
  GET_THREE_WORDS("""
      "어휘력 증가를 위한 단어 3가지 각각 뜻과 함께 뽑아줘."
        "[출력 형식] 마크다운을 제외하고 json으로 보낼 것"
        [예시]
        { "words" :
          [
            { "word": "함축적", "meaning": "겉으로 드러나지 않고 속에 깊은 뜻을 담고 있는" },
            { "word": "고무적", "meaning": "힘을 내도록 격려하거나 어떤 일에 용기를 북돋워 주는" },
            { "word": "가시적", "meaning": "눈으로 직접 확인할 수 있거나 결과가 겉으로 뚜렷하게 드러나는" }
          ]
        }
      """
      );

  private final String prompt;
}
