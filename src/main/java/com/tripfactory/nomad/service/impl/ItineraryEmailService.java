package com.tripfactory.nomad.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.tripfactory.nomad.domain.entity.Payment;
import com.tripfactory.nomad.domain.entity.TripPlan;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.enums.PaymentStatus;
import com.tripfactory.nomad.repository.PaymentRepository;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.service.NotificationService;

import lombok.RequiredArgsConstructor;

/**
 * Builds the itinerary PDF for a trip and sends it. This is the ONLY email NOMAD
 * sends for a trip: the "booking complete" email with the itinerary attached.
 */
@Service
@RequiredArgsConstructor
public class ItineraryEmailService {

    private static final Logger log = LoggerFactory.getLogger(ItineraryEmailService.class);

    private final TripPlanRepository tripPlanRepository;
    private final PaymentRepository paymentRepository;
    private final ItineraryPdfGenerator pdfGenerator;
    private final NotificationService notificationService;

    public byte[] buildPdf(TripRequest trip, BigDecimal cost) {
        List<TripPlan> plans = tripPlanRepository.findByTripRequestIdOrderByDayNumberAscStartTimeAsc(trip.getId());
        Payment payment = paymentRepository
                .findFirstByTripRequestIdAndPaymentStatusOrderByIdDesc(trip.getId(), PaymentStatus.CAPTURED)
                .orElse(null);
        return pdfGenerator.generate(trip, plans, cost, payment);
    }

    /**
     * @return true only if the email was actually handed to the mail provider.
     *         Never throws: a failed email must not undo a successful payment.
     */
    public boolean sendBookingEmail(TripRequest trip) {
        try {
            String to = trip.getUser().getEmail();
            String name = trip.getUser().getName() == null || trip.getUser().getName().isBlank()
                    ? "traveller" : trip.getUser().getName();
            String city = trip.getCity() == null ? "your trip" : trip.getCity();
            byte[] pdf = buildPdf(trip, trip.getEstimatedCost());
            return notificationService.sendEmailWithAttachment(to,
                    "Your NOMAD trip to " + city + " is booked",
                    textBody(name, city, trip.getId()),
                    htmlBody(name, city, trip.getId()),
                    "nomad-itinerary-" + trip.getId() + ".pdf", pdf);
        } catch (Exception ex) {
            log.warn("Itinerary email failed for trip {}: {}", trip.getId(), ex.getMessage());
            return false;
        }
    }

    static String textBody(String name, String city, Long id) {
        return "Hi " + name + ",\n\nYour trip to " + city + " is booked (booking #" + id + ").\n"
                + "Your itinerary is attached as a PDF - keep it handy on your phone.\n\n"
                + "Have a great trip!\nNOMAD";
    }

    static String htmlBody(String name, String city, Long id) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;background:#f5f7ff;padding:24px\">"
                + "<div style=\"max-width:520px;margin:0 auto;background:#fff;border-radius:14px;overflow:hidden;"
                + "border:1px solid #e4e7ec\">"
                + "<div style=\"background:#1b2559;color:#fff;padding:28px\">"
                + "<div style=\"letter-spacing:4px;font-weight:bold;font-size:14px\">NOMAD</div>"
                + "<div style=\"font-size:26px;font-weight:bold;margin-top:14px\">Booking confirmed</div>"
                + "<div style=\"color:#c7d2fe;margin-top:4px\">" + escape(city) + " &middot; booking #" + id
                + "</div></div>"
                + "<div style=\"padding:28px;color:#151b3a;font-size:15px;line-height:1.6\">"
                + "<p>Hi " + escape(name) + ",</p>"
                + "<p>Your trip to <b>" + escape(city) + "</b> is booked. Your full itinerary is attached "
                + "as a PDF &mdash; keep it handy on your phone.</p>"
                + "<p style=\"color:#667085;font-size:13px\">You can see this booking any time under "
                + "<b>My Trips</b> in NOMAD.</p>"
                + "<p>Have a great trip!<br>NOMAD</p></div></div></div>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
