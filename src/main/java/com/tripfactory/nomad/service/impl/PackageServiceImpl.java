package com.tripfactory.nomad.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.tripfactory.nomad.api.dto.PackageDetailResponse;
import com.tripfactory.nomad.api.dto.PackageSummaryResponse;
import com.tripfactory.nomad.api.dto.PlaceResponse;
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.repository.PlaceRepository;
import com.tripfactory.nomad.service.PackageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PackageServiceImpl implements PackageService {

    private final PlaceRepository placeRepository;
    private final AtomicLong idGenerator = new AtomicLong(100);
    private final List<PackageSummaryResponse> packages = new ArrayList<>();

    @PostConstruct
    public void init() {
        // Build three example packages using seeded places when available
        List<Place> bengaluru = placeRepository.findByCityIgnoreCase("Bengaluru");
        List<Place> mumbai = placeRepository.findByCityIgnoreCase("Mumbai");
        List<Place> delhi = placeRepository.findByCityIgnoreCase("Delhi");

        packages.add(buildSummary("Weekend in Bengaluru", "A curated 2-day Bengaluru weekend with food, parks and nightlife.", new BigDecimal("4999"), bengaluru));
        packages.add(buildSummary("Mumbai Shoreline", "Explore beaches, bazaars and local cuisine in Mumbai.", new BigDecimal("5999"), mumbai));
        packages.add(buildSummary("Delhi Heritage Trip", "Historical walks, markets and cultural experiences in Delhi.", new BigDecimal("5499"), delhi));
    }

    private PackageSummaryResponse buildSummary(String name, String desc, BigDecimal price, List<Place> fromPlaces) {
        PackageSummaryResponse s = new PackageSummaryResponse();
        s.setId(idGenerator.getAndIncrement());
        s.setName(name);
        s.setShortDescription(desc);
        s.setPrice(price);
        // Places do not currently have an image field; leave imageUrl null for now
        s.setImageUrl(null);
        return s;
    }

    @Override
    public List<PackageSummaryResponse> getHomepagePackages() {
        return packages.stream().limit(3).collect(Collectors.toList());
    }

    @Override
    public List<PackageSummaryResponse> getAllPackages() {
        return new ArrayList<>(packages);
    }

    @Override
    public PackageDetailResponse getPackageById(Long id) {
        PackageSummaryResponse summary = packages.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
        if (summary == null) return null;

        PackageDetailResponse detail = new PackageDetailResponse();
        detail.setId(summary.getId());
        detail.setName(summary.getName());
        detail.setDescription(summary.getShortDescription());
        detail.setPrice(summary.getPrice());
        detail.setImageUrl(summary.getImageUrl());

        // For demo purposes, attach up to 5 places from the same city (fallback to any)
        List<Place> places = placeRepository.findByCityIgnoreCase("Bengaluru");
        if (summary.getName().toLowerCase().contains("mumbai")) {
            places = placeRepository.findByCityIgnoreCase("Mumbai");
        } else if (summary.getName().toLowerCase().contains("delhi")) {
            places = placeRepository.findByCityIgnoreCase("Delhi");
        }
        if (places == null || places.isEmpty()) {
            places = placeRepository.findAll();
        }

        List<PlaceResponse> placeResponses = places.stream().limit(5).map(p -> {
            PlaceResponse pr = new PlaceResponse();
            pr.setId(p.getId());
            pr.setName(p.getName());
            pr.setCity(p.getCity());
            pr.setLatitude(p.getLatitude());
            pr.setLongitude(p.getLongitude());
            pr.setCategory(p.getCategory());
            pr.setRating(p.getRating());
            return pr;
        }).collect(Collectors.toList());

        detail.setPlaces(placeResponses);
        detail.setActivities(List.of("Guided tour", "Local food tasting", "Photo stops"));

        double avg = placeResponses.stream().mapToDouble(p -> p.getRating() == null ? 0.0 : p.getRating()).average().orElse(0.0);
        detail.setAverageRating(avg);

        return detail;
    }
}
