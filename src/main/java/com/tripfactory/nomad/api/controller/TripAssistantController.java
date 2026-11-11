package com.tripfactory.nomad.api.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.service.engine.TripAssistant;
import com.tripfactory.nomad.service.impl.AnthropicAssistantClient;
import com.tripfactory.nomad.service.impl.AnthropicAssistantClient.Turn;
import com.tripfactory.nomad.service.impl.TripAssistantContextLoader;
import com.tripfactory.nomad.service.util.AssistantRateLimiter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** "Ask about your trip": answers questions from the trip's own plan, with AI when it is switched on. */
@RestController
@RequestMapping("/api/trips/{tripId}/assistant")
public class TripAssistantController {

    public record Message(String role, String content) {
    }

    public record Ask(@NotBlank @Size(max = 500) String question, List<Message> history) {
    }

    /** mode is "ai" when Claude answered, "basic" for the built-in answer, "limit" when asked too often */
    public record Answer(String answer, String mode) {
    }

    private static final int MAX_HISTORY = 6;
    private static final int MAX_CONTENT = 600;

    private final TripAssistantContextLoader loader;
    private final AnthropicAssistantClient client;
    private final AssistantRateLimiter limiter = new AssistantRateLimiter(25, 60L * 60L * 1000L);

    public TripAssistantController(TripAssistantContextLoader loader, AnthropicAssistantClient client) {
        this.loader = loader;
        this.client = client;
    }

    @PostMapping
    @PreAuthorize("@authz.canAccessTrip(#tripId)")
    public ResponseEntity<Answer> ask(@PathVariable Long tripId, @Valid @RequestBody Ask request) {
        String who = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!limiter.allow(who, System.currentTimeMillis())) {
            return ResponseEntity.status(429).body(new Answer(
                    "You've asked a lot of questions this hour. Please try again in a little while.", "limit"));
        }

        TripAssistant.Trip trip = loader.load(tripId);
        String question = request.question().trim();

        if (client.isConfigured()) {
            String reply = client.ask(TripAssistant.systemPrompt(trip), turns(request.history(), question));
            if (reply != null) {
                return ResponseEntity.ok(new Answer(reply, "ai"));
            }
        }
        return ResponseEntity.ok(new Answer(TripAssistant.fallback(question, trip), "basic"));
    }

    /** Recent chat plus the new question, shaped the way the AI expects: alternating, starting with the user. */
    static List<Turn> turns(List<Message> history, String question) {
        List<Turn> out = new ArrayList<>();
        if (history != null) {
            int from = Math.max(0, history.size() - MAX_HISTORY);
            for (Message m : history.subList(from, history.size())) {
                if (m == null || m.content() == null || m.content().isBlank()) {
                    continue;
                }
                String role = "assistant".equals(m.role()) ? "assistant" : "user";
                String content = m.content().length() > MAX_CONTENT ? m.content().substring(0, MAX_CONTENT) : m.content();
                if (out.isEmpty() && "assistant".equals(role)) {
                    continue;
                }
                if (!out.isEmpty() && out.get(out.size() - 1).role().equals(role)) {
                    continue;
                }
                out.add(new Turn(role, content));
            }
        }
        if (!out.isEmpty() && "user".equals(out.get(out.size() - 1).role())) {
            out.remove(out.size() - 1);
        }
        out.add(new Turn("user", question));
        return out;
    }
}
