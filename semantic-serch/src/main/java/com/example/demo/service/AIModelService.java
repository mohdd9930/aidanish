package com.example.demo.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AIModelService {

    private final RestTemplate restTemplate;

    @Value("${python.api.url}")
    private String pythonApiUrl;

    public AIModelService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateText(String inputText, int maxLength) {
        String url = pythonApiUrl + "/generate";

        // Create the request payload
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("input_text", inputText);
        requestPayload.put("max_length", maxLength);

        // Send the POST request
        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestPayload, Map.class);

        // Extract the response
        Map<String, Object> responseBody = response.getBody();
        return responseBody != null ? (String) responseBody.get("response") : null;
    }
}