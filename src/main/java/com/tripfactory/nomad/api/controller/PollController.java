package com.tripfactory.nomad.api.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.tripfactory.nomad.api.dto.PollCreateRequest;
import com.tripfactory.nomad.api.dto.PollResponse;
import com.tripfactory.nomad.api.dto.PollVoteRequest;
import com.tripfactory.nomad.domain.entity.GroupPoll;
import com.tripfactory.nomad.domain.entity.PollOption;
import com.tripfactory.nomad.domain.entity.PollVote;
import com.tripfactory.nomad.domain.entity.TripGroup;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.UserRole;
import com.tripfactory.nomad.repository.GroupPollRepository;
import com.tripfactory.nomad.repository.PollOptionRepository;
import com.tripfactory.nomad.repository.PollVoteRepository;
import com.tripfactory.nomad.repository.TripGroupRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/** Group polls: any group member can ask a question, everyone in the group votes once (and can change it). */
@RestController
@RequestMapping("/api/groups/{groupId}/polls")
@RequiredArgsConstructor
public class PollController {

    private final GroupPollRepository pollRepository;
    private final PollOptionRepository optionRepository;
    private final PollVoteRepository voteRepository;
    private final TripGroupRepository tripGroupRepository;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PollResponse>> list(@PathVariable Long groupId) {
        return ResponseEntity.ok(build(pollRepository.findByGroupIdWithCreator(groupId), currentUser()));
    }

    @PostMapping
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    @Transactional
    public ResponseEntity<PollResponse> create(@PathVariable Long groupId, @Valid @RequestBody PollCreateRequest request) {
        TripGroup group = tripGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        User me = currentUser();

        // Trim and drop duplicates so a poll never lists the same choice twice.
        Set<String> choices = new LinkedHashSet<>();
        for (String raw : request.getOptions()) {
            String text = raw == null ? "" : raw.trim();
            if (!text.isEmpty()) {
                choices.add(text);
            }
        }
        if (choices.size() < 2) {
            throw new BadRequestException("A poll needs at least two different choices");
        }

        GroupPoll poll = new GroupPoll();
        poll.setGroup(group);
        poll.setCreatedBy(me);
        poll.setQuestion(request.getQuestion().trim());
        pollRepository.save(poll);

        int position = 0;
        for (String text : choices) {
            PollOption option = new PollOption();
            option.setPoll(poll);
            option.setText(text);
            option.setPosition(position++);
            optionRepository.save(option);
        }
        return new ResponseEntity<>(build(List.of(poll), me).get(0), HttpStatus.CREATED);
    }

    @PostMapping("/{pollId}/vote")
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    @Transactional
    public ResponseEntity<PollResponse> vote(@PathVariable Long groupId, @PathVariable Long pollId,
            @Valid @RequestBody PollVoteRequest request) {
        GroupPoll poll = findInGroup(groupId, pollId);
        if (poll.isClosed()) {
            throw new BadRequestException("This poll is closed");
        }
        PollOption option = optionRepository.findById(request.getOptionId())
                .filter(o -> o.getPoll().getId().equals(poll.getId()))
                .orElseThrow(() -> new BadRequestException("That choice is not part of this poll"));
        User me = currentUser();

        PollVote vote = voteRepository.findByPollIdAndUserId(poll.getId(), me.getId()).orElseGet(() -> {
            PollVote v = new PollVote();
            v.setPoll(poll);
            v.setUser(me);
            return v;
        });
        vote.setOption(option);
        voteRepository.save(vote);
        return ResponseEntity.ok(build(List.of(poll), me).get(0));
    }

    @PostMapping("/{pollId}/close")
    @PreAuthorize("@authz.canAccessGroup(#groupId)")
    @Transactional
    public ResponseEntity<PollResponse> close(@PathVariable Long groupId, @PathVariable Long pollId) {
        GroupPoll poll = findInGroup(groupId, pollId);
        User me = currentUser();
        if (!poll.getCreatedBy().getId().equals(me.getId()) && me.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only the person who created the poll can close it");
        }
        poll.setClosed(true);
        pollRepository.save(poll);
        return ResponseEntity.ok(build(List.of(poll), me).get(0));
    }

    private GroupPoll findInGroup(Long groupId, Long pollId) {
        GroupPoll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll not found"));
        if (!poll.getGroup().getId().equals(groupId)) {
            throw new ResourceNotFoundException("Poll not found");
        }
        return poll;
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private List<PollResponse> build(List<GroupPoll> polls, User me) {
        List<PollResponse> out = new ArrayList<>();
        if (polls.isEmpty()) {
            return out;
        }
        List<Long> ids = new ArrayList<>();
        for (GroupPoll p : polls) {
            ids.add(p.getId());
        }
        Collection<Long> pollIds = ids;

        Map<Long, List<PollOption>> optionsByPoll = new HashMap<>();
        for (PollOption o : optionRepository.findByPollIds(pollIds)) {
            optionsByPoll.computeIfAbsent(o.getPoll().getId(), k -> new ArrayList<>()).add(o);
        }
        Map<Long, Integer> counts = new HashMap<>();
        Map<Long, Long> mine = new HashMap<>();
        for (PollVote v : voteRepository.findByPollIds(pollIds)) {
            Long optionId = v.getOption().getId();
            counts.merge(optionId, 1, Integer::sum);
            if (v.getUser().getId().equals(me.getId())) {
                mine.put(v.getPoll().getId(), optionId);
            }
        }

        for (GroupPoll p : polls) {
            PollResponse r = new PollResponse();
            r.setId(p.getId());
            r.setQuestion(p.getQuestion());
            r.setClosed(p.isClosed());
            r.setCreatedByName(p.getCreatedBy().getName());
            r.setMine(p.getCreatedBy().getId().equals(me.getId()));
            r.setCreatedAt(p.getCreatedAt());
            r.setMyOptionId(mine.get(p.getId()));
            int total = 0;
            List<PollResponse.Option> options = new ArrayList<>();
            for (PollOption o : optionsByPoll.getOrDefault(p.getId(), List.of())) {
                PollResponse.Option opt = new PollResponse.Option();
                opt.setId(o.getId());
                opt.setText(o.getText());
                int votes = counts.getOrDefault(o.getId(), 0);
                opt.setVotes(votes);
                total += votes;
                options.add(opt);
            }
            r.setOptions(options);
            r.setTotalVotes(total);
            out.add(r);
        }
        return out;
    }
}
