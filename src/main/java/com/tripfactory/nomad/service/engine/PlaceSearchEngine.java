package com.tripfactory.nomad.service.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.service.util.GeoUtils;

/**
 * Search over places: free text, filters, sorting, paging, and facet counts that show how many results
 * each category / city WOULD have. Facets ignore their own filter (pick "Food" and the category facet
 * still shows the other categories), which is what makes a filter panel usable.
 */
public final class PlaceSearchEngine {

    public static final int MAX_PAGE_SIZE = 50;

    private PlaceSearchEngine() {
    }

    public record Item(Long id, String name, String city, double latitude, double longitude,
                       InterestType category, double rating, String imageUrl) {
    }

    public enum Sort { RELEVANCE, RATING, NAME, DISTANCE;

        public static Sort parse(String raw) {
            if (raw == null || raw.isBlank()) return RELEVANCE;
            try {
                return Sort.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return RELEVANCE;
            }
        }
    }

    /** All fields optional. {@code lat}/{@code lon} are only needed for distance sorting and distance shown. */
    public record Query(String text, String city, InterestType category, Double minRating, Sort sort,
                        Double lat, Double lon, int page, int size) {
    }

    public record Hit(Item item, Double distanceKm, int relevance) {
    }

    public record Result(List<Hit> hits, int page, int size, long totalItems, int totalPages,
                         Map<String, Long> categoryFacets, Map<String, Long> cityFacets) {
    }

    public static Result search(List<Item> all, Query q) {
        int size = Math.max(1, Math.min(q.size() <= 0 ? 20 : q.size(), MAX_PAGE_SIZE));
        int page = Math.max(0, q.page());
        Sort sort = q.sort() == null ? Sort.RELEVANCE : q.sort();
        boolean hasPoint = q.lat() != null && q.lon() != null;
        String text = normalise(q.text());

        // Facets: each one is counted with every filter EXCEPT its own.
        Map<String, Long> categoryFacets = new TreeMap<>();
        Map<String, Long> cityFacets = new TreeMap<>();
        List<Hit> matches = new ArrayList<>();

        for (Item item : all) {
            int relevance = relevance(item, text);
            if (relevance < 0) continue;
            boolean ratingOk = q.minRating() == null || item.rating() >= q.minRating();
            if (!ratingOk) continue;

            boolean cityOk = isBlank(q.city()) || item.city().equalsIgnoreCase(q.city().trim());
            boolean categoryOk = q.category() == null || item.category() == q.category();

            if (cityOk) categoryFacets.merge(item.category().name(), 1L, Long::sum);
            if (categoryOk) cityFacets.merge(item.city(), 1L, Long::sum);

            if (cityOk && categoryOk) {
                Double km = hasPoint ? Math.round(GeoUtils.haversineKm(q.lat(), q.lon(), item.latitude(), item.longitude()) * 10.0) / 10.0 : null;
                matches.add(new Hit(item, km, relevance));
            }
        }

        matches.sort(comparator(sort, hasPoint, !text.isEmpty()));

        long total = matches.size();
        int totalPages = (int) ((total + size - 1) / size);
        int from = (int) Math.min((long) page * size, total);
        int to = (int) Math.min(from + (long) size, total);
        return new Result(List.copyOf(matches.subList(from, to)), page, size, total, totalPages,
                sortedByCount(categoryFacets), sortedByCount(cityFacets));
    }

    /** -1 = does not match the text. Higher = better: exact name 100, name starts 80, name word starts 60, name contains 40, city 20. */
    static int relevance(Item item, String text) {
        if (text.isEmpty()) return 0;
        String name = normalise(item.name());
        String city = normalise(item.city());
        String cat = item.category().name().toLowerCase(Locale.ROOT);
        if (name.equals(text)) return 100;
        if (name.startsWith(text)) return 80;
        for (String word : name.split("\\s+")) {
            if (word.startsWith(text)) return 60;
        }
        if (name.contains(text)) return 40;
        if (city.contains(text) || cat.contains(text)) return 20;
        // Every word of a multi-word query must appear somewhere (name, city or category).
        String[] words = text.split("\\s+");
        if (words.length > 1) {
            String haystack = name + " " + city + " " + cat;
            for (String w : words) {
                if (!haystack.contains(w)) return -1;
            }
            return 30;
        }
        return -1;
    }

    private static Comparator<Hit> comparator(Sort sort, boolean hasPoint, boolean hasText) {
        Comparator<Hit> byName = Comparator.comparing(h -> h.item().name().toLowerCase(Locale.ROOT));
        Comparator<Hit> byRatingDesc = Comparator.comparingDouble((Hit h) -> h.item().rating()).reversed();
        Comparator<Hit> byRelevanceDesc = Comparator.comparingInt(Hit::relevance).reversed();
        Comparator<Hit> byId = Comparator.comparing(h -> h.item().id(), Comparator.nullsLast(Comparator.naturalOrder()));
        switch (sort) {
            case RATING: return byRatingDesc.thenComparing(byName).thenComparing(byId);
            case NAME: return byName.thenComparing(byId);
            case DISTANCE:
                if (hasPoint) {
                    return Comparator.comparingDouble((Hit h) -> h.distanceKm() == null ? Double.MAX_VALUE : h.distanceKm())
                            .thenComparing(byRatingDesc).thenComparing(byId);
                }
                return byRatingDesc.thenComparing(byName).thenComparing(byId);
            default:
                // With a search text, the best text match first; without one, best rated first.
                return hasText ? byRelevanceDesc.thenComparing(byRatingDesc).thenComparing(byName).thenComparing(byId)
                               : byRatingDesc.thenComparing(byName).thenComparing(byId);
        }
    }

    private static Map<String, Long> sortedByCount(Map<String, Long> in) {
        Map<String, Long> out = new LinkedHashMap<>();
        in.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }

    private static String normalise(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
