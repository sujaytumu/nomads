package com.tripfactory.nomad.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Asks Claude (Anthropic's Messages API) a question. Off by default: it only works when ANTHROPIC_API_KEY is
 * set on the server. Any failure returns null so the caller can fall back to the built-in answerer.
 */
@Service
public class AnthropicAssistantClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicAssistantClient.class);
    private static final String URL = "https://api.anthropic.com/v1/messages";

    public record Turn(String role, String content) {
    }

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String model;

    public AnthropicAssistantClient(
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${nomad.assistant.model:claude-haiku-5-5}") String model) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(25000);
        this.restTemplate = new RestTemplate(factory);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    /** @param turns alternating user/assistant messages, starting and ending with a user message */
    public String ask(String system, List<Turn> turns) {
        if (!isConfigured()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            List<Map<String, String>> messages = new ArrayList<>();
            for (Turn t : turns) {
                messages.add(Map.of("role", t.role(), "content", t.content()));
            }
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 400,
                    "system", system,
                    "messages", messages);

            JsonNode res = restTemplate.exchange(URL, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class)
                    .getBody();
            if (res == null || !res.has("content")) {
                return null;
            }
            for (JsonNode part : res.get("content")) {
                if ("text".equals(part.path("type").asText()) && !part.path("text").asText().isBlank()) {
                    return part.get("text").asText().trim();
                }
            }
            return null;
        } catch (Exception ex) {
            log.warn("Assistant AI call failed, using the built-in answer: {}", ex.getMessage());
            return null;
        }
    }
}
