package com.tripfactory.nomad.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.service.engine.JournalExport.Entry;

class JournalExportTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 14);

    private static Entry e(int day, String mood, String text, int hour, int minute) {
        return new Entry(day, mood, text, LocalDateTime.of(2026, 11, 14 + day - 1, hour, minute));
    }

    @Test
    void emptyJournalSaysSo() {
        String out = JournalExport.toText("Goa", DATE, List.of());
        assertTrue(out.startsWith("My Goa trip - 14 Nov 2026"), out);
        assertTrue(out.contains("(No notes yet)"));
    }

    @Test
    void notesAreGroupedByDayWithDatesAndSortedByTime() {
        List<Entry> entries = new ArrayList<>(List.of(
            e(2, "🤩", "Baga beach sunrise", 6, 30),
            e(1, null, "Landed, hotel is great", 14, 5),
            e(1, "😋", "Fish thali at lunch", 13, 10)));
        String out = JournalExport.toText("Goa", DATE, entries);
        int day1 = out.indexOf("Day 1 (14 Nov 2026)");
        int day2 = out.indexOf("Day 2 (15 Nov 2026)");
        assertTrue(day1 > 0 && day2 > day1, out);
        assertTrue(out.indexOf("Fish thali") < out.indexOf("Landed"), "earlier note first:\n" + out);
        assertTrue(out.contains("13:10 😋 Fish thali at lunch"), out);
        assertTrue(out.contains("14:05 Landed, hotel is great"), out);
        assertTrue(out.contains("3 notes"), out);
    }

    @Test
    void worksWithoutATripDate() {
        String out = JournalExport.toText("Pune", null, List.of(e(1, null, "Hello", 9, 0)));
        assertTrue(out.startsWith("My Pune trip\n"), out);
        assertTrue(out.contains("Day 1\n"), out);
        assertTrue(out.contains("1 note "), out);
    }

    @Test
    void multiLineNotesStayOnOneLine() {
        String out = JournalExport.toText("Goa", DATE, List.of(e(1, null, "line one\n\nline two", 9, 0)));
        assertTrue(out.contains("line one line two"), out);
    }

    @Test
    void missingCityFallsBackToWeekend() {
        assertTrue(JournalExport.toText(null, DATE, List.of()).startsWith("My weekend trip"));
    }

    @Test
    void fileNamesAreSafe() {
        assertEquals("nomad-goa-journal.txt", JournalExport.fileName("Goa"));
        assertEquals("nomad-new-delhi-journal.txt", JournalExport.fileName("  New  Delhi! "));
        assertEquals("nomad-trip-journal.txt", JournalExport.fileName(null));
        assertEquals("nomad-trip-journal.txt", JournalExport.fileName("../.."));
    }
}
