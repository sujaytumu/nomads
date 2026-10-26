package com.tripfactory.nomad.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.service.engine.Achievements.Badge;
import com.tripfactory.nomad.service.engine.Achievements.Input;
import com.tripfactory.nomad.service.engine.Achievements.Level;

class AchievementsTest {

    private static Badge find(List<Badge> badges, String code) {
        return badges.stream().filter(b -> b.code().equals(code)).findFirst().orElseThrow();
    }

    @Test
    void brandNewTravellerHasNoBadgesAndIsLevelOne() {
        Input in = new Input(0, 0, 0, 0, 0, 0, null);
        assertTrue(Achievements.badges(in).stream().noneMatch(Badge::earned));
        assertEquals(0, Achievements.xp(in));
        Level l = Achievements.level(0);
        assertEquals(1, l.level());
        assertEquals("Newcomer", l.title());
        assertEquals(0, l.percentToNext());
    }

    @Test
    void firstTripUnlocksFirstStepsOnly() {
        List<Badge> badges = Achievements.badges(new Input(1, 3, 1, 5.0, 0, 0, Map.of(InterestType.FOOD, 1)));
        assertTrue(find(badges, "FIRST_TRIP").earned());
        assertFalse(find(badges, "WEEKEND_WARRIOR").earned());
        assertEquals(1, find(badges, "WEEKEND_WARRIOR").current());
        assertEquals(3, find(badges, "WEEKEND_WARRIOR").target());
    }

    @Test
    void progressIsCappedAtTheTarget() {
        List<Badge> badges = Achievements.badges(new Input(25, 0, 0, 0, 0, 0, null));
        Badge b = find(badges, "GLOBETROTTER");
        assertTrue(b.earned());
        assertEquals(10, b.current());
        assertEquals(10, b.target());
    }

    @Test
    void interestBadgesNeedThreeTripsOfThatInterest() {
        Map<InterestType, Integer> byInterest = new EnumMap<>(InterestType.class);
        byInterest.put(InterestType.FOOD, 3);
        byInterest.put(InterestType.NATURE, 2);
        List<Badge> badges = Achievements.badges(new Input(5, 0, 0, 0, 0, 0, byInterest));
        assertTrue(find(badges, "LOVER_FOOD").earned());
        assertEquals("Foodie", find(badges, "LOVER_FOOD").title());
        assertFalse(find(badges, "LOVER_NATURE").earned());
        assertEquals(2, find(badges, "LOVER_NATURE").current());
    }

    @Test
    void everyInterestHasABadge() {
        List<Badge> badges = Achievements.badges(new Input(0, 0, 0, 0, 0, 0, null));
        for (InterestType t : InterestType.values()) {
            find(badges, "LOVER_" + t.name());
        }
        assertEquals(8 + InterestType.values().length, badges.size());
    }

    @Test
    void xpCombinesTripsPlacesDistanceGroupsAndReviews() {
        // 2 trips (200) + 10 places (150) + 12.4 km (round(24.8)=25) + 1 group (40) + 2 reviews (50) = 465
        assertEquals(465, Achievements.xp(new Input(2, 10, 3, 12.4, 1, 2, null)));
    }

    @Test
    void levelsGetProgressivelyHarder() {
        assertEquals(1, Achievements.level(99).level());
        assertEquals(2, Achievements.level(100).level());
        assertEquals(2, Achievements.level(399).level());
        assertEquals(3, Achievements.level(400).level());
        assertEquals("Explorer", Achievements.level(400).title());
        // Halfway through level 2 (100..400): 250 xp = 50%.
        Level l = Achievements.level(250);
        assertEquals(150, l.xpIntoLevel());
        assertEquals(300, l.xpForNextLevel());
        assertEquals(50, l.percentToNext());
    }

    @Test
    void levelTitleStopsAtTheLastOne() {
        assertEquals("Legend", Achievements.level(1_000_000).title());
        assertEquals(1, Achievements.level(-50).level());
    }
}
