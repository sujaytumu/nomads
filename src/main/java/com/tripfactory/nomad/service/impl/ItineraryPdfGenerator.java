package com.tripfactory.nomad.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.springframework.stereotype.Component;

import com.tripfactory.nomad.domain.entity.Payment;
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TripStatus;

/**
 * Builds the trip itinerary PDF (the document emailed after a completed booking
 * and downloadable from the app) with Apache PDFBox.
 *
 * Layout notes:
 * - All drawing helpers take distances from the TOP of the page, which is much
 *   easier to reason about than PDF's bottom-left origin.
 * - PDFBox's built-in Helvetica can only encode a small character set and
 *   throws on anything else (rupee sign, accents, other scripts), so every
 *   string is reduced to plain ASCII before it is drawn.
 */
@Component
public class ItineraryPdfGenerator {

    private static final float W = PDRectangle.A4.getWidth();
    private static final float H = PDRectangle.A4.getHeight();
    private static final float M = 40f;
    private static final float CW = W - 2 * M;
    // Content must stay above this line; the page footer lives below it.
    private static final float BOTTOM = H - 64f;
    private static final float KAPPA = 0.5522847498f;

    private static final int INK = 0x151B3A;
    private static final int MUTED = 0x667085;
    private static final int LINE = 0xE4E7EC;
    private static final int PANEL = 0xF5F7FF;
    private static final int BRAND = 0x4F6CFF;
    private static final int DEEP = 0x1B2559;
    private static final int SOFT = 0xC7D2FE;
    private static final int WHITE = 0xFFFFFF;
    private static final int GREEN = 0x12B76A;
    private static final int AMBER = 0xF5B546;
    private static final int RED = 0xD92D20;
    private static final int SHADOW = 0x101840;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm",
            Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /** Tinted background and text colour for each place category. */
    private static final Map<InterestType, int[]> CATEGORY_COLORS = Map.of(
            InterestType.FOOD, new int[] { 0xFFF1E6, 0xC2410C },
            InterestType.CULTURE, new int[] { 0xF3E8FF, 0x7E22CE },
            InterestType.NATURE, new int[] { 0xE6F7EE, 0x15803D },
            InterestType.ADVENTURE, new int[] { 0xFEE9E7, 0xB91C1C },
            InterestType.SHOPPING, new int[] { 0xFDE8F3, 0xBE185D },
            InterestType.NIGHTLIFE, new int[] { 0xE8EBFF, 0x3730A3 },
            InterestType.RELAXATION, new int[] { 0xE0F5F5, 0x0F766E });
    private static final int[] DEFAULT_CATEGORY = { 0xEEF0F4, 0x475467 };

    /**
     * @param payment the captured payment for this trip, or null if it hasn't been paid
     * @param plans   the day-by-day stops; empty for package bookings, which have no itinerary
     */
    public byte[] generate(TripRequest trip, List<TripPlan> plans, BigDecimal estimatedCost, Payment payment) {
        try (PDDocument pdf = new PDDocument()) {
            Canvas c = new Canvas(pdf);
            boolean booked = trip.getStatus() == TripStatus.CONFIRMED;

            drawHero(c, trip, plans, booked);
            drawSummary(c, trip, plans, estimatedCost, booked);
            drawBookingDetails(c, trip, payment, booked);
            if (plans.isEmpty()) {
                drawPackageNote(c);
            } else {
                drawDays(c, plans);
            }
            drawGoodToKnow(c, trip);
            c.finish();
            c.footers(trip.getId());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdf.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate itinerary PDF", ex);
        }
    }

    // ---- sections -------------------------------------------------------

