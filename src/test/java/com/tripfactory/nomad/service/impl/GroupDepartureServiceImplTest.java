package com.tripfactory.nomad.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

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
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.domain.enums.UserRole;
import com.tripfactory.nomad.repository.GroupDepartureRepository;
import com.tripfactory.nomad.repository.TripGroupRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class GroupDepartureServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @Mock
    private GroupDepartureRepository departureRepository;
    @Mock
    private TripGroupRepository tripGroupRepository;
    @Mock
    private TripRequestRepository tripRequestRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupDepartureServiceImpl service;

    private final AtomicLong ids = new AtomicLong(1000);
    private GroupDeparture departure;
    // Whatever the service "has saved" for the departure, so tests can model
    // other people's existing bookings.
    private final List<TripRequest> existing = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TripGroup group = new TripGroup();
        group.setId(50L);
        group.setCity("Pune");
        group.setStatus(GroupStatus.CLOSED);

        departure = new GroupDeparture();
        departure.setId(7L);
        departure.setTitle("Sinhagad sunrise trek");
        departure.setCity("Pune");
        departure.setInterest(InterestType.ADVENTURE);
        departure.setStartDate(LocalDate.now(ZONE).plusDays(10));
        departure.setEndDate(LocalDate.now(ZONE).plusDays(11));
        departure.setPricePerSeat(new BigDecimal("1500"));
        departure.setMinTravellers(3);
        departure.setMaxTravellers(4);
        departure.setStatus(DepartureStatus.OPEN);
        departure.setGroup(group);

        lenient().when(departureRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(departure));
        lenient().when(departureRepository.findById(7L)).thenReturn(Optional.of(departure));
        lenient().when(tripRequestRepository.findByDepartureId(7L)).thenAnswer(inv -> new ArrayList<>(existing));
        lenient().when(tripRequestRepository.save(any(TripRequest.class))).thenAnswer(inv -> {
            TripRequest t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(ids.incrementAndGet());
                t.setCreatedAt(LocalDateTime.now());
                existing.add(t);
            }
            return t;
        });
    }

    private User user(long id, String name) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setCity("Pune");
        u.setInterestType(InterestType.ADVENTURE);
        u.setRole(UserRole.USER);
        lenient().when(userRepository.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    private TripRequest booking(User u, int seats, TripStatus status, LocalDateTime holdUntil) {
        TripRequest t = new TripRequest();
        t.setId(ids.incrementAndGet());
        t.setUser(u);
        t.setDeparture(departure);
        t.setGroup(departure.getGroup());
        t.setRequestedGroupSize(seats);
        t.setEstimatedCost(departure.getPricePerSeat().multiply(BigDecimal.valueOf(seats)));
        t.setStatus(status);
        t.setSeatHoldUntil(holdUntil);
        t.setCreatedAt(LocalDateTime.now());
        existing.add(t);
        return t;
    }

    // ---- joining ---------------------------------------------------------

    @Test
    void join_computesAmountOnServerAndHoldsSeats() {
        user(1, "Asha Rao");

        DepartureBookingResponse b = service.join(7L, 1L, 2);

        assertThat(b.getAmount()).isEqualByComparingTo("3000");
        assertThat(b.getSeats()).isEqualTo(2);
        assertThat(b.getStatus()).isEqualTo(TripStatus.REQUESTED);
        assertThat(b.getSeatHoldUntil()).isAfter(LocalDateTime.now());
        // Not paid yet -> must not be handed the chat group.
        assertThat(b.getGroupId()).isNull();
    }

    @Test
    void join_rejectsMoreSeatsThanLeft() {
        user(1, "Asha Rao");
        User other = user(2, "Ravi Kumar");
        booking(other, 3, TripStatus.CONFIRMED, null);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.join(7L, 1L, 2));
        assertThat(ex.getMessage()).contains("Only 1 seat left");
    }

    @Test
    void join_rejectsWhenFull() {
        user(1, "Asha Rao");
        booking(user(2, "Ravi Kumar"), 4, TripStatus.CONFIRMED, null);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.join(7L, 1L, 1));
        assertThat(ex.getMessage()).contains("full");
    }

    @Test
    void join_unpaidSeatsWithinHoldBlockOthers_butExpiredHoldsDoNot() {
        user(1, "Asha Rao");
        User holder = user(2, "Ravi Kumar");
        TripRequest held = booking(holder, 4, TripStatus.PAYMENT_PENDING, LocalDateTime.now().plusMinutes(10));

        assertThrows(BadRequestException.class, () -> service.join(7L, 1L, 1));

        // Ravi never paid and his hold ran out -> the seats are free again.
        held.setSeatHoldUntil(LocalDateTime.now().minusMinutes(1));
        DepartureBookingResponse b = service.join(7L, 1L, 4);
        assertThat(b.getSeats()).isEqualTo(4);
    }

    @Test
    void join_cancelledBookingsFreeTheirSeats() {
        user(1, "Asha Rao");
        booking(user(2, "Ravi Kumar"), 4, TripStatus.CANCELLED, LocalDateTime.now().plusMinutes(10));

        assertThat(service.join(7L, 1L, 4).getSeats()).isEqualTo(4);
    }

    @Test
    void join_rejectsSomeoneWhoAlreadyPaid() {
        User asha = user(1, "Asha Rao");
        booking(asha, 1, TripStatus.CONFIRMED, null);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.join(7L, 1L, 1));
        assertThat(ex.getMessage()).contains("already booked");
    }

    @Test
    void join_retryWithSameSeatsReusesTheExistingBookingInsteadOfDuplicating() {
        User asha = user(1, "Asha Rao");
        TripRequest first = booking(asha, 2, TripStatus.PAYMENT_PENDING, LocalDateTime.now().plusMinutes(5));

        DepartureBookingResponse b = service.join(7L, 1L, 2);

        assertThat(b.getTripRequestId()).isEqualTo(first.getId());
        assertThat(existing).hasSize(1);
        assertThat(first.getSeatHoldUntil()).isAfter(LocalDateTime.now().plusMinutes(20));
    }

    @Test
    void join_changingSeatCountCancelsTheOldAttemptAndCreatesANewOne() {
        User asha = user(1, "Asha Rao");
        TripRequest first = booking(asha, 2, TripStatus.PAYMENT_PENDING, LocalDateTime.now().plusMinutes(5));

        DepartureBookingResponse b = service.join(7L, 1L, 3);

        assertThat(first.getStatus()).isEqualTo(TripStatus.CANCELLED);
        assertThat(b.getTripRequestId()).isNotEqualTo(first.getId());
        assertThat(b.getSeats()).isEqualTo(3);
        // Her own old hold must not count against her new request.
        assertThat(b.getAmount()).isEqualByComparingTo("4500");
    }

    @Test
    void join_rejectsClosedOrStartedTrips() {
        user(1, "Asha Rao");

        departure.setStatus(DepartureStatus.CANCELLED);
        assertThrows(BadRequestException.class, () -> service.join(7L, 1L, 1));

        departure.setStatus(DepartureStatus.OPEN);
        departure.setStartDate(LocalDate.now(ZONE));
        assertThrows(BadRequestException.class, () -> service.join(7L, 1L, 1));
    }

    @Test
    void join_rejectsBadSeatCounts() {
        assertThrows(BadRequestException.class, () -> service.join(7L, 1L, 0));
        assertThrows(BadRequestException.class, () -> service.join(7L, 1L, 5));
        verify(tripRequestRepository, never()).save(any());
    }

    // ---- paying ----------------------------------------------------------

    @Test
    void assertCanPay_failsWhenHoldExpiredAndTripFilledUpMeanwhile() {
        User asha = user(1, "Asha Rao");
        TripRequest mine = booking(asha, 2, TripStatus.PAYMENT_PENDING, LocalDateTime.now().minusMinutes(5));
        booking(user(2, "Ravi Kumar"), 3, TripStatus.CONFIRMED, null);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.assertCanPay(mine));
        assertThat(ex.getMessage()).contains("expired");
    }

    @Test
    void assertCanPay_succeedsAndRefreshesHoldWhenSeatsStillFree() {
        User asha = user(1, "Asha Rao");
        TripRequest mine = booking(asha, 2, TripStatus.PAYMENT_PENDING, LocalDateTime.now().minusMinutes(5));
        booking(user(2, "Ravi Kumar"), 1, TripStatus.CONFIRMED, null);

        service.assertCanPay(mine);

        assertThat(mine.getSeatHoldUntil()).isAfter(LocalDateTime.now().plusMinutes(20));
    }

    @Test
    void assertCanPay_ignoresOrdinaryTrips() {
        TripRequest plain = new TripRequest();
        plain.setStatus(TripStatus.REQUESTED);

        service.assertCanPay(plain);

        verify(departureRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void assertCanPay_rejectsCancelledAndAlreadyPaidBookings() {
        User asha = user(1, "Asha Rao");
        TripRequest cancelled = booking(asha, 1, TripStatus.CANCELLED, null);
        TripRequest paid = booking(asha, 1, TripStatus.CONFIRMED, null);

        assertThrows(BadRequestException.class, () -> service.assertCanPay(cancelled));
        assertThrows(BadRequestException.class, () -> service.assertCanPay(paid));
    }

    // ---- listing / counts ------------------------------------------------

    @Test
    void get_reportsSeatsLeftAndGuaranteeFromPaidSeats() {
        booking(user(2, "Ravi Kumar"), 2, TripStatus.CONFIRMED, null);
        booking(user(3, "Meera Nair"), 1, TripStatus.PAYMENT_PENDING, LocalDateTime.now().plusMinutes(10));

        DepartureResponse r = service.get(7L);

        assertThat(r.getConfirmedSeats()).isEqualTo(2);
        assertThat(r.getSeatsLeft()).isEqualTo(1); // 4 max - 2 paid - 1 held
        assertThat(r.getGuaranteedToRun()).isFalse(); // needs 3 paid
        assertThat(r.getBookingOpen()).isTrue();
        assertThat(r.getDurationDays()).isEqualTo(2);

        booking(user(4, "Dev Patel"), 1, TripStatus.CONFIRMED, null);
        assertThat(service.get(7L).getGuaranteedToRun()).isTrue();
    }

    // ---- travellers privacy ---------------------------------------------

    @Test
    void travellers_visibleToPaidTravellers_firstNamesOnly() {
        User asha = user(1, "Asha Rao");
        booking(asha, 1, TripStatus.CONFIRMED, null);
        booking(user(2, "Ravi Kumar"), 2, TripStatus.CONFIRMED, null);
        booking(user(3, "Meera Nair"), 1, TripStatus.PAYMENT_PENDING, LocalDateTime.now().plusMinutes(10));
        when(departureRepository.existsById(7L)).thenReturn(true);

        List<DepartureTravellerResponse> list = service.travellers(7L, 1L);

        assertThat(list).extracting(DepartureTravellerResponse::getFirstName).containsExactly("Asha", "Ravi");
    }

    @Test
    void travellers_deniedToSomeoneWhoHasNotPaid() {
        user(1, "Asha Rao");
        booking(user(2, "Ravi Kumar"), 1, TripStatus.CONFIRMED, null);
        booking(user(3, "Meera Nair"), 1, TripStatus.PAYMENT_PENDING, LocalDateTime.now().plusMinutes(10));
        when(departureRepository.existsById(7L)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> service.travellers(7L, 1L));
        assertThrows(AccessDeniedException.class, () -> service.travellers(7L, 3L));
    }

    @Test
    void travellers_adminCanAlwaysSee() {
        User admin = user(9, "Admin Person");
        admin.setRole(UserRole.ADMIN);
        booking(user(2, "Ravi Kumar"), 1, TripStatus.CONFIRMED, null);
        when(departureRepository.existsById(7L)).thenReturn(true);

        assertThat(service.travellers(7L, 9L)).hasSize(1);
    }

    // ---- creating --------------------------------------------------------

    private DepartureCreateRequest validCreate() {
        DepartureCreateRequest r = new DepartureCreateRequest();
        r.setTitle("Goa weekend");
        r.setCity("Bombay");
        r.setInterest(InterestType.RELAXATION);
        r.setStartDate(LocalDate.now(ZONE).plusDays(20));
        r.setEndDate(LocalDate.now(ZONE).plusDays(22));
        r.setPricePerSeat(new BigDecimal("4000"));
        r.setMinTravellers(4);
        r.setMaxTravellers(8);
        return r;
    }

    @Test
    void create_makesAClosedBackingGroupSoPlannerAutoMatchingNeverPicksIt() {
        when(tripGroupRepository.save(any(TripGroup.class))).thenAnswer(inv -> {
            TripGroup g = inv.getArgument(0);
            g.setId(77L);
            return g;
        });
        when(departureRepository.save(any(GroupDeparture.class))).thenAnswer(inv -> {
            GroupDeparture d = inv.getArgument(0);
            d.setId(8L);
            return d;
        });

        DepartureResponse r = service.create(validCreate());

        assertThat(r.getCity()).isEqualTo("Mumbai"); // alias resolved
        assertThat(r.getSeatsLeft()).isEqualTo(8);
        org.mockito.ArgumentCaptor<TripGroup> captor = org.mockito.ArgumentCaptor.forClass(TripGroup.class);
        verify(tripGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(GroupStatus.CLOSED);
    }

    @Test
    void create_validatesDatesAndCapacity() {
        DepartureCreateRequest past = validCreate();
        past.setStartDate(LocalDate.now(ZONE));
        assertThrows(BadRequestException.class, () -> service.create(past));

        DepartureCreateRequest backwards = validCreate();
        backwards.setEndDate(backwards.getStartDate().minusDays(1));
        assertThrows(BadRequestException.class, () -> service.create(backwards));

        DepartureCreateRequest tooLong = validCreate();
        tooLong.setEndDate(tooLong.getStartDate().plusDays(30));
        assertThrows(BadRequestException.class, () -> service.create(tooLong));

        DepartureCreateRequest minAboveMax = validCreate();
        minAboveMax.setMinTravellers(9);
        assertThrows(BadRequestException.class, () -> service.create(minAboveMax));

        verify(departureRepository, never()).save(any());
    }
}
