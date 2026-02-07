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
      ),
  EVALUATE_SENTENCES(
      """
          다음은 사용자가 3개의 단어를 사용하여 각각 만든 예문들입니다.
          입력된 순서대로 각 문장이 단어의 뜻과 문맥에 맞게 잘 쓰였는지 구체적으로 평가하고,
          각 단어를 활용한 다른 좋은 예문 3가지씩을 추천해줘.
          피드백 문장을 작성할 때, 대상 단어에 따옴표(큰따옴표, 작은따옴표)를 절대 붙이지 마.
          [입력 데이터]
          %s
        
          [응답 형식]
          반드시 마크다운 기호(```json 등) 없이 아래 JSON 형식으로만 응답할 것.
          입력된 문장의 순서와 1:1로 대응되는 배열이어야 함.
            {
              "feedbacks": [
                          {
                            "feedback": "첫 번째 단어/문장에 대한 평가 내용",
                            "examples": ["추천 예문 1", "추천 예문 2", "추천 예문 3"]
                          },
                          {
                            "feedback": "두 번째 단어/문장에 대한 평가 내용",
                            "examples": ["추천 예문 1", "추천 예문 2", "추천 예문 3"]
                          },
                          {
                            "feedback": "세 번째 단어/문장에 대한 평가 내용",
                            "examples": ["추천 예문 1", "추천 예문 2", "추천 예문 3"]
                          }
                        ]
           }
        """
  );

  private final String prompt;
}
