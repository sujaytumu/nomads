package com.tripfactory.nomad.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.PlaceResponse;
import com.tripfactory.nomad.api.dto.PlaceSearchResponse;
import com.tripfactory.nomad.api.dto.PlaceSuggestion;
import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.repository.PlaceRepository;
import com.tripfactory.nomad.service.PlaceSearchService;
import com.tripfactory.nomad.service.engine.PlaceSearchEngine;
import com.tripfactory.nomad.service.engine.SearchSpell;
import com.tripfactory.nomad.service.util.CityAliasResolver;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * The places table is small (hundreds of rows), so the whole table is loaded and searched in memory. That is
 * what makes exact facet counts and relevance ranking simple; if it ever grows to many thousands of rows, move
 * the filtering into the database and keep the engine for ranking.
 */
@Service
@RequiredArgsConstructor
public class PlaceSearchServiceImpl implements PlaceSearchService {

    private final PlaceRepository placeRepository;

    @Override
    @Transactional(readOnly = true)
    public PlaceSearchResponse search(String text, String city, InterestType category, Double minRating, String sort,
                                      Double lat, Double lon, int page, int size) {
        List<PlaceSearchEngine.Item> all = loadAll();
        PlaceSearchEngine.Result result = run(all, text, city, category, minRating, sort, lat, lon, page, size);
        String corrected = null;
        // Nothing found: try the city's other name (Bangalore) or a fixed typo (lalbag) before giving up.
        if (result.totalItems() == 0 && text != null && !text.isBlank()) {
            String fixed = SearchSpell.correct(text, vocabulary(all));
            if (fixed != null && !fixed.equalsIgnoreCase(text.trim())) {
                PlaceSearchEngine.Result retry = run(all, fixed, city, category, minRating, sort, lat, lon, page, size);
                if (retry.totalItems() > 0) {
                    result = retry;
                    corrected = fixed;
                }
            }
        }

        List<PlaceSearchResponse.Item> items = new ArrayList<>();
        for (PlaceSearchEngine.Hit h : result.hits()) {
            PlaceSearchEngine.Item i = h.item();
            items.add(new PlaceSearchResponse.Item(i.id(), i.name(), i.city(), i.latitude(), i.longitude(),
                    i.category(), i.rating(), i.imageUrl(), h.distanceKm()));
        }
        return new PlaceSearchResponse(items, result.page(), result.size(), result.totalItems(), result.totalPages(),
                new PlaceSearchResponse.Facets(result.categoryFacets(), result.cityFacets()), corrected);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlaceSuggestion> suggest(String text) {
        String t = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (t.length() < 2) {
            return List.of();
        }
        List<PlaceSearchEngine.Item> all = loadAll();
        List<PlaceSuggestion> out = new ArrayList<>();

        // Cities first: matches the name, or a nickname such as "bangalore" or "vizag".
        String nickname = CityAliasResolver.canonicalize(t);
        Set<String> cities = new LinkedHashSet<>();
        for (PlaceSearchEngine.Item i : all) {
            cities.add(i.city());
        }
        for (String c : cities) {
            if (c.toLowerCase(Locale.ROOT).startsWith(t) || c.equalsIgnoreCase(nickname)) {
                long count = all.stream().filter(i -> i.city().equals(c)).count();
                out.add(new PlaceSuggestion("CITY", c, count + " places", null, c));
            }
        }

        String query = t;
        PlaceSearchEngine.Result r = run(all, query, null, null, null, "relevance", null, null, 0, 8);
        if (r.totalItems() == 0) {
            String fixed = SearchSpell.correct(t, vocabulary(all));
            if (fixed != null) {
                r = run(all, fixed, null, null, null, "relevance", null, null, 0, 8);
            }
        }
        for (PlaceSearchEngine.Hit h : r.hits()) {
            PlaceSearchEngine.Item i = h.item();
            out.add(new PlaceSuggestion("PLACE", i.name(), i.city() + " · " + pretty(i.category()), i.id(), i.city()));
        }
        return out.subList(0, Math.min(8, out.size()));
    }

    private List<PlaceSearchEngine.Item> loadAll() {
        List<PlaceSearchEngine.Item> all = new ArrayList<>();
        for (Place p : placeRepository.findAll()) {
            all.add(new PlaceSearchEngine.Item(p.getId(), p.getName(), p.getCity(), p.getLatitude(), p.getLongitude(),
                    p.getCategory(), p.getRating() == null ? 0.0 : p.getRating(), p.getImageUrl()));
        }
        return all;
    }

    private static PlaceSearchEngine.Result run(List<PlaceSearchEngine.Item> all, String text, String city,
            InterestType category, Double minRating, String sort, Double lat, Double lon, int page, int size) {
        return PlaceSearchEngine.search(all, new PlaceSearchEngine.Query(
                text, city, category, minRating, PlaceSearchEngine.Sort.parse(sort), lat, lon, page, size));
    }

    /** Every word that exists in a place name or city, in lower case. */
    private static Set<String> vocabulary(List<PlaceSearchEngine.Item> all) {
        Set<String> words = new HashSet<>();
        for (PlaceSearchEngine.Item i : all) {
            for (String w : (i.name() + " " + i.city()).toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
                if (w.length() >= 3) {
                    words.add(w);
                }
            }
        }
        return words;
    }

    private static String pretty(InterestType t) {
        String n = t.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceResponse getPlace(Long id) {
        Place p = placeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Place not found"));
        PlaceResponse r = new PlaceResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setCity(p.getCity());
        r.setLatitude(p.getLatitude());
        r.setLongitude(p.getLongitude());
        r.setCategory(p.getCategory());
        r.setRating(p.getRating());
        r.setImageUrl(p.getImageUrl());
        return r;
    }
}
