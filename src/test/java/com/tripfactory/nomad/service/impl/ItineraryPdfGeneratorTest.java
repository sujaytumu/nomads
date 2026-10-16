package com.tripfactory.nomad.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.domain.entity.Payment;
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TravelMode;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.domain.enums.WeekendType;

class ItineraryPdfGeneratorTest {

    private final ItineraryPdfGenerator generator = new ItineraryPdfGenerator();

    private TripRequest trip(TripStatus status) {
        User user = new User();
        user.setName("Asha Rao");
        user.setEmail("a@b.com");

        TripRequest trip = new TripRequest();
        trip.setId(12L);
        trip.setUser(user);
        trip.setCity("Bengaluru");
        trip.setWeekendType(WeekendType.TWO_DAY);
        trip.setInterest(InterestType.CULTURE);
        trip.setTravelMode(TravelMode.SOLO);
        trip.setPickupRequired(Boolean.FALSE);
        trip.setStatus(status);
        return trip;
    }

    private TripPlan stop(int day, int hour, String placeName, double km) {
        Place place = new Place();
        place.setName(placeName);
        place.setCity("Bengaluru");
        place.setCategory(InterestType.CULTURE);
        place.setRating(4.5);

        TripPlan plan = new TripPlan();
        plan.setDayNumber(day);
        plan.setPlace(place);
        plan.setStartTime(LocalTime.of(hour, 0));
        plan.setEndTime(LocalTime.of(hour + 2, 0));
        plan.setDistanceFromPrevious(km);
        return plan;
    }

    private String textOf(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // Letter-spaced and kerned text can come out with stray spaces, so compare without whitespace.
            return new PDFTextStripper().getText(document).replaceAll("\\s+", "");
        }
    }

    private int pagesOf(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }

    private Payment payment() {
        Payment payment = new Payment();
        payment.setRazorpayOrderId("order_1");
        payment.setRazorpayPaymentId("pay_ABC123");
        payment.setCreatedAt(LocalDateTime.of(2026, 10, 16, 9, 30));
        return payment;
    }

    private void saveSample(String name, byte[] pdf) throws IOException {
        // Kept so CI (and anyone locally) can look at the rendered design.
        Path dir = Path.of("target", "sample-pdfs");
        Files.createDirectories(dir);
        Files.write(dir.resolve(name), pdf);
    }

    @Test
    void generate_producesAReadablePdfWithTheTripDetails() throws IOException {
        List<TripPlan> plans = List.of(
                stop(1, 9, "Lalbagh Botanical Garden", 3.2),
                stop(1, 11, "Bangalore Palace", 4.1),
                stop(2, 9, "Cubbon Park", 2.0));

        byte[] pdf = generator.generate(trip(TripStatus.CONFIRMED), plans, new BigDecimal("1500"), payment());
        saveSample("itinerary-booked.pdf", pdf);

        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        String text = textOf(pdf);
        assertThat(text)
                .contains("Bengaluru")
                .contains("BOOKINGCONFIRMED")
                .contains("DAY1")
                .contains("DAY2")
                .contains("LalbaghBotanicalGarden")
                .contains("BangalorePalace")
                .contains("CubbonPark")
                .contains("09:00")
                .contains("pay_ABC123")
                .contains("Paidinfull")
                .contains("Rs.1,500");
    }

    @Test
    void generate_labelsAnUnpaidTripAsNotBooked() throws IOException {
        byte[] pdf = generator.generate(trip(TripStatus.PLANNED),
                List.of(stop(1, 9, "Cubbon Park", 2.0)), new BigDecimal("500"), null);

        assertThat(textOf(pdf)).contains("PLANNED-NOTBOOKEDYET").contains("ESTIMATEDCOST");
    }

    @Test
    void generate_packageBookingWithoutStopsStillMakesAPdf() throws IOException {
        byte[] pdf = generator.generate(trip(TripStatus.CONFIRMED), List.of(), new BigDecimal("4999"), payment());
        saveSample("package-booked.pdf", pdf);

        assertThat(pagesOf(pdf)).isEqualTo(1);
        assertThat(textOf(pdf)).contains("Packagebooking").contains("Rs.4,999");
    }

    @Test
    void generate_doesNotFailOnCharactersHelveticaCannotEncode() throws IOException {
        // The built-in PDF font throws on these unless they are cleaned first.
        TripRequest trip = trip(TripStatus.PLANNED);
        trip.setCity("Pondichéry – ₹ हिन्दी");

        byte[] pdf = generator.generate(trip,
                List.of(stop(1, 9, "Café ’Nomad’ 日本", 1.0)), new BigDecimal("500"), null);

        assertThat(pagesOf(pdf)).isEqualTo(1);
        assertThat(textOf(pdf)).contains("Pondichery");
    }

    @Test
    void generate_startsANewPageWhenTheItineraryIsLong() throws IOException {
        List<TripPlan> plans = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            plans.add(stop(1 + i / 20, 9, "Stop number " + i, 1.0));
        }

        byte[] pdf = generator.generate(trip(TripStatus.PLANNED), plans, new BigDecimal("20000"), null);
        saveSample("itinerary-long.pdf", pdf);

        assertThat(pagesOf(pdf)).isGreaterThan(1);
        String text = textOf(pdf);
        assertThat(text).contains("Stopnumber0").contains("Stopnumber39").contains("Page1of");
    }

    @Test
    void generate_truncatesAVeryLongPlaceName() throws IOException {
        String longName = "A".repeat(300);

        byte[] pdf = generator.generate(trip(TripStatus.PLANNED),
                List.of(stop(1, 9, longName, 1.0)), new BigDecimal("500"), null);

        assertThat(pagesOf(pdf)).isEqualTo(1);
        assertThat(textOf(pdf)).contains("...");
    }

    @Test
    void ascii_replacesRupeeAndStripsAccents() {
        assertThat(ItineraryPdfGenerator.ascii("₹500")).isEqualTo("Rs.500");
        assertThat(ItineraryPdfGenerator.ascii("Café")).isEqualTo("Cafe");
        assertThat(ItineraryPdfGenerator.ascii("a–b")).isEqualTo("a-b");
        assertThat(ItineraryPdfGenerator.ascii(null)).isEqualTo("-");
        assertThat(ItineraryPdfGenerator.ascii("   ")).isEqualTo("-");
    }
}