    private void drawHero(Canvas c, TripRequest trip, List<TripPlan> plans, boolean booked) throws IOException {
        final float heroHeight = 232f;
        final int strips = 96;
        for (int i = 0; i < strips; i++) {
            double t = Math.pow(i / (double) (strips - 1), 1.15);
            c.rect(i * (W / strips), 0, W / strips + 0.6f, heroHeight, mix(DEEP, BRAND, t), 1f);
        }
        // Soft decorative circles
        c.circle(W - 70, 60, 170, WHITE, 0.07f);
        c.circle(W - 20, 170, 110, WHITE, 0.06f);
        c.circle(70, heroHeight + 10, 120, WHITE, 0.05f);
        // Dotted route motif
        c.dottedRoute();
        c.circle(330, 150, 4.5f, WHITE, 0.9f);
        c.circle(556, 62, 4.5f, AMBER, 1f);

        c.text("NOMAD", M, 46, 15, c.bold, WHITE, 3.4f, Align.LEFT);

        String label;
        int pillColor;
        TripStatus status = trip.getStatus();
        if (status == TripStatus.CONFIRMED) {
            label = "BOOKING CONFIRMED";
            pillColor = GREEN;
        } else if (status == TripStatus.CANCELLED) {
            label = "CANCELLED";
            pillColor = RED;
        } else if (status == TripStatus.PAYMENT_PENDING) {
            label = "AWAITING PAYMENT";
            pillColor = AMBER;
        } else {
            label = "PLANNED - NOT BOOKED YET";
            pillColor = AMBER;
        }
        float pillWidth = c.width(label, c.bold, 8, 1.4f) + 26;
        c.roundRect(W - M - pillWidth, 30, pillWidth, 22, 11, pillColor, 1f, -1);
        c.text(label, W - M - pillWidth / 2, 44.2f, 8, c.bold, WHITE, 1.4f, Align.CENTER);

        c.text(plans.isEmpty() ? "YOUR BOOKING" : "YOUR WEEKEND ITINERARY", M, 104, 9, c.bold, SOFT, 2.6f,
                Align.LEFT);

        String city = ascii(trip.getCity());
        if ("-".equals(city)) {
            city = "Weekend trip";
        }
        float titleSize = 42;
        while (c.width(city, c.bold, titleSize, 0) > CW - 140 && titleSize > 24) {
            titleSize -= 2;
        }
        float titleBase = 104 + 14 + titleSize * 0.78f;
        c.text(city, M, titleBase, titleSize, c.bold, WHITE, 0, Align.LEFT);
        c.text(subtitle(trip, plans), M, titleBase + 26, 12, c.regular, 0xDDE3FF, 0, Align.LEFT);
    }

    private String subtitle(TripRequest trip, List<TripPlan> plans) {
        List<String> parts = new ArrayList<>();
        if (plans.isEmpty()) {
            parts.add("Package getaway");
        } else if (trip.getWeekendType() != null) {
            parts.add("TWO_DAY".equals(trip.getWeekendType().name()) ? "Two-day getaway" : "One-day getaway");
        }
        if (!plans.isEmpty() && trip.getInterest() != null) {
            parts.add(titleCase(trip.getInterest().name()) + " lover");
        }
        if (trip.getTravelMode() != null) {
            parts.add(titleCase(trip.getTravelMode().name()) + " traveller");
        }
        if (Boolean.TRUE.equals(trip.getPickupRequired())) {
            parts.add("Pickup included");
        }
        return String.join("   |   ", parts);
    }

    private void drawSummary(Canvas c, TripRequest trip, List<TripPlan> plans, BigDecimal cost, boolean booked)
            throws IOException {
        final float top = 196f;
        final float height = 80f;
        c.roundRect(M + 1, top + 5, CW, height, 14, SHADOW, 0.10f, -1);
        c.roundRect(M, top + 2, CW, height, 14, SHADOW, 0.06f, -1);
        c.roundRect(M, top, CW, height, 14, WHITE, 1f, LINE);

        String[][] stats;
        String costLabel = booked ? "TOTAL PAID" : (plans.isEmpty() ? "AMOUNT DUE" : "ESTIMATED COST");
        if (plans.isEmpty()) {
            stats = new String[][] {
                    { "BOOKING TYPE", "Package" },
                    { "DESTINATION", ascii(trip.getCity()) },
                    { "STATUS", booked ? "Booked" : statusWord(trip.getStatus()) },
                    { costLabel, money(cost) } };
        } else {
            long days = plans.stream().map(TripPlan::getDayNumber).distinct().count();
            double km = plans.stream().map(TripPlan::getDistanceFromPrevious)
                    .filter(d -> d != null).mapToDouble(Double::doubleValue).sum();
            stats = new String[][] {
                    { "DURATION", days + (days == 1 ? " day" : " days") },
                    { "PLACES", plans.size() + (plans.size() == 1 ? " stop" : " stops") },
                    { "TRAVEL", String.format(Locale.US, "%.1f km", km) },
                    { costLabel, money(cost) } };
        }
        float columnWidth = CW / 4;
        for (int i = 0; i < stats.length; i++) {
            float x = M + i * columnWidth + 22;
            c.text(stats[i][0], x, top + 31, 7.5f, c.bold, MUTED, 1.5f, Align.LEFT);
            c.text(c.fit(stats[i][1], c.bold, 17, columnWidth - 30), x, top + 55, 17, c.bold, INK, 0, Align.LEFT);
            if (i > 0) {
                c.line(M + i * columnWidth, top + 18, M + i * columnWidth, top + height - 18, LINE, 0.8f, false);
            }
        }
        c.cur = top + height + 34;
    }

