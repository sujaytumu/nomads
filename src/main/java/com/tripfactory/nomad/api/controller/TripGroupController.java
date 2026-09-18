package com.tripfactory.nomad.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.ChatMessageResponse;
import com.tripfactory.nomad.api.dto.TripGroupMemberResponse;
import com.tripfactory.nomad.api.dto.TripGroupResponse;
import com.tripfactory.nomad.chat.ChatWebSocketHandler;
import com.tripfactory.nomad.repository.ChatMessageRepository;
import com.tripfactory.nomad.service.TripGroupService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class TripGroupController {

    private final TripGroupService tripGroupService;
    private final ChatMessageRepository chatMessageRepository;

    @GetMapping("/{groupId}")
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    public ResponseEntity<TripGroupResponse> getGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(tripGroupService.getGroup(groupId));
    }

    @GetMapping("/{groupId}/members")
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    public ResponseEntity<List<TripGroupMemberResponse>> getGroupMembers(@PathVariable Long groupId) {
        return ResponseEntity.ok(tripGroupService.getGroupMembers(groupId));
    }

    @GetMapping("/{groupId}/messages")
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    public ResponseEntity<List<ChatMessageResponse>> getGroupMessages(@PathVariable Long groupId) {
        List<ChatMessageResponse> messages = chatMessageRepository.findByGroupIdOrderByCreatedAtAsc(groupId)
                .stream().map(ChatWebSocketHandler::toResponse).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(messages);
    }
}