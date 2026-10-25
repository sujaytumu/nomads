package com.tripfactory.nomad.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PollResponse {

    private Long id;
    private String question;
    private boolean closed;
    private String createdByName;
    /** True when the signed-in user created this poll (and so may close it). */
    private boolean mine;
    private LocalDateTime createdAt;
    private int totalVotes;
    /** The option the signed-in user voted for, or null. */
    private Long myOptionId;
    private List<Option> options;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Option {
        private Long id;
        private String text;
        private int votes;
    }
}