    private String statusWord(TripStatus status) {
        if (status == TripStatus.CANCELLED) {
            return "Cancelled";
        }
        if (status == TripStatus.PAYMENT_PENDING) {
            return "Awaiting payment";
        }
        return "Planned";
    }

    private void drawBookingDetails(Canvas c, TripRequest trip, Payment payment, boolean booked) throws IOException {
        c.sectionLabel("BOOKING DETAILS");
        final float panelHeight = 112f;
        c.roundRect(M, c.cur, CW, panelHeight, 12, PANEL, 1f, -1);

        float left = M + 20;
        float right = M + CW / 2 + 10;
        float width = CW / 2 - 30;
        float top = c.cur;

        String reference = "-";
        String paidOn = "-";
        if (payment != null) {
            reference = payment.getRazorpayPaymentId() != null && !payment.getRazorpayPaymentId().isBlank()
                    ? payment.getRazorpayPaymentId()
                    : payment.getRazorpayOrderId();
            if (payment.getCreatedAt() != null) {
                paidOn = payment.getCreatedAt().format(DATE_FORMAT);
            }
        }

        c.keyValue("BOOKING ID", "#" + trip.getId(), left, top + 24, width);
        c.keyValue("TRAVELLER", ascii(trip.getUser() == null ? null : trip.getUser().getName()), left, top + 53, width);
        c.keyValue("EMAIL", ascii(trip.getUser() == null ? null : trip.getUser().getEmail()), left, top + 82, width);
        c.keyValue("PAYMENT", booked ? "Paid in full" : "Pending", right, top + 24, width);
        c.keyValue(booked ? "PAYMENT REFERENCE" : "NEXT STEP",
                booked ? ascii(reference) : "Pay in the NOMAD app to confirm", right, top + 53, width);
        c.keyValue(booked ? "PAID ON" : "STATUS", booked ? paidOn : statusWord(trip.getStatus()), right, top + 82,
                width);
        c.cur += panelHeight + 38;
    }

    private void drawPackageNote(Canvas c) throws IOException {
        c.text("Your booking", M, c.cur, 19, c.bold, INK, 0, Align.LEFT);
        c.cur += 24;
        c.roundRect(M, c.cur, CW, 74, 12, PANEL, 1f, -1);
        c.text("Package booking", M + 20, c.cur + 28, 13, c.bold, INK, 0, Align.LEFT);
        c.text("Your package is booked. Open My Trips in NOMAD any time to see this booking,", M + 20,
                c.cur + 47, 9.5f, c.regular, MUTED, 0, Align.LEFT);
        c.text("and the Pickup page to arrange your ride.", M + 20, c.cur + 60, 9.5f, c.regular, MUTED, 0,
                Align.LEFT);
        c.cur += 74 + 10;
    }

