package com.springdemo.main.cheong_be.controller;

import com.springdemo.main.cheong_be.dto.AiReqDto.EvaluationReq;
import com.springdemo.main.cheong_be.dto.AiResDto.EvaluationRes;
import com.springdemo.main.cheong_be.dto.AiResDto.Words;
import com.springdemo.main.cheong_be.enums.AiPrompt;
import com.springdemo.main.cheong_be.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WordController {

  private final AiService aiService;

  @GetMapping("/get")
  public Words test(
  ){
    return aiService.generateWords(AiPrompt.GET_THREE_WORDS);
  }

  @PostMapping("/evaluate/batch")
  public EvaluationRes evaluate(@RequestBody EvaluationReq req){
    return aiService.evaluationSentence(AiPrompt.EVALUATE_SENTENCES,req);
  }
}
