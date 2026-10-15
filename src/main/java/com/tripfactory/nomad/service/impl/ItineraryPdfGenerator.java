package com.tripfactory.nomad.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;

/**
 * Builds a printable day-by-day trip itinerary as a PDF using Apache PDFBox
 * (same library as the payment receipt).
 *
 * PDFBox's built-in Helvetica can only encode a small character set and throws
 * on anything else (the rupee sign, accented letters, other scripts), so every
 * string is reduced to plain ASCII before it is drawn.
 */
@Component
public class ItineraryPdfGenerator {

    private static final float PAGE_W = PDRectangle.A4.getWidth();
    private static final float PAGE_H = PDRectangle.A4.getHeight();
    private static final float MARGIN = 50f;
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN;
    // Content must stay above this line; the page footer lives below it.
    private static final float BOTTOM = 70f;
    private static final float BAND_H = 90f;
    private static final float TIME_COL_W = 100f;

    private static final float[] BRAND = { 0.31f, 0.42f, 1f };
    private static final float[] INK = { 0.10f, 0.12f, 0.20f };
    private static final float[] MUTED = { 0.40f, 0.45f, 0.55f };
    private static final float[] PANEL = { 0.95f, 0.96f, 0.99f };
    private static final float[] WHITE = { 1f, 1f, 1f };
    private static final float[] GREEN = { 0.10f, 0.55f, 0.30f };
    private static final float[] AMBER = { 0.70f, 0.45f, 0.00f };
    private static final float[] RED = { 0.75f, 0.20f, 0.20f };

    private static final DateTimeFormatter GENERATED_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm",
            Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public byte[] generate(TripRequest trip, List<TripPlan> plans, BigDecimal estimatedCost) {
        try (PDDocument document = new PDDocument()) {
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            Page page = new Page(document, bold, regular);
            drawHeader(page, trip, plans, estimatedCost);
            drawDays(page, plans);
            page.close();

            addFooters(document, regular);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate itinerary PDF", ex);
        }
    }

    // ---- sections -------------------------------------------------------

