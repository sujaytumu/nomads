package com.tripfactory.nomad.chat;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripfactory.nomad.api.dto.ChatMessageResponse;
import com.tripfactory.nomad.domain.entity.ChatMessage;
import com.tripfactory.nomad.domain.entity.TripGroup;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.ChatMessageRepository;
import com.tripfactory.nomad.repository.TripGroupRepository;
import com.tripfactory.nomad.repository.UserRepository;

/**
 * Plain WebSocket (not full STOMP) chat handler - one session per connection,
 * grouped by tripGroupId. Kept deliberately simple: the browser's native
 * WebSocket API is enough for a single chat room per group, no extra
 * frontend library needed.
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final Map<Long, Set<WebSocketSession>> sessionsByGroup = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final ChatMessageRepository chatMessageRepository;
    private final TripGroupRepository tripGroupRepository;
    private final UserRepository userRepository;

    public ChatWebSocketHandler(ChatMessageRepository chatMessageRepository, TripGroupRepository tripGroupRepository,
            UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.tripGroupRepository = tripGroupRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long groupId = (Long) session.getAttributes().get("groupId");
        sessionsByGroup.computeIfAbsent(groupId, k -> new CopyOnWriteArraySet<>()).add(session);
        LOGGER.info("Chat session opened for group {}", groupId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long groupId = (Long) session.getAttributes().get("groupId");
        Set<WebSocketSession> sessions = sessionsByGroup.get(groupId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long groupId = (Long) session.getAttributes().get("groupId");
        Long userId = (Long) session.getAttributes().get("userId");

        String content;
        try {
            Map<?, ?> parsed = objectMapper.readValue(message.getPayload(), Map.class);
            content = String.valueOf(parsed.get("content"));
        } catch (Exception ex) {
            content = message.getPayload();
        }
        if (content == null || content.isBlank() || content.length() > 1000) {
            return;
        }

        Optional<User> senderOpt = userRepository.findById(userId);
        Optional<TripGroup> groupOpt = tripGroupRepository.findById(groupId);
        if (senderOpt.isEmpty() || groupOpt.isEmpty()) {
            return;
        }

        ChatMessage entity = new ChatMessage();
        entity.setGroup(groupOpt.get());
        entity.setSender(senderOpt.get());
        entity.setContent(content);
        ChatMessage saved = chatMessageRepository.save(entity);

        broadcast(groupId, toResponse(saved));
    }

    private void broadcast(Long groupId, ChatMessageResponse response) {
        Set<WebSocketSession> sessions = sessionsByGroup.get(groupId);
        if (sessions == null) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(response);
        } catch (Exception ex) {
            return;
        }
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(json));
                }
            } catch (IOException ex) {
                LOGGER.warn("Failed to send chat message to a session: {}", ex.getMessage());
            }
        }
    }

    public static ChatMessageResponse toResponse(ChatMessage m) {
        ChatMessageResponse r = new ChatMessageResponse();
        r.setId(m.getId());
        r.setGroupId(m.getGroup().getId());
        r.setSenderId(m.getSender().getId());
        r.setSenderName(m.getSender().getName());
        r.setContent(m.getContent());
        r.setCreatedAt(m.getCreatedAt());
        return r;
    }
}
