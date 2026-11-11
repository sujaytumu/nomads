package com.tripfactory.nomad.service.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.service.engine.TripAssistant.Stop;
import com.tripfactory.nomad.service.engine.TripAssistant.Trip;
import com.tripfactory.nomad.service.util.AssistantRateLimiter;

class TripAssistantTest {

    private Trip trip(List<Integer> rain) {
        return new Trip("Hyderabad", 2, "2026-12-05", 4, "CAR", "12000",
                List.of(
                        new Stop(1, "Charminar", "CULTURE", "09:00", 0),
                        new Stop(1, "Paradise Biryani", "FOOD", "12:30", 4.2),
                        new Stop(2, "Hussain Sagar", "NATURE", "09:30", 6.0)),
                rain);
    }

    @Test
    void systemPrompt_containsPlanAndRules() {
        String p = TripAssistant.systemPrompt(trip(List.of(20, 60)));
        assertThat(p).contains("Hyderabad").contains("Charminar").contains("Day 2").contains("day 2 60%");
        assertThat(p).contains("Do not invent");
    }

    @Test
    void fallback_dayQuestion_listsThatDay() {
        String a = TripAssistant.fallback("What's on day 1?", trip(List.of()));
        assertThat(a).contains("Charminar").contains("Paradise Biryani").doesNotContain("Hussain");
    }

    @Test
    void fallback_lastDay_usesTripLength() {
        assertThat(TripAssistant.fallback("what about the last day", trip(List.of()))).contains("Hussain Sagar");
    }

    @Test
    void fallback_cost_splitsPerPerson() {
        assertThat(TripAssistant.fallback("how much will it cost?", trip(List.of()))).contains("12000").contains("3000");
    }

    @Test
    void fallback_packing_addsUmbrellaWhenRainy() {
        assertThat(TripAssistant.fallback("what should I pack", trip(List.of(10, 70)))).contains("umbrella");
        assertThat(TripAssistant.fallback("what should I pack", trip(List.of(10, 10)))).doesNotContain("umbrella");
    }

    @Test
    void fallback_pace_mentionsBusiestDay() {
        assertThat(TripAssistant.fallback("is it too tiring?", trip(List.of()))).contains("2 stops");
    }

    @Test
    void fallback_noStops_isFriendly() {
        Trip empty = new Trip("Goa", 1, null, 1, null, null, List.of(), List.of());
        assertThat(TripAssistant.fallback("anything", empty)).contains("no stops");
    }

    @Test
    void fallback_unknownQuestion_givesOverviewWithHints() {
        assertThat(TripAssistant.fallback("hello there", trip(List.of()))).contains("Charminar").contains("packing");
    }

    @Test
    void rateLimiter_blocksOverLimitThenAllowsAfterWindow() {
        AssistantRateLimiter l = new AssistantRateLimiter(2, 1000);
        assertThat(l.allow("a", 0)).isTrue();
        assertThat(l.allow("a", 10)).isTrue();
        assertThat(l.allow("a", 20)).isFalse();
        assertThat(l.allow("b", 20)).isTrue();
        assertThat(l.allow("a", 1500)).isTrue();
    }
}
