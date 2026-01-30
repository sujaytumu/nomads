package com.tripfactory.nomad.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tripfactory.nomad.api.dto.ReviewCreateRequest;
import com.tripfactory.nomad.api.dto.ReviewResponse;
import com.tripfactory.nomad.domain.entity.Review;
import com.tripfactory.nomad.domain.entity.TripRequest;
import com.tripfactory.nomad.repository.ReviewRepository;
import com.tripfactory.nomad.repository.TripRequestRepository;
import com.tripfactory.nomad.service.ReviewService;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final TripRequestRepository tripRequestRepository;

    @Override
    public ReviewResponse createReview(ReviewCreateRequest request) {
        TripRequest tripRequest = tripRequestRepository.findById(request.getTripRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        Review review = new Review();
        review.setTripRequest(tripRequest);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    @Override
    public List<ReviewResponse> getReviewsByTrip(Long tripRequestId) {
        return reviewRepository.findByTripRequestId(tripRequestId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponse toResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setTripRequestId(review.getTripRequest().getId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }
}