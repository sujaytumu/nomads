package com.tripfactory.nomad.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.MyPlaceRatingResponse;
import com.tripfactory.nomad.api.dto.PlaceRatingsResponse;
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.entity.PlaceRating;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.enums.TripStatus;
import com.tripfactory.nomad.repository.PlaceRatingRepository;
import com.tripfactory.nomad.repository.PlaceRepository;
import com.tripfactory.nomad.repository.TripPlanRepository;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.service.PlaceRatingService;
import com.tripfactory.nomad.service.engine.RatingSummary;
import com.tripfactory.nomad.service.exception.BadRequestException;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceRatingServiceImpl implements PlaceRatingService {

    static final int LATEST_REVIEWS = 10;

    private final PlaceRatingRepository ratingRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final TripPlanRepository tripPlanRepository;

    @Override
    @Transactional(readOnly = true)
    public PlaceRatingsResponse overview(Long placeId) {
        Place place = requirePlace(placeId);
        List<PlaceRating> all = ratingRepository.findByPlaceIdOrderByUpdatedAtDesc(placeId);

        List<Integer> stars = new ArrayList<>(all.size());
        List<PlaceRatingsResponse.Review> reviews = new ArrayList<>();
        for (PlaceRating r : all) {
            stars.add(r.getStars());
            boolean hasText = r.getComment() != null && !r.getComment().isBlank();
            if (hasText && reviews.size() < LATEST_REVIEWS) {
                reviews.add(new PlaceRatingsResponse.Review(r.getStars(), r.getComment(), firstName(r.getUser().getName()), r.getUpdatedAt()));
            }
        }
        RatingSummary.Summary s = RatingSummary.compute(stars, place.getRating() == null ? 0.0 : place.getRating());
        return new PlaceRatingsResponse(placeId, s.count(), s.average(), s.blended(), s.distribution(), reviews);
    }

    @Override
    @Transactional(readOnly = true)
    public MyPlaceRatingResponse mine(String email, Long placeId) {
        requirePlace(placeId);
        User user = requireUser(email);
        PlaceRating existing = ratingRepository.findByUserIdAndPlaceId(user.getId(), placeId).orElse(null);
        return new MyPlaceRatingResponse(canRate(user, placeId),
                existing == null ? null : existing.getStars(), existing == null ? null : existing.getComment());
    }

    @Override
    @Transactional
    public MyPlaceRatingResponse rate(String email, Long placeId, int stars, String comment) {
        if (stars < 1 || stars > 5) {
            throw new BadRequestException("Rating must be between 1 and 5 stars");
        }
        Place place = requirePlace(placeId);
        User user = requireUser(email);
        if (!canRate(user, placeId)) {
            throw new BadRequestException("You can rate a place after booking a trip that includes it");
        }
        PlaceRating r = ratingRepository.findByUserIdAndPlaceId(user.getId(), placeId).orElseGet(() -> {
            PlaceRating fresh = new PlaceRating();
            fresh.setUser(user);
            fresh.setPlace(place);
            return fresh;
        });
        r.setStars(stars);
        String text = comment == null ? null : comment.trim();
        r.setComment(text == null || text.isEmpty() ? null : text);
        PlaceRating saved = ratingRepository.save(r);
        return new MyPlaceRatingResponse(true, saved.getStars(), saved.getComment());
    }

    @Override
    @Transactional
    public void remove(String email, Long placeId) {
        User user = requireUser(email);
        ratingRepository.deleteByUserIdAndPlaceId(user.getId(), placeId);
    }

    private boolean canRate(User user, Long placeId) {
        return tripPlanRepository.countVisits(user.getId(), TripStatus.CONFIRMED, placeId) > 0;
    }

    private Place requirePlace(Long id) {
        return placeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Place not found"));
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) return "Traveller";
        return name.trim().split("\\s+")[0];
    }
}