    private void drawDays(Canvas c, List<TripPlan> plans) throws IOException {
        c.text("Your itinerary", M, c.cur, 19, c.bold, INK, 0, Align.LEFT);
        c.text("Timings are a guide - linger where you like.", M, c.cur + 17, 9.5f, c.regular, MUTED, 0, Align.LEFT);
        c.cur += 40;

        Map<Integer, List<TripPlan>> byDay = new TreeMap<>();
        for (TripPlan plan : plans) {
            byDay.computeIfAbsent(plan.getDayNumber(), k -> new ArrayList<>()).add(plan);
        }

        for (Map.Entry<Integer, List<TripPlan>> day : byDay.entrySet()) {
            c.ensure(22 + 18 + 58);
            List<TripPlan> stops = day.getValue();
            double km = stops.stream().map(TripPlan::getDistanceFromPrevious)
                    .filter(d -> d != null).mapToDouble(Double::doubleValue).sum();
            String summary = stops.size() + (stops.size() == 1 ? " stop" : " stops")
                    + "  |  " + String.format(Locale.US, "%.1f km", km);

            c.roundRect(M, c.cur, 62, 22, 11, BRAND, 1f, -1);
            c.text("DAY " + day.getKey(), M + 31, c.cur + 14.6f, 8.5f, c.bold, WHITE, 1.6f, Align.CENTER);
            c.text(summary, W - M, c.cur + 15, 9, c.regular, MUTED, 0, Align.RIGHT);
            c.line(M + 74, c.cur + 11, W - M - c.width(summary, c.regular, 9, 0) - 14, c.cur + 11, LINE, 0.8f,
                    false);
            c.cur += 22 + 18;

            PDPage previousPage = null;
            float previousCenter = 0;
            int number = 1;
            for (TripPlan stop : stops) {
                c.ensure(58 + 14);
                float top = c.cur;
                float nodeX = M + 18;
                float cardHeight = 58;
                float center = top + cardHeight / 2;

                if (previousPage == c.page) {
                    c.line(nodeX, previousCenter + 11, nodeX, center - 11, SOFT, 1.4f, true);
                }
                drawStop(c, stop, number, top, cardHeight, nodeX);
                previousPage = c.page;
                previousCenter = center;
                number++;
                c.cur += cardHeight + 14;
            }
            c.cur += 8;
        }
    }

    private void drawStop(Canvas c, TripPlan stop, int number, float top, float height, float nodeX)
            throws IOException {
        Place place = stop.getPlace();
        int[] colors = place != null && place.getCategory() != null
                ? CATEGORY_COLORS.getOrDefault(place.getCategory(), DEFAULT_CATEGORY)
                : DEFAULT_CATEGORY;

        c.roundRect(M + 44, top, CW - 44, height, 11, WHITE, 1f, LINE);
        c.rect(M + 44, top + 14, 3, height - 28, colors[1], 1f);
        c.circle(nodeX, top + height / 2, 11, BRAND, 1f);
        c.text(String.valueOf(number), nodeX, top + height / 2 + 3.6f, 10, c.bold, WHITE, 0, Align.CENTER);

        String start = stop.getStartTime() == null ? "-" : stop.getStartTime().format(TIME_FORMAT);
        String end = stop.getEndTime() == null ? "-" : stop.getEndTime().format(TIME_FORMAT);
        c.text(start, M + 62, top + 26, 15, c.bold, INK, 0, Align.LEFT);
        c.text("to " + end, M + 62, top + 42, 8.5f, c.regular, MUTED, 0, Align.LEFT);
        c.line(M + 128, top + 12, M + 128, top + height - 12, LINE, 0.8f, false);

        float x = M + 142;
        String name = place == null ? "-" : ascii(place.getName());
        c.text(c.fit(name, c.bold, 12.5f, CW - 44 - 98 - 18), x, top + 24, 12.5f, c.bold, INK, 0, Align.LEFT);

        if (place != null && place.getCategory() != null) {
            String category = titleCase(place.getCategory().name()).toUpperCase(Locale.ROOT);
            float chipWidth = c.width(category, c.bold, 7.2f, 1f) + 16;
            c.roundRect(x, top + 33, chipWidth, 15, 7.5f, colors[0], 1f, -1);
            c.text(category, x + chipWidth / 2, top + 43.4f, 7.2f, c.bold, colors[1], 1f, Align.CENTER);
            x += chipWidth + 12;
        }
        if (place != null && place.getRating() != null) {
            c.star(x + 4.5f, top + 40.5f, 5, AMBER);
            String rating = String.format(Locale.US, "%.1f", place.getRating());
            c.text(rating, x + 12, top + 43.6f, 9.5f, c.bold, INK, 0, Align.LEFT);
            x += 12 + c.width(rating, c.bold, 9.5f, 0) + 12;
        }
        if (stop.getDistanceFromPrevious() != null) {
            c.text(String.format(Locale.US, "%.1f km %s", stop.getDistanceFromPrevious(),
                    number == 1 ? "from your start" : "from previous stop"), x, top + 43.6f, 9, c.regular, MUTED, 0,
                    Align.LEFT);
        }
    }

