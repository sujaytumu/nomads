package com.tripfactory.nomad.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;

/** Day-by-day itinerary PDF (PDFBox, multi-page aware). */
@Component
public class ItineraryPdfGenerator {

    private static final float MARGIN = 60;
    private static final float LINE = 16;

    public byte[] generate(TripRequest trip, List<TripPlan> plans) {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font body = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            Cursor c = new Cursor(doc);
            c.line(bold, 22, "NOMAD - Trip Itinerary");
            c.gap(6);
            c.line(body, 11, "Trip #" + trip.getId() + " - " + safe(trip.getCity()));
            c.line(body, 11, "Traveller: " + safe(trip.getUser().getName()));
            c.gap(14);

            if (plans.isEmpty()) {
                c.line(body, 11, "No stops have been planned for this trip yet.");
            }
            Integer currentDay = null;
            for (TripPlan p : plans) {
                if (!p.getDayNumber().equals(currentDay)) {
                    currentDay = p.getDayNumber();
                    c.gap(10);
                    c.line(bold, 14, "Day " + currentDay);
                }
                c.line(body, 11, p.getStartTime() + " - " + p.getEndTime() + "   " + safe(p.getPlace().getName()));
                if (p.getDistanceFromPrevious() != null && p.getDistanceFromPrevious() > 0) {
                    c.line(body, 9, String.format("      %.1f km from previous stop", p.getDistanceFromPrevious()));
                }
            }
            c.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate itinerary PDF", ex);
        }
    }

    /** Standard-14 fonts only support WinAnsi; replace anything else so PDFBox never throws. */
    static String safe(String s) {
        if (s == null) return "-";
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            sb.append(ch >= 32 && ch <= 126 ? ch : '?');
        }
        return sb.toString();
    }

    private static final class Cursor {
        private final PDDocument doc;
        private PDPageContentStream stream;
        private float y;

        Cursor(PDDocument doc) throws IOException {
            this.doc = doc;
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        void gap(float h) {
            y -= h;
        }

        void line(PDType1Font font, float size, String text) throws IOException {
            if (y < MARGIN) newPage();
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(text);
            stream.endText();
            y -= Math.max(LINE, size + 4);
        }

        void close() throws IOException {
            stream.close();
        }
    }
}
