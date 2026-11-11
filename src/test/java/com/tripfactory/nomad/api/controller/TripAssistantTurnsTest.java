package com.tripfactory.nomad.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.api.controller.TripAssistantController.Message;
import com.tripfactory.nomad.service.impl.AnthropicAssistantClient.Turn;

class TripAssistantTurnsTest {

    @Test
    void noHistory_isJustTheQuestion() {
        List<Turn> t = TripAssistantController.turns(null, "hi");
        assertThat(t).hasSize(1);
        assertThat(t.get(0).role()).isEqualTo("user");
    }

    @Test
    void history_alternatesStartsWithUserAndEndsWithNewQuestion() {
        List<Turn> t = TripAssistantController.turns(List.of(
                new Message("assistant", "stray opener"),
                new Message("user", "q1"),
                new Message("assistant", "a1"),
                new Message("user", "q2 unanswered")), "q3");
        assertThat(t).extracting(Turn::role).containsExactly("user", "assistant", "user");
        assertThat(t.get(2).content()).isEqualTo("q3");
    }

    @Test
    void consecutiveSameRole_isSkipped() {
        List<Turn> t = TripAssistantController.turns(List.of(
                new Message("user", "q1"), new Message("user", "q1 again"), new Message("assistant", "a1")), "q2");
        assertThat(t).extracting(Turn::role).containsExactly("user", "assistant", "user");
    }
}
