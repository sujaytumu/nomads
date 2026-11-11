package com.tripfactory.nomad.service.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Everything the trip assistant knows, as plain text, plus a simple answerer that is used when no AI key
 * is configured (or the AI service is down), so the feature never just breaks.
 */
public final class TripAssistant {

    public record Stop(int day, String name, String category, String start, double legKm) {
    }

    public record Trip(String city, int days, String date, int people, String travelMode, String estimatedCost,
            List<Stop> stops, List<Integer> rain) {
    }

    private static final Pattern DAY_NUMBER = Pattern.compile("\\bday\\s*(\\d)\\b");

    private TripAssistant() {
    }

    /** Instructions + trip facts for the AI model. */
    public static String systemPrompt(Trip t) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are NOMAD's friendly trip assistant inside a weekend-trip planning app. ")
                .append("Answer questions about the traveller's trip below, using these facts and general travel knowledge about the city. ")
                .append("Keep answers short (under 120 words), plain text, no markdown. ")
                .append("Do not invent opening hours, prices, bookings or phone numbers; if you are not sure, say so and suggest checking locally. ")
                .append("If the question is not about travel or this trip, politely steer back to the trip.\n\n");
        sb.append(facts(t));
        return sb.toString();
    }

    /** The trip as readable lines. */
    public static String facts(Trip t) {
        StringBuilder sb = new StringBuilder();
        sb.append("TRIP FACTS\n");
        sb.append("City: ").append(nz(t.city(), "unknown")).append('\n');
        sb.append("Length: ").append(t.days()).append(t.days() == 1 ? " day" : " days").append('\n');
        if (t.date() != null) {
            sb.append("Start date: ").append(t.date()).append('\n');
        }
        sb.append("Travellers: ").append(Math.max(1, t.people())).append('\n');
        if (t.travelMode() != null) {
            sb.append("Travel mode: ").append(t.travelMode()).append('\n');
        }
        if (t.estimatedCost() != null) {
            sb.append("Estimated cost (whole trip): Rs ").append(t.estimatedCost()).append('\n');
        }
        if (t.rain() != null && !t.rain().isEmpty()) {
            sb.append("Chance of rain per day: ");
            for (int i = 0; i < t.rain().size(); i++) {
                sb.append(i == 0 ? "" : ", ").append("day ").append(i + 1).append(' ').append(t.rain().get(i)).append('%');
            }
            sb.append('\n');
        }
        if (t.stops().isEmpty()) {
            sb.append("No stops planned yet.\n");
        } else {
            sb.append("Plan:\n");
            for (Stop s : t.stops()) {
                sb.append("Day ").append(s.day()).append(": ");
                if (s.start() != null) {
                    sb.append(s.start()).append(' ');
                }
                sb.append(s.name()).append(" (").append(s.category().toLowerCase(Locale.ROOT));
                if (s.legKm() > 0) {
                    sb.append(", ").append(Math.round(s.legKm() * 10) / 10.0).append(" km from previous");
                }
                sb.append(")\n");
            }
        }
        return sb.toString();
    }

    /** A decent answer from the plan alone. Never throws. */
    public static String fallback(String question, Trip t) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (t.stops().isEmpty()) {
            return "Your trip has no stops yet. Add a few places from the plan page and I can help with the day-by-day details.";
        }

        Matcher m = DAY_NUMBER.matcher(q);
        Integer day = null;
        if (m.find()) {
            day = Integer.parseInt(m.group(1));
        } else if (q.contains("first day") || q.contains("day one")) {
            day = 1;
        } else if (q.contains("second day") || q.contains("day two")) {
            day = 2;
        } else if (q.contains("third day") || q.contains("day three")) {
            day = 3;
        } else if (q.contains("last day")) {
            day = t.days();
        }
        if (day != null) {
            return dayPlan(t, day);
        }

        if (has(q, "pack", "carry", "bring", "wear")) {
            return packing(t);
        }
        if (has(q, "cost", "budget", "price", "spend", "expens", "money", "afford")) {
            String per = perPerson(t);
            return t.estimatedCost() == null
                    ? "I don't have a cost estimate for this trip yet."
                    : "The estimate for the whole trip is about Rs " + t.estimatedCost() + per
                            + ". Entry fees, food and fuel can vary, so keep a small buffer.";
        }
        if (has(q, "weather", "rain", "umbrella", "hot", "cold", "temperature")) {
            return weather(t);
        }
        if (has(q, "tiring", "tired", "busy", "pace", "relax", "too much", "packed", "easy")) {
            return pace(t);
        }
        if (has(q, "food", "eat", "restaurant", "lunch", "dinner", "breakfast", "cafe")) {
            return byCategory(t, "food", "I don't see a food stop in your plan. Add one near the middle of each day so you are not hungry on the road.");
        }
        if (has(q, "start", "when", "time", "early", "leave")) {
            return startTimes(t);
        }
        return overview(t);
    }

    private static String dayPlan(Trip t, int day) {
        List<String> lines = new ArrayList<>();
        for (Stop s : t.stops()) {
            if (s.day() == day) {
                lines.add((s.start() != null ? s.start() + " " : "") + s.name());
            }
        }
        if (lines.isEmpty()) {
            return "There are no stops on day " + day + ". Your trip has " + t.days() + (t.days() == 1 ? " day." : " days.");
        }
        return "Day " + day + ": " + String.join(", then ", lines) + ".";
    }

    private static String packing(Trip t) {
        List<String> items = new ArrayList<>(List.of("water bottle", "phone charger / power bank", "ID and some cash", "comfortable shoes"));
        if (maxRain(t) >= 40) {
            items.add("umbrella or light rain jacket");
        }
        for (Stop s : t.stops()) {
            String c = s.category().toLowerCase(Locale.ROOT);
            if (c.contains("nature") || c.contains("adventure")) {
                items.add("sunscreen and a cap");
                break;
            }
        }
        return "Worth packing: " + String.join(", ", items) + ".";
    }

    private static String weather(Trip t) {
        if (t.rain() == null || t.rain().isEmpty()) {
            return "I don't have a forecast for this trip yet. Check again a few days before you leave.";
        }
        StringBuilder sb = new StringBuilder("Chance of rain: ");
        for (int i = 0; i < t.rain().size(); i++) {
            sb.append(i == 0 ? "" : ", ").append("day ").append(i + 1).append(' ').append(t.rain().get(i)).append('%');
        }
        sb.append(maxRain(t) >= 40 ? ". Carry an umbrella and keep indoor stops as a backup." : ". Looks mostly dry.");
        return sb.toString();
    }

    private static String pace(Trip t) {
        Map<Integer, Integer> perDay = new TreeMap<>();
        double km = 0;
        for (Stop s : t.stops()) {
            perDay.merge(s.day(), 1, Integer::sum);
            km += s.legKm();
        }
        int busiest = perDay.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        String verdict = busiest <= 3 ? "That is a relaxed pace." : busiest == 4 ? "That is a full day, but doable with an early start." : "That is quite packed, so consider dropping one stop per busy day.";
        return "Your busiest day has " + busiest + " stops and the route is about " + Math.round(km) + " km in total. " + verdict;
    }

    private static String byCategory(Trip t, String category, String none) {
        List<String> names = new ArrayList<>();
        for (Stop s : t.stops()) {
            if (s.category().toLowerCase(Locale.ROOT).contains(category)) {
                names.add(s.name() + " (day " + s.day() + ")");
            }
        }
        return names.isEmpty() ? none : "Food stops in your plan: " + String.join(", ", names) + ".";
    }

    private static String startTimes(Trip t) {
        List<String> firsts = new ArrayList<>();
        int seen = 0;
        for (Stop s : t.stops()) {
            if (s.day() != seen) {
                seen = s.day();
                firsts.add("day " + s.day() + (s.start() != null ? " at " + s.start() : "") + " with " + s.name());
            }
        }
        return "Each day starts: " + String.join("; ", firsts) + ". Leaving a little earlier helps beat traffic and crowds.";
    }

    private static String overview(Trip t) {
        int stops = t.stops().size();
        String names = String.join(", ", t.stops().stream().limit(4).map(Stop::name).toList());
        return "Your " + t.days() + "-day" + " trip in " + nz(t.city(), "the city") + " has " + stops
                + (stops == 1 ? " stop" : " stops") + ": " + names + (stops > 4 ? " and more" : "")
                + ". Ask me about a day (\"What's on day 1?\"), packing, weather, cost or whether the pace is too tiring.";
    }

    private static String perPerson(Trip t) {
        try {
            double total = Double.parseDouble(t.estimatedCost());
            int people = Math.max(1, t.people());
            return people > 1 ? " (about Rs " + Math.round(total / people) + " each for " + people + " people)" : "";
        } catch (Exception ex) {
            return "";
        }
    }

    private static int maxRain(Trip t) {
        return t.rain() == null ? 0 : t.rain().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    private static boolean has(String q, String... words) {
        for (String w : words) {
            if (q.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private static String nz(String s, String d) {
        return s == null || s.isBlank() ? d : s;
    }
}