    private void drawGoodToKnow(Canvas c, TripRequest trip) throws IOException {
        c.ensure(150);
        c.cur += 14;
        c.sectionLabel("GOOD TO KNOW");
        c.roundRect(M, c.cur, CW, 96, 12, PANEL, 1f, -1);
        String[] tips = {
                "Opening hours can change - check ahead for the places you care about most.",
                "Carry a government photo ID and keep this itinerary handy on your phone.",
                Boolean.TRUE.equals(trip.getPickupRequired())
                        ? "Pickup assistance is included - open \"Pickup\" in NOMAD to see your vehicle and driver."
                        : "Need a ride? Request pickup assistance from the \"Pickup\" page in NOMAD." };
        for (int i = 0; i < tips.length; i++) {
            float y = c.cur + 24 + i * 24;
            c.circle(M + 22, y - 3, 2.6f, BRAND, 1f);
            c.text(tips[i], M + 34, y, 9.5f, c.regular, INK, 0, Align.LEFT);
        }
        c.cur += 96;
    }

    // ---- text helpers ---------------------------------------------------

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

    private static int mix(int from, int to, double t) {
        int r = (int) Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = (int) Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = (int) Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return (r << 16) | (g << 8) | b;
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
        for (char ch : s.toCharArray()) {
            if (ch < 32) {
                out.append(' ');
            } else if (ch < 127) {
                out.append(ch);
            } else {
                out.append('?');
            }
        }
        String cleaned = out.toString().trim();
        return cleaned.isEmpty() ? "-" : cleaned;
    }

    // ---- drawing --------------------------------------------------------

    private enum Align { LEFT, CENTER, RIGHT }

    private interface Drawing {
        void run() throws IOException;
    }

    /** One document being drawn: pages, the current page and a top-based cursor. */
    private static final class Canvas {
        final PDDocument pdf;
        final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        final List<PDPage> pages = new ArrayList<>();
        PDPage page;
        PDPageContentStream stream;
        float cur;

        Canvas(PDDocument pdf) throws IOException {
            this.pdf = pdf;
            startPage(true);
        }

        private void startPage(boolean first) throws IOException {
            page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);
            pages.add(page);
            stream = new PDPageContentStream(pdf, page);
            if (!first) {
                rect(0, 0, W, 30, DEEP, 1f);
                text("NOMAD", M, 19, 9, bold, WHITE, 2.4f, Align.LEFT);
                cur = 62;
            }
        }

        void ensure(float needed) throws IOException {
            if (cur + needed > BOTTOM) {
                stream.close();
                startPage(false);
            }
        }

        void finish() throws IOException {
            stream.close();
        }

        /** Page numbers need the final page count, so they are added in a second pass. */
        void footers(Long bookingId) throws IOException {
            int total = pages.size();
            for (int i = 0; i < total; i++) {
                page = pages.get(i);
                stream = new PDPageContentStream(pdf, page, PDPageContentStream.AppendMode.APPEND, true, true);
                line(M, H - 40, W - M, H - 40, LINE, 0.7f, false);
                text("NOMAD  |  Smart Weekend Travel & Assistance", M, H - 26, 8, regular, MUTED, 0, Align.LEFT);
                text("Booking #" + bookingId + "  |  Page " + (i + 1) + " of " + total, W - M, H - 26, 8, regular,
                        MUTED, 0, Align.RIGHT);
                stream.close();
            }
        }

        float width(String text, PDType1Font font, float size, float spacing) throws IOException {
            return font.getStringWidth(text) / 1000f * size + spacing * text.length();
        }

        String fit(String text, PDType1Font font, float size, float maxWidth) throws IOException {
            if (width(text, font, size, 0) <= maxWidth) {
                return text;
            }
            String shortened = text;
            while (shortened.length() > 1 && width(shortened + "...", font, size, 0) > maxWidth) {
                shortened = shortened.substring(0, shortened.length() - 1).stripTrailing();
            }
            return shortened + "...";
        }

        void text(String value, float x, float top, float size, PDType1Font font, int color, float spacing,
                Align align) throws IOException {
            float textWidth = width(value, font, size, spacing);
            float px = align == Align.RIGHT ? x - textWidth : align == Align.CENTER ? x - textWidth / 2 : x;
            fillColor(color);
            stream.beginText();
            stream.setFont(font, size);
            if (spacing != 0) {
                stream.setCharacterSpacing(spacing);
            }
            stream.newLineAtOffset(px, H - top);
            stream.showText(value);
            if (spacing != 0) {
                stream.setCharacterSpacing(0);
            }
            stream.endText();
        }

        void keyValue(String key, String value, float x, float top, float maxWidth) throws IOException {
            text(key, x, top, 7.5f, bold, MUTED, 1.2f, Align.LEFT);
            text(fit(value, regular, 10.5f, maxWidth), x, top + 13, 10.5f, regular, INK, 0, Align.LEFT);
        }

