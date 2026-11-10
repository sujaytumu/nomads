package com.tripfactory.nomad.api.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.domain.entity.MemberLocation;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.MemberLocationRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.engine.LiveLocationRules;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Opt-in live positions for a group on trip day. Nothing is shared until a member turns it on, only group
 * members can see it, a position stops showing after 15 minutes, and stopping removes it straight away.
 */
@RestController
@RequestMapping("/api/groups/{groupId}/live")
@RequiredArgsConstructor
public class LiveLocationController {

    public record Position(double lat, double lng, Double accuracy) {
    }

    public record Shared(Long userId, String name, boolean me, double lat, double lng, Double accuracy, long secondsAgo) {
    }

    private final MemberLocationRepository locations;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Shared>> list(@PathVariable Long groupId) {
        User me = currentUser();
        LocalDateTime now = LocalDateTime.now();
        List<Shared> out = new ArrayList<>();
        for (MemberLocation m : locations.findByGroupIdAndUpdatedAtAfter(groupId, now.minus(LiveLocationRules.SHOW_FOR))) {
            String name = userRepository.findById(m.getUserId()).map(User::getName).orElse("Traveller");
            long ago = Math.max(0, java.time.Duration.between(m.getUpdatedAt(), now).getSeconds());
            out.add(new Shared(m.getUserId(), name, m.getUserId().equals(me.getId()), m.getLat(), m.getLng(), m.getAccuracyM(), ago));
        }
        return ResponseEntity.ok(out);
    }

    @PutMapping
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    @Transactional
    public ResponseEntity<Map<String, Object>> share(@PathVariable Long groupId, @RequestBody Position p) {
        if (p == null || !LiveLocationRules.validPosition(p.lat(), p.lng())) {
            throw new BadRequestException("That position isn't valid.");
        }
        User me = currentUser();
        LocalDateTime now = LocalDateTime.now();
        MemberLocation row = locations.findByGroupIdAndUserId(groupId, me.getId()).orElseGet(() -> {
            MemberLocation n = new MemberLocation();
            n.setGroupId(groupId);
            n.setUserId(me.getId());
            return n;
        });
        row.setLat(p.lat());
        row.setLng(p.lng());
        row.setAccuracyM(p.accuracy() != null && p.accuracy() >= 0 && p.accuracy() < 100000 ? p.accuracy() : null);
        row.setUpdatedAt(now);
        locations.save(row);
        locations.deleteOlderThan(now.minus(LiveLocationRules.KEEP_FOR));
        return ResponseEntity.ok(Map.of("shared", true));
    }

    @DeleteMapping
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    @Transactional
    public ResponseEntity<Map<String, Object>> stop(@PathVariable Long groupId) {
        return ResponseEntity.ok(Map.of("removed", locations.stopSharing(groupId, currentUser().getId())));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