    private void drawHeader(Page p, TripRequest trip, List<TripPlan> plans, BigDecimal estimatedCost)
            throws IOException {
        p.rect(0, PAGE_H - BAND_H, PAGE_W, BAND_H, BRAND);
        p.textAt(p.bold, 26, MARGIN, PAGE_H - 45, "NOMAD", WHITE);
        p.textAt(p.regular, 11, MARGIN, PAGE_H - 66, "Weekend itinerary", WHITE);
        p.y = PAGE_H - BAND_H - 36;

        String city = ascii(trip.getCity());
        p.textAt(p.bold, 20, MARGIN, p.y, "-".equals(city) ? "Weekend trip" : city + " weekend trip", INK);
        p.y -= 22;

        List<String> meta = new ArrayList<>();
        meta.add("Trip #" + trip.getId());
        if (trip.getWeekendType() != null) {
            meta.add("TWO_DAY".equals(trip.getWeekendType().name()) ? "Two-day trip" : "One-day trip");
        }
        if (trip.getInterest() != null) {
            meta.add("Interest: " + titleCase(trip.getInterest().name()));
        }
        if (trip.getTravelMode() != null) {
            meta.add(titleCase(trip.getTravelMode().name()));
        }
        if (Boolean.TRUE.equals(trip.getPickupRequired())) {
            meta.add("Pickup included");
        }
        p.textAt(p.regular, 10, MARGIN, p.y, String.join("   |   ", meta), MUTED);
        p.y -= 20;

        float[] statusColor = AMBER;
        String status = "Planned - not booked yet";
        if (trip.getStatus() != null) {
            switch (trip.getStatus()) {
                case CONFIRMED -> {
                    status = "Booked";
                    statusColor = GREEN;
                }
                case CANCELLED -> {
                    status = "Cancelled";
                    statusColor = RED;
                }
                case PAYMENT_PENDING -> status = "Awaiting payment";
                default -> status = "Planned - not booked yet";
            }
        }
        p.textAt(p.bold, 11, MARGIN, p.y, "Status: " + status, statusColor);
        p.y -= 28;

        double travelKm = plans.stream()
                .map(TripPlan::getDistanceFromPrevious)
                .filter(d -> d != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        float boxW = CONTENT_W / 3f;
        float boxH = 48f;
        p.rect(MARGIN, p.y - boxH, CONTENT_W, boxH, PANEL);
        stat(p, 0, boxW, boxH, "STOPS", String.valueOf(plans.size()));
        stat(p, 1, boxW, boxH, "APPROX. TRAVEL", String.format(Locale.US, "%.1f km", travelKm));
        stat(p, 2, boxW, boxH, "ESTIMATED COST", money(estimatedCost));
        p.y -= boxH + 28;
    }

    private void stat(Page p, int index, float boxW, float boxH, String label, String value) throws IOException {
        float x = MARGIN + index * boxW + 14;
        p.textAt(p.regular, 8, x, p.y - 16, label, MUTED);
        p.textAt(p.bold, 14, x, p.y - 36, value, INK);
    }

    private void drawDays(Page p, List<TripPlan> plans) throws IOException {
        Map<Integer, List<TripPlan>> byDay = new TreeMap<>();
        for (TripPlan plan : plans) {
            byDay.computeIfAbsent(plan.getDayNumber(), k -> new ArrayList<>()).add(plan);
        }

        for (Map.Entry<Integer, List<TripPlan>> day : byDay.entrySet()) {
            // Keep a day heading together with at least its first stop.
            p.ensureSpace(24 + 44);
            p.textAt(p.bold, 15, MARGIN, p.y, "Day " + day.getKey(), BRAND);
            p.y -= 8;
            p.line(MARGIN, p.y, MARGIN + CONTENT_W, p.y);
            p.y -= 20;

            int number = 1;
            for (TripPlan plan : day.getValue()) {
                p.ensureSpace(44);
                drawStop(p, plan, number++);
            }
            p.y -= 10;
        }
    }

    private void drawStop(Page p, TripPlan plan, int number) throws IOException {
        Place place = plan.getPlace();
        String time = plan.getStartTime() == null || plan.getEndTime() == null
                ? "-"
                : plan.getStartTime().format(TIME_FORMAT) + " - " + plan.getEndTime().format(TIME_FORMAT);
        p.textAt(p.bold, 11, MARGIN, p.y, time, INK);

        float x = MARGIN + TIME_COL_W;
        float maxWidth = CONTENT_W - TIME_COL_W;
        String name = number + ". " + (place == null ? "-" : ascii(place.getName()));
        p.textAt(p.bold, 11, x, p.y, fit(p.bold, 11, name, maxWidth), INK);

        List<String> details = new ArrayList<>();
        if (place != null && place.getCategory() != null) {
            details.add(titleCase(place.getCategory().name()));
        }
        if (place != null && place.getRating() != null) {
            details.add(String.format(Locale.US, "Rating %.1f", place.getRating()));
        }
        if (plan.getDistanceFromPrevious() != null) {
            details.add(String.format(Locale.US, "%.1f km %s", plan.getDistanceFromPrevious(),
                    number == 1 ? "from your starting point" : "from previous stop"));
        }
        p.textAt(p.regular, 9, x, p.y - 14, fit(p.regular, 9, String.join("  |  ", details), maxWidth), MUTED);
        p.y -= 38;
    }

    private void addFooters(PDDocument document, PDType1Font font) throws IOException {
        int total = document.getNumberOfPages();
        String generated = "Generated by NOMAD on " + LocalDateTime.now().format(GENERATED_FORMAT);
        for (int i = 0; i < total; i++) {
            PDPage page = document.getPage(i);
            try (PDPageContentStream stream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                String pageLabel = "Page " + (i + 1) + " of " + total;
                float labelWidth = font.getStringWidth(pageLabel) / 1000f * 9;
                drawText(stream, font, 9, MARGIN, 32, generated, MUTED);
                drawText(stream, font, 9, PAGE_W - MARGIN - labelWidth, 32, pageLabel, MUTED);
            }
        }
    }

    // ---- low-level helpers ---------------------------------------------

    private static void drawText(PDPageContentStream stream, PDType1Font font, float size, float x, float y,
            String text, float[] color) throws IOException {
        stream.setNonStrokingColor(color[0], color[1], color[2]);
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    private static String fit(PDType1Font font, float size, String text, float maxWidth) throws IOException {
        if (width(font, size, text) <= maxWidth) {
            return text;
        }
        String shortened = text;
        while (shortened.length() > 1 && width(font, size, shortened + "...") > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "...";
    }

    private static float width(PDType1Font font, float size, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * size;
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        DecimalFormat format = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
        return "Rs. " + format.format(value);
    }

    private static String titleCase(String enumName) {
        String lower = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /** Reduces text to printable ASCII so Helvetica can always encode it. */
    static String ascii(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String s = value
                .replace("₹", "Rs.")
                .replace('–', '-')
                .replace('—', '-')
                .replace('‘', '\'')
                .replace('’', '\'')
                .replace('“', '"')
                .replace('”', '"');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c < 32) {
                out.append(' ');
            } else if (c < 127) {
                out.append(c);
            } else {
                out.append('?');
            }
        }
        return out.toString().trim();
    }

    /** The page being written, with automatic page breaks. */
    private static final class Page {
        final PDDocument document;
        final PDType1Font bold;
        final PDType1Font regular;
        PDPageContentStream stream;
        float y;

        Page(PDDocument document, PDType1Font bold, PDType1Font regular) throws IOException {
            this.document = document;
            this.bold = bold;
            this.regular = regular;
            startPage();
        }

        private void startPage() throws IOException {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_H - MARGIN;
        }

        void ensureSpace(float needed) throws IOException {
            if (y - needed < BOTTOM) {
                stream.close();
                startPage();
            }
        }

        void textAt(PDType1Font font, float size, float x, float baselineY, String text, float[] color)
                throws IOException {
            drawText(stream, font, size, x, baselineY, text, color);
        }

        void rect(float x, float yBottom, float w, float h, float[] color) throws IOException {
            stream.setNonStrokingColor(color[0], color[1], color[2]);
            stream.addRect(x, yBottom, w, h);
            stream.fill();
        }

        void line(float x1, float y1, float x2, float y2) throws IOException {
            stream.setStrokingColor(0.80f, 0.83f, 0.90f);
            stream.setLineWidth(0.7f);
            stream.moveTo(x1, y1);
            stream.lineTo(x2, y2);
            stream.stroke();
        }

        void close() throws IOException {
            stream.close();
        }
    }
}