        void sectionLabel(String label) throws IOException {
            rect(M, cur - 9, 3, 11, BRAND, 1f);
            text(label, M + 11, cur, 8.5f, bold, INK, 1.8f, Align.LEFT);
            cur += 14;
        }

        void rect(float x, float top, float w, float h, int color, float alpha) throws IOException {
            withAlpha(alpha, () -> {
                fillColor(color);
                stream.addRect(x, H - (top + h), w, h);
                stream.fill();
            });
        }

        /** @param border border colour, or -1 for none */
        void roundRect(float x, float top, float w, float h, float r, int fill, float alpha, int border)
                throws IOException {
            withAlpha(alpha, () -> {
                float k = r * KAPPA;
                float y0 = H - (top + h);
                float y1 = H - top;
                stream.moveTo(x + r, y0);
                stream.lineTo(x + w - r, y0);
                stream.curveTo(x + w - r + k, y0, x + w, y0 + r - k, x + w, y0 + r);
                stream.lineTo(x + w, y1 - r);
                stream.curveTo(x + w, y1 - r + k, x + w - r + k, y1, x + w - r, y1);
                stream.lineTo(x + r, y1);
                stream.curveTo(x + r - k, y1, x, y1 - r + k, x, y1 - r);
                stream.lineTo(x, y0 + r);
                stream.curveTo(x, y0 + r - k, x + r - k, y0, x + r, y0);
                stream.closePath();
                fillColor(fill);
                if (border >= 0) {
                    strokeColor(border);
                    stream.setLineWidth(0.8f);
                    stream.fillAndStroke();
                } else {
                    stream.fill();
                }
            });
        }

        void circle(float cx, float top, float r, int color, float alpha) throws IOException {
            withAlpha(alpha, () -> {
                float k = r * KAPPA;
                float cy = H - top;
                stream.moveTo(cx + r, cy);
                stream.curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r);
                stream.curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy);
                stream.curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r);
                stream.curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy);
                stream.closePath();
                fillColor(color);
                stream.fill();
            });
        }

        void line(float x1, float top1, float x2, float top2, int color, float width, boolean dashed)
                throws IOException {
            stream.saveGraphicsState();
            strokeColor(color);
            stream.setLineWidth(width);
            if (dashed) {
                stream.setLineDashPattern(new float[] { 3, 3 }, 0);
            }
            stream.moveTo(x1, H - top1);
            stream.lineTo(x2, H - top2);
            stream.stroke();
            stream.restoreGraphicsState();
        }

        /** The dotted curve across the hero. */
        void dottedRoute() throws IOException {
            withAlpha(0.5f, () -> {
                strokeColor(WHITE);
                stream.setLineWidth(1.4f);
                stream.setLineCapStyle(1);
                stream.setLineDashPattern(new float[] { 0.1f, 5 }, 0);
                stream.moveTo(330, H - 150);
                stream.curveTo(380, H - 60, 440, H - 190, 500, H - 100);
                stream.curveTo(520, H - 70, 540, H - 70, 556, H - 62);
                stream.stroke();
            });
        }

        void star(float cx, float top, float r, int color) throws IOException {
            fillColor(color);
            for (int i = 0; i < 10; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                float radius = i % 2 == 1 ? r * 0.42f : r;
                float px = cx + (float) (radius * Math.cos(angle));
                float py = H - (top + (float) (radius * Math.sin(angle)));
                if (i == 0) {
                    stream.moveTo(px, py);
                } else {
                    stream.lineTo(px, py);
                }
            }
            stream.closePath();
            stream.fill();
        }

        private void withAlpha(float alpha, Drawing drawing) throws IOException {
            if (alpha >= 1f) {
                drawing.run();
                return;
            }
            stream.saveGraphicsState();
            PDExtendedGraphicsState state = new PDExtendedGraphicsState();
            state.setNonStrokingAlphaConstant(alpha);
            state.setStrokingAlphaConstant(alpha);
            stream.setGraphicsStateParameters(state);
            drawing.run();
            stream.restoreGraphicsState();
        }

        private void fillColor(int rgb) throws IOException {
            stream.setNonStrokingColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
        }

        private void strokeColor(int rgb) throws IOException {
            stream.setStrokingColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
        }
    }
}
