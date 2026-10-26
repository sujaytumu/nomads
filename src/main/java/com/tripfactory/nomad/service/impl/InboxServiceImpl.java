package com.tripfactory.nomad.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.InboxItemResponse;
import com.tripfactory.nomad.api.dto.InboxPageResponse;
import com.tripfactory.nomad.domain.entity.InboxNotification;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.repository.InboxNotificationRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.InboxService;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InboxServiceImpl implements InboxService {

    static final int MAX_PAGE_SIZE = 50;

    private final InboxNotificationRepository inboxRepository;
    private final UserRepository userRepository;

    /**
     * Runs in its own transaction so a problem writing a notification can never roll back the payment or
     * booking that triggered it.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void push(Long userId, String type, String title, String body, String link) {
        User user = userRepository.getReferenceById(userId);
        InboxNotification n = new InboxNotification();
        n.setUser(user);
        n.setType(clip(type, 40));
        n.setTitle(clip(title, 120));
        n.setBody(clip(body, 500));
        n.setLink(clip(link, 200));
        inboxRepository.save(n);
    }

    @Override
    @Transactional(readOnly = true)
    public InboxPageResponse list(String email, boolean unreadOnly, int page, int size) {
        User user = requireUser(email);
        int safeSize = Math.max(1, Math.min(size <= 0 ? 20 : size, MAX_PAGE_SIZE));
        int safePage = Math.max(0, page);
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<InboxNotification> result = unreadOnly
                ? inboxRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(user.getId(), pageable)
                : inboxRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId(), pageable);
        List<InboxItemResponse> items = result.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new InboxPageResponse(items, inboxRepository.countByUserIdAndReadAtIsNull(user.getId()),
                safePage, safeSize, result.getTotalElements(), result.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return inboxRepository.countByUserIdAndReadAtIsNull(requireUser(email).getId());
    }

    @Override
    @Transactional
    public boolean markRead(String email, Long id) {
        User user = requireUser(email);
        return inboxRepository.findByIdAndUserId(id, user.getId()).map(n -> {
            if (n.getReadAt() == null) {
                n.setReadAt(LocalDateTime.now());
                inboxRepository.save(n);
            }
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public int markAllRead(String email) {
        return inboxRepository.markAllRead(requireUser(email).getId(), LocalDateTime.now());
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private InboxItemResponse toResponse(InboxNotification n) {
        return new InboxItemResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(),
                n.getReadAt() != null, n.getCreatedAt());
    }

    private static String clip(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
