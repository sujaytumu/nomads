package com.tripfactory.nomad.service.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tripfactory.nomad.domain.enums.InterestType;

/** Travel XP, levels and badges worked out from a traveller's history. Pure functions, easy to test. */
public final class Achievements {

    private Achievements() {
    }

    /** Everything the rules look at. {@code tripsByInterest} counts booked trips per interest. */
    public record Input(int confirmedTrips, int distinctPlaces, int distinctCities, double kmTravelled,
                        int groupTrips, int reviews, Map<InterestType, Integer> tripsByInterest) {
        public Input {
            tripsByInterest = tripsByInterest == null ? Map.of() : tripsByInterest;
        }
    }

    public record Badge(String code, String title, String description, String icon, int current, int target, boolean earned) {
    }

    public record Level(int level, String title, long xp, long xpIntoLevel, long xpForNextLevel, int percentToNext) {
    }

    private static final String[] LEVEL_TITLES = {
        "Newcomer", "Wanderer", "Explorer", "Trailblazer", "Adventurer", "Globetrotter", "Legend"
    };

    public static long xp(Input in) {
        return 100L * in.confirmedTrips()
                + 15L * in.distinctPlaces()
                + Math.round(2 * in.kmTravelled())
                + 40L * in.groupTrips()
                + 25L * in.reviews();
    }

    /** Level n starts at 100 * (n-1)^2 XP, so every level takes a little longer than the last. */
    public static Level level(long xp) {
        long safe = Math.max(0, xp);
        int level = (int) Math.floor(Math.sqrt(safe / 100.0)) + 1;
        long start = 100L * (level - 1) * (level - 1);
        long next = 100L * level * level;
        long into = safe - start;
        long span = next - start;
        int pct = span <= 0 ? 100 : (int) Math.min(100, (into * 100) / span);
        String title = LEVEL_TITLES[Math.min(level - 1, LEVEL_TITLES.length - 1)];
        return new Level(level, title, safe, into, span, pct);
    }

    public static List<Badge> badges(Input in) {
        List<Badge> out = new ArrayList<>();
        out.add(badge("FIRST_TRIP", "First Steps", "Book your first trip", "🎒", in.confirmedTrips(), 1));
        out.add(badge("WEEKEND_WARRIOR", "Weekend Warrior", "Book 3 trips", "⚡", in.confirmedTrips(), 3));
        out.add(badge("GLOBETROTTER", "Globetrotter", "Book 10 trips", "🌍", in.confirmedTrips(), 10));
        out.add(badge("CITY_HOPPER", "City Hopper", "Visit 3 different cities", "🏙️", in.distinctCities(), 3));
        out.add(badge("EXPLORER", "Explorer", "See 10 different places", "🧭", in.distinctPlaces(), 10));
        out.add(badge("MARATHONER", "Marathoner", "Travel 42 km between stops", "🏃", (int) Math.floor(in.kmTravelled()), 42));
        out.add(badge("SOCIAL_BUTTERFLY", "Social Butterfly", "Join 2 group trips", "🦋", in.groupTrips(), 2));
        out.add(badge("CRITIC", "Critic", "Review a trip", "⭐", in.reviews(), 1));
        for (InterestType type : InterestType.values()) {
            String[] meta = interestBadge(type);
            out.add(badge("LOVER_" + type.name(), meta[0], "Take 3 " + meta[1] + " trips", meta[2],
                    in.tripsByInterest().getOrDefault(type, 0), 3));
        }
        return out;
    }

    private static Badge badge(String code, String title, String description, String icon, int current, int target) {
        return new Badge(code, title, description, icon, Math.min(Math.max(current, 0), target), target, current >= target);
    }

    private static String[] interestBadge(InterestType type) {
        switch (type) {
            case FOOD: return new String[] { "Foodie", "food", "🍜" };
            case CULTURE: return new String[] { "Culture Vulture", "culture", "🏛️" };
            case NATURE: return new String[] { "Nature Lover", "nature", "🌿" };
            case ADVENTURE: return new String[] { "Thrill Seeker", "adventure", "🧗" };
            case SHOPPING: return new String[] { "Shopaholic", "shopping", "🛍️" };
            case NIGHTLIFE: return new String[] { "Night Owl", "nightlife", "🌙" };
            default: return new String[] { "Chill Master", "relaxation", "🧘" };
        }
    }
}
