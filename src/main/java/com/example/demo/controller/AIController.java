package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.AIModelService;

@RestController
@RequestMapping("/api")
public class AIController {

    private final AIModelService aiModelService;

    public AIController(AIModelService aiModelService) {
        this.aiModelService = aiModelService;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateText(@RequestBody Map<String, Object> request) {
        String inputText = (String) request.get("input_text");
        int maxLength = (int) request.getOrDefault("max_length", 50);

        String response = aiModelService.generateText(inputText, maxLength);
        return ResponseEntity.ok(response);
    }
}