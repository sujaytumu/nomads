package com.tripfactory.nomad.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.PaymentStatus;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.service.engine.AnalyticsEngine.DayPoint;
import com.tripfactory.nomad.service.engine.AnalyticsEngine.PaymentRow;
import com.tripfactory.nomad.service.engine.AnalyticsEngine.Snapshot;
import com.tripfactory.nomad.service.engine.AnalyticsEngine.TripRow;
import com.tripfactory.nomad.service.engine.AnalyticsEngine.UserRow;

class AnalyticsEngineTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    private static TripRow trip(long id, String city, TripStatus s, InterestType i, int daysAgo) {
        return new TripRow(id, city, s, i, TODAY.minusDays(daysAgo));
    }

    private static PaymentRow pay(long tripId, PaymentStatus s, String amount, int daysAgo) {
        return new PaymentRow(tripId, s, new BigDecimal(amount), TODAY.minusDays(daysAgo));
    }

    private static Snapshot sample() {
        List<UserRow> users = List.of(new UserRow(TODAY), new UserRow(TODAY), new UserRow(TODAY.minusDays(3)), new UserRow(TODAY.minusDays(100)));
        List<TripRow> trips = List.of(
            trip(1, "Goa", TripStatus.CONFIRMED, InterestType.FOOD, 2),
            trip(2, "goa", TripStatus.CONFIRMED, InterestType.NATURE, 1),
            trip(3, "Pune", TripStatus.CONFIRMED, InterestType.FOOD, 0),
            trip(4, "Pune", TripStatus.CANCELLED, InterestType.FOOD, 5),
            trip(5, "Mumbai", TripStatus.PLANNED, InterestType.CULTURE, 0));
        List<PaymentRow> pays = List.of(
            pay(1, PaymentStatus.CAPTURED, "3000.00", 2),
            pay(2, PaymentStatus.CAPTURED, "2000.00", 1),
            pay(3, PaymentStatus.CAPTURED, "1000.00", 0),
            pay(4, PaymentStatus.REFUNDED, "1500.00", 4),
            pay(5, PaymentStatus.CREATED, "800.00", 0));
        return AnalyticsEngine.compute(users, trips, pays, TODAY, 7);
    }

    @Test
    void totalsAddUp() {
        AnalyticsEngine.Totals t = sample().totals();
        assertEquals(4, t.users());
        assertEquals(5, t.trips());
        assertEquals(3, t.bookedTrips());
        assertEquals(1, t.cancelledTrips());
        assertEquals(0.6, t.bookingRate(), 1e-9);
        assertEquals(6000.0, t.netRevenue(), 1e-9);
        assertEquals(1500.0, t.refunded(), 1e-9);
        assertEquals(2000.0, t.averageBookingValue(), 1e-9);
    }

    @Test
    void funnelCountsTripsWithPaymentAttempts() {
        List<AnalyticsEngine.FunnelStep> f = sample().funnel();
        assertEquals(5, f.get(0).count());
        assertEquals(100.0, f.get(0).percentOfFirst(), 1e-9);
        assertEquals(5, f.get(1).count());          // trips 1-5 all have a payment row
        assertEquals(3, f.get(2).count());
        assertEquals(60.0, f.get(2).percentOfFirst(), 1e-9);
    }

    @Test
    void tripsByStatusIncludesEveryStatusEvenWhenZero() {
        var m = sample().tripsByStatus();
        assertEquals(3, (int) m.get("CONFIRMED"));
        assertEquals(1, (int) m.get("CANCELLED"));
        assertEquals(1, (int) m.get("PLANNED"));
        assertEquals(0, (int) m.get("REQUESTED"));
        assertEquals(TripStatus.values().length, m.size());
    }

    @Test
    void topCitiesMergeCaseAndRankBookedTripsOnly() {
        var cities = sample().topCities();
        assertEquals("Goa", cities.get(0).label());   // "Goa" + "goa" merged, first spelling kept
        assertEquals(2, cities.get(0).count());
        assertEquals("Pune", cities.get(1).label());
        assertEquals(1, cities.get(1).count());       // the cancelled Pune trip is not counted
        assertEquals(2, cities.size());               // Mumbai is only planned
    }

    @Test
    void interestMixCountsBookedTrips() {
        var mix = sample().interestMix();
        assertEquals("FOOD", mix.get(0).label());
        assertEquals(2, mix.get(0).count());
        assertEquals("NATURE", mix.get(1).label());
    }

    @Test
    void dailySeriesCoversTheWholeWindowOldestFirst() {
        Snapshot s = sample();
        assertEquals(7, s.daily().size());
        assertEquals(TODAY.minusDays(6).toString(), s.daily().get(0).date());
        assertEquals(TODAY.toString(), s.daily().get(6).date());
        assertEquals(TODAY.minusDays(6).toString(), s.from());
    }

    @Test
    void dailyPointsCountSignupsTripsBookingsAndRevenueOnTheRightDay() {
        Snapshot s = sample();
        DayPoint today = s.daily().get(6);
        assertEquals(2, today.signups());
        assertEquals(2, today.tripsPlanned());        // trips 3 and 5
        assertEquals(1, today.bookings());
        assertEquals(1000.0, today.revenue(), 1e-9);
        DayPoint threeAgo = s.daily().get(3);
        assertEquals(1, threeAgo.signups());
        // The refunded payment 4 days ago is not a booking and brings in no revenue.
        assertEquals(0, s.daily().get(2).bookings());
        assertEquals(0.0, s.daily().get(2).revenue(), 1e-9);
    }

    @Test
    void thingsOutsideTheWindowAreLeftOutOfTheSeriesButStayInTotals() {
        Snapshot s = sample();
        int signupsInWindow = s.daily().stream().mapToInt(DayPoint::signups).sum();
        assertEquals(3, signupsInWindow);             // the signup 100 days ago is outside
        assertEquals(4, s.totals().users());
    }

    @Test
    void emptyDataIsAllZeroesNotErrors() {
        Snapshot s = AnalyticsEngine.compute(List.of(), List.of(), List.of(), TODAY, 30);
        assertEquals(0, s.totals().trips());
        assertEquals(0.0, s.totals().bookingRate(), 1e-9);
        assertEquals(0.0, s.totals().averageBookingValue(), 1e-9);
        assertEquals(0.0, s.funnel().get(2).percentOfFirst(), 1e-9);
        assertEquals(30, s.daily().size());
        assertTrue(s.topCities().isEmpty());
    }

    @Test
    void windowIsClampedAndDefaulted() {
        assertEquals(AnalyticsEngine.MAX_DAYS, AnalyticsEngine.compute(List.of(), List.of(), List.of(), TODAY, 9999).days());
        assertEquals(30, AnalyticsEngine.compute(List.of(), List.of(), List.of(), TODAY, 0).days());
        assertEquals(1, AnalyticsEngine.compute(List.of(), List.of(), List.of(), TODAY, 1).daily().size());
    }
}
