package com.tripfactory.nomad.chat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.jwt.JwtService;

/**
 * Authenticates the WebSocket handshake via a JWT passed as a query param
 * (?token=...), since the browser's native WebSocket API can't set custom
 * headers the way a normal HTTP request can. Also checks the connecting
 * user is actually a member of the group they're trying to join - group
 * chat should not be joinable by anyone who just knows the group ID.
 */
@Component
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TripRequestRepository tripRequestRepository;

    public ChatHandshakeInterceptor(JwtService jwtService, UserRepository userRepository,
            TripRequestRepository tripRequestRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.tripRequestRepository = tripRequestRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(request.getURI());
        List<String> tokenParams = uriBuilder.build().getQueryParams().get("token");
        String token = (tokenParams == null || tokenParams.isEmpty()) ? null : tokenParams.get(0);

        if (token == null || !jwtService.isTokenValid(token)) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        String email = jwtService.extractSubject(token);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        Long groupId = extractGroupId(request.getURI().getPath());
        if (groupId == null) {
            response.setStatusCode(org.springframework.http.HttpStatus.BAD_REQUEST);
            return false;
        }

        User user = userOpt.get();
        boolean isMember = tripRequestRepository.existsByGroupIdAndUserId(groupId, user.getId());
        if (!isMember) {
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put("userId", user.getId());
        attributes.put("groupId", groupId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Exception exception) {
        // no-op
    }

    private Long extractGroupId(String path) {
        // path looks like /ws/chat/{groupId}
        String[] parts = path.split("/");
        try {
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception ex) {
            return null;
        }
    }
}
