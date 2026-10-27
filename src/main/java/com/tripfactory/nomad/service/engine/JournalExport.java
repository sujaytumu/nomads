package com.tripfactory.nomad.service.engine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Turns a trip's journal notes into a plain-text diary the traveller can download or share. Pure, so it is easy to test. */
public final class JournalExport {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private JournalExport() {
    }

    public record Entry(int day, String mood, String text, LocalDateTime writtenAt) {
    }

    public static String toText(String city, LocalDate tripDate, List<Entry> entries) {
        StringBuilder sb = new StringBuilder();
        String place = city == null || city.isBlank() ? "weekend" : city.trim();
        sb.append("My ").append(place).append(" trip");
        if (tripDate != null) {
            sb.append(" - ").append(DAY.format(tripDate));
        }
        sb.append("\n");

        if (entries == null || entries.isEmpty()) {
            sb.append("\n(No notes yet)\n");
            return sb.toString();
        }

        Map<Integer, List<Entry>> byDay = new TreeMap<>();
        for (Entry e : entries) {
            byDay.computeIfAbsent(Math.max(1, e.day()), k -> new ArrayList<>()).add(e);
        }
        for (Map.Entry<Integer, List<Entry>> day : byDay.entrySet()) {
            sb.append("\nDay ").append(day.getKey());
            if (tripDate != null) {
                sb.append(" (").append(DAY.format(tripDate.plusDays(day.getKey() - 1L))).append(")");
            }
            sb.append("\n");
            day.getValue().sort(Comparator.comparing(Entry::writtenAt, Comparator.nullsLast(Comparator.naturalOrder())));
            for (Entry e : day.getValue()) {
                sb.append("  - ");
                if (e.writtenAt() != null) {
                    sb.append(TIME.format(e.writtenAt())).append(" ");
                }
                if (e.mood() != null && !e.mood().isBlank()) {
                    sb.append(e.mood()).append(" ");
                }
                sb.append(e.text() == null ? "" : e.text().trim().replaceAll("\\s*\\R\\s*", " "));
                sb.append("\n");
            }
        }
        sb.append("\n").append(entries.size()).append(entries.size() == 1 ? " note" : " notes").append(" - written in NOMAD\n");
        return sb.toString();
    }

    /** A safe file name for the download, e.g. "nomad-goa-journal.txt". */
    public static String fileName(String city) {
        String base = city == null ? "" : city.trim().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return "nomad-" + (base.isEmpty() ? "trip" : base) + "-journal.txt";
    }
}
