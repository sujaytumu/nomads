package com.tripfactory.nomad.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.DepartureBookingResponse;
import com.tripfactory.nomad.api.dto.DepartureCreateRequest;
import com.tripfactory.nomad.api.dto.DepartureResponse;
import com.tripfactory.nomad.api.dto.DepartureTravellerResponse;
import com.tripfactory.nomad.domain.entity.GroupDeparture;
import com.tripfactory.nomad.domain.entity.TripGroup;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.DepartureStatus;
import com.tripfactory.nomad.domain.enums.GroupStatus;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.TravelMode;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.domain.enums.UserRole;
import com.tripfactory.nomad.domain.enums.WeekendType;
import com.tripfactory.nomad.repository.GroupDepartureRepository;
import com.tripfactory.nomad.repository.TripGroupRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.GroupDepartureService;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;
import com.tripfactory.nomad.service.util.CityAliasResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupDepartureServiceImpl implements GroupDepartureService {

    /** How long unpaid seats stay reserved for someone who is paying. */
    static final int SEAT_HOLD_MINUTES = 30;
    static final int MAX_SEATS_PER_BOOKING = 4;
    static final int MAX_TRIP_DAYS = 14;

    // Trips are sold in India (INR), so "is the trip still in the future" is
    // judged on the Indian calendar rather than whatever zone the server runs in.
    private static final ZoneId TRIP_ZONE = ZoneId.of("Asia/Kolkata");

    private final GroupDepartureRepository departureRepository;
    private final TripGroupRepository tripGroupRepository;
    private final TripRequestRepository tripRequestRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DepartureResponse> listOpen(String city, InterestType interest) {
        String wantedCity = (city == null || city.isBlank()) ? null : CityAliasResolver.canonicalize(city);
        List<GroupDeparture> departures = departureRepository
                .findByStatusAndStartDateAfterOrderByStartDateAsc(DepartureStatus.OPEN, today())
                .stream()
                .filter(d -> wantedCity == null || d.getCity().equalsIgnoreCase(wantedCity))
                .filter(d -> interest == null || d.getInterest() == interest)
                .collect(Collectors.toList());
        if (departures.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = departures.stream().map(GroupDeparture::getId).collect(Collectors.toList());
        Map<Long, List<TripRequest>> tripsByDeparture = tripRequestRepository.findByDepartureIdIn(ids).stream()
                .collect(Collectors.groupingBy(t -> t.getDeparture().getId()));
        LocalDateTime now = LocalDateTime.now();
        return departures.stream()
                .map(d -> toResponse(d, tripsByDeparture.getOrDefault(d.getId(), List.of()), now))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartureResponse get(Long departureId) {
        GroupDeparture departure = departureRepository.findById(departureId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        return toResponse(departure, tripRequestRepository.findByDepartureId(departureId), LocalDateTime.now());
    }

    @Override
    @Transactional
    public DepartureResponse create(DepartureCreateRequest request) {
        if (!request.getStartDate().isAfter(today())) {
            throw new BadRequestException("Start date must be in the future");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before the start date");
        }
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (days > MAX_TRIP_DAYS) {
            throw new BadRequestException("A trip can be at most " + MAX_TRIP_DAYS + " days long");
        }
        if (request.getMaxTravellers() < request.getMinTravellers()) {
            throw new BadRequestException("Maximum travellers cannot be lower than the minimum");
        }

        String city = CityAliasResolver.canonicalize(request.getCity());

        TripGroup group = new TripGroup();
        group.setCity(city);
        group.setInterest(request.getInterest());
        group.setWeekendType(request.getStartDate().equals(request.getEndDate())
                ? WeekendType.ONE_DAY
                : WeekendType.TWO_DAY);
        // CLOSED keeps this group out of the Trip Planner's automatic stranger
        // matching, which only ever picks OPEN groups. Seats on a departure are
        // sold through the departure itself, never by auto-assignment.
        group.setStatus(GroupStatus.CLOSED);
        group = tripGroupRepository.save(group);

        GroupDeparture departure = new GroupDeparture();
        departure.setTitle(request.getTitle().trim());
        departure.setCity(city);
        departure.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        departure.setInterest(request.getInterest());
        departure.setStartDate(request.getStartDate());
        departure.setEndDate(request.getEndDate());
        departure.setPricePerSeat(request.getPricePerSeat());
        departure.setMinTravellers(request.getMinTravellers());
        departure.setMaxTravellers(request.getMaxTravellers());
        departure.setMeetingPoint(request.getMeetingPoint() == null ? null : request.getMeetingPoint().trim());
        departure.setStatus(DepartureStatus.OPEN);
        departure.setGroup(group);
        departure = departureRepository.save(departure);

        return toResponse(departure, List.of(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public DepartureBookingResponse join(Long departureId, Long userId, int seats) {
        if (seats < 1 || seats > MAX_SEATS_PER_BOOKING) {
            throw new BadRequestException("You can book between 1 and " + MAX_SEATS_PER_BOOKING + " seats at a time");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Lock the departure row: concurrent bookings of the last seats queue up here.
        GroupDeparture departure = departureRepository.findByIdForUpdate(departureId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        assertBookable(departure);

        LocalDateTime now = LocalDateTime.now();
        List<TripRequest> existing = tripRequestRepository.findByDepartureId(departure.getId());

        for (TripRequest earlier : existing) {
            if (!earlier.getUser().getId().equals(userId)) {
                continue;
            }
            if (earlier.getStatus() == TripStatus.CONFIRMED) {
                throw new BadRequestException("You have already booked this trip");
            }
            if (earlier.getStatus() == TripStatus.CANCELLED) {
                continue;
            }
            // An unpaid earlier attempt by the same person. Same seats and still
            // held: just reuse it (a double-click or a retry after a payment
            // hiccup must not create duplicate bookings). Otherwise it is
            // superseded by this new attempt.
            if (countsTowardsCapacity(earlier, now) && seatsOf(earlier) == seats) {
                earlier.setSeatHoldUntil(now.plusMinutes(SEAT_HOLD_MINUTES));
                tripRequestRepository.save(earlier);
                return toBooking(earlier);
            }
            earlier.setStatus(TripStatus.CANCELLED);
            tripRequestRepository.save(earlier);
        }

        int seatsLeft = departure.getMaxTravellers() - seatsTaken(existing, null, now);
        if (seatsLeft <= 0) {
            throw new BadRequestException("This trip is full");
        }
        if (seats > seatsLeft) {
            throw new BadRequestException("Only " + seatsLeft + " seat" + (seatsLeft == 1 ? "" : "s") + " left on this trip");
        }

        TripRequest trip = new TripRequest();
        trip.setUser(user);
        trip.setGroup(departure.getGroup());
        trip.setDeparture(departure);
        trip.setCity(departure.getCity());
        trip.setWeekendType(departure.getGroup().getWeekendType());
        trip.setInterest(departure.getInterest());
        trip.setTravelMode(TravelMode.GROUP);
        trip.setPickupRequired(Boolean.FALSE);
        trip.setRequestedGroupSize(seats);
        // Server-computed - the payment amount is always derived from this, never from the client.
        trip.setEstimatedCost(departure.getPricePerSeat().multiply(BigDecimal.valueOf(seats)));
        trip.setStatus(TripStatus.REQUESTED);
        trip.setSeatHoldUntil(now.plusMinutes(SEAT_HOLD_MINUTES));
        trip = tripRequestRepository.save(trip);

        return toBooking(trip);
    }

    @Override
    @Transactional
    public void assertCanPay(TripRequest trip) {
        if (trip.getDeparture() == null) {
            return;
        }
        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new BadRequestException("This booking has been cancelled");
        }
        if (trip.getStatus() == TripStatus.CONFIRMED) {
            throw new BadRequestException("This booking is already paid");
        }

        GroupDeparture departure = departureRepository.findByIdForUpdate(trip.getDeparture().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        assertBookable(departure);

        LocalDateTime now = LocalDateTime.now();
        List<TripRequest> all = tripRequestRepository.findByDepartureId(departure.getId());
        int takenByOthers = seatsTaken(all, trip.getId(), now);
        if (takenByOthers + seatsOf(trip) > departure.getMaxTravellers()) {
            // The hold on these seats ran out and the trip has filled up since.
            throw new BadRequestException("Your seat reservation expired and the trip is now full");
        }
        // Keep the seats reserved while the person completes the payment.
        trip.setSeatHoldUntil(now.plusMinutes(SEAT_HOLD_MINUTES));
        tripRequestRepository.save(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DepartureBookingResponse> myBooking(Long departureId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return tripRequestRepository.findByDepartureIdAndUserId(departureId, userId).stream()
                .filter(t -> t.getStatus() == TripStatus.CONFIRMED || countsTowardsCapacity(t, now))
                .max(Comparator.comparing(TripRequest::getCreatedAt))
                .map(this::toBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartureTravellerResponse> travellers(Long departureId, Long viewerUserId) {
        if (!departureRepository.existsById(departureId)) {
            throw new ResourceNotFoundException("Trip not found");
        }
        User viewer = userRepository.findById(viewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<TripRequest> confirmed = tripRequestRepository.findByDepartureId(departureId).stream()
                .filter(t -> t.getStatus() == TripStatus.CONFIRMED)
                .sorted(Comparator.comparing(TripRequest::getCreatedAt))
                .collect(Collectors.toList());

        boolean allowed = viewer.getRole() == UserRole.ADMIN
                || confirmed.stream().anyMatch(t -> t.getUser().getId().equals(viewerUserId));
        if (!allowed) {
            throw new AccessDeniedException("Only travellers on this trip can see who else is going");
        }

        return confirmed.stream().map(t -> {
            DepartureTravellerResponse r = new DepartureTravellerResponse();
            r.setFirstName(firstName(t.getUser().getName()));
            r.setCity(t.getUser().getCity());
            r.setInterestType(t.getUser().getInterestType());
            r.setSeats(seatsOf(t));
            r.setJoinedAt(t.getCreatedAt());
            return r;
        }).collect(Collectors.toList());
    }

    // ---- helpers ---------------------------------------------------------

    private LocalDate today() {
        return LocalDate.now(TRIP_ZONE);
    }

    private void assertBookable(GroupDeparture departure) {
        if (departure.getStatus() != DepartureStatus.OPEN) {
            throw new BadRequestException("This trip is no longer open for booking");
        }
        if (!departure.getStartDate().isAfter(today())) {
            throw new BadRequestException("Booking for this trip has closed");
        }
    }

    private int seatsOf(TripRequest trip) {
        Integer seats = trip.getRequestedGroupSize();
        return (seats == null || seats < 1) ? 1 : seats;
    }

    /** Paid bookings always count; unpaid ones only while their seat hold is running. */
    private boolean countsTowardsCapacity(TripRequest trip, LocalDateTime now) {
        if (trip.getStatus() == TripStatus.CONFIRMED) {
            return true;
        }
        boolean waitingForPayment = trip.getStatus() == TripStatus.REQUESTED
                || trip.getStatus() == TripStatus.PLANNED
                || trip.getStatus() == TripStatus.PAYMENT_PENDING;
        return waitingForPayment && trip.getSeatHoldUntil() != null && trip.getSeatHoldUntil().isAfter(now);
    }

    private int seatsTaken(Collection<TripRequest> trips, Long excludeTripId, LocalDateTime now) {
        return trips.stream()
                .filter(t -> excludeTripId == null || !excludeTripId.equals(t.getId()))
                .filter(t -> countsTowardsCapacity(t, now))
                .mapToInt(this::seatsOf)
                .sum();
    }

    private String firstName(String fullName) {
        String name = fullName == null ? "" : fullName.trim();
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }

    private DepartureResponse toResponse(GroupDeparture d, List<TripRequest> trips, LocalDateTime now) {
        int confirmedSeats = trips.stream()
                .filter(t -> t.getStatus() == TripStatus.CONFIRMED)
                .mapToInt(this::seatsOf)
                .sum();
        int seatsLeft = Math.max(0, d.getMaxTravellers() - seatsTaken(trips, null, now));

        DepartureResponse r = new DepartureResponse();
        r.setId(d.getId());
        r.setTitle(d.getTitle());
        r.setCity(d.getCity());
        r.setDescription(d.getDescription());
        r.setInterest(d.getInterest());
        r.setStartDate(d.getStartDate());
        r.setEndDate(d.getEndDate());
        r.setDurationDays((int) ChronoUnit.DAYS.between(d.getStartDate(), d.getEndDate()) + 1);
        r.setPricePerSeat(d.getPricePerSeat());
        r.setMinTravellers(d.getMinTravellers());
        r.setMaxTravellers(d.getMaxTravellers());
        r.setMeetingPoint(d.getMeetingPoint());
        r.setStatus(d.getStatus());
        r.setConfirmedSeats(confirmedSeats);
        r.setSeatsLeft(seatsLeft);
        r.setGuaranteedToRun(confirmedSeats >= d.getMinTravellers());
        r.setBookingOpen(d.getStatus() == DepartureStatus.OPEN && d.getStartDate().isAfter(today()) && seatsLeft > 0);
        return r;
    }

    private DepartureBookingResponse toBooking(TripRequest trip) {
        DepartureBookingResponse r = new DepartureBookingResponse();
        r.setTripRequestId(trip.getId());
        r.setDepartureId(trip.getDeparture().getId());
        r.setStatus(trip.getStatus());
        r.setSeats(seatsOf(trip));
        r.setAmount(trip.getEstimatedCost());
        if (trip.getStatus() == TripStatus.CONFIRMED) {
            if (trip.getGroup() != null) {
                r.setGroupId(trip.getGroup().getId());
            }
        } else {
            r.setSeatHoldUntil(trip.getSeatHoldUntil());
        }
        return r;
    }
}
