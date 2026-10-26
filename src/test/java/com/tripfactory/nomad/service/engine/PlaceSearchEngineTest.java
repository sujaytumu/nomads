package com.tripfactory.nomad.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.service.engine.PlaceSearchEngine.Item;
import com.tripfactory.nomad.service.engine.PlaceSearchEngine.Query;
import com.tripfactory.nomad.service.engine.PlaceSearchEngine.Result;
import com.tripfactory.nomad.service.engine.PlaceSearchEngine.Sort;

class PlaceSearchEngineTest {

    private static final List<Item> PLACES = List.of(
        new Item(1L, "Shaniwar Wada", "Pune", 18.5195, 73.8553, InterestType.CULTURE, 4.5, null),
        new Item(2L, "Sinhagad Fort", "Pune", 18.3663, 73.7559, InterestType.ADVENTURE, 4.6, null),
        new Item(3L, "Vaishali Restaurant", "Pune", 18.5167, 73.8412, InterestType.FOOD, 4.3, null),
        new Item(4L, "Baga Beach", "Goa", 15.5553, 73.7517, InterestType.RELAXATION, 4.4, null),
        new Item(5L, "Fisherman's Wharf", "Goa", 15.4989, 73.8278, InterestType.FOOD, 4.1, null),
        new Item(6L, "Marine Drive", "Mumbai", 18.9440, 72.8238, InterestType.NATURE, 4.7, null)
    );

    private static Query q(String text, String city, InterestType cat, Double minRating, Sort sort, Double lat, Double lon, int page, int size) {
        return new Query(text, city, cat, minRating, sort, lat, lon, page, size);
    }

    private static List<Long> ids(Result r) {
        return r.hits().stream().map(h -> h.item().id()).collect(Collectors.toList());
    }

    @Test
    void noFiltersReturnsEverythingBestRatedFirst() {
        Result r = PlaceSearchEngine.search(PLACES, q(null, null, null, null, null, null, null, 0, 20));
        assertEquals(6, r.totalItems());
        assertEquals(List.of(6L, 2L, 1L, 4L, 3L, 5L), ids(r));
    }

    @Test
    void textMatchesNameCityAndRanksNameMatchesFirst() {
        Result byName = PlaceSearchEngine.search(PLACES, q("sinha", null, null, null, null, null, null, 0, 20));
        assertEquals(List.of(2L), ids(byName));
        // "goa" matches by city only (relevance 20), "baga" by name start.
        Result byCity = PlaceSearchEngine.search(PLACES, q("goa", null, null, null, null, null, null, 0, 20));
        assertEquals(2, byCity.totalItems());
        Result startsWith = PlaceSearchEngine.search(PLACES, q("baga", null, null, null, null, null, null, 0, 20));
        assertEquals(List.of(4L), ids(startsWith));
    }

    @Test
    void multiWordQueryNeedsEveryWord() {
        Result r = PlaceSearchEngine.search(PLACES, q("food goa", null, null, null, null, null, null, 0, 20));
        assertEquals(List.of(5L), ids(r));
        Result none = PlaceSearchEngine.search(PLACES, q("food mumbai", null, null, null, null, null, null, 0, 20));
        assertEquals(0, none.totalItems());
    }

    @Test
    void filtersCombine() {
        Result r = PlaceSearchEngine.search(PLACES, q(null, "pune", InterestType.FOOD, 4.0, null, null, null, 0, 20));
        assertEquals(List.of(3L), ids(r));
        Result high = PlaceSearchEngine.search(PLACES, q(null, null, null, 4.5, null, null, null, 0, 20));
        assertEquals(List.of(6L, 2L, 1L), ids(high));
    }

    @Test
    void facetsIgnoreTheirOwnFilter() {
        // City = Pune: the category facet counts Pune places only, but the city facet still shows all cities.
        Result r = PlaceSearchEngine.search(PLACES, q(null, "Pune", null, null, null, null, null, 0, 20));
        assertEquals(3, r.totalItems());
        assertEquals(1L, (long) r.categoryFacets().get("CULTURE"));
        assertNull(r.categoryFacets().get("RELAXATION"));
        assertEquals(3L, (long) r.cityFacets().get("Pune"));
        assertEquals(2L, (long) r.cityFacets().get("Goa"));
        assertEquals(1L, (long) r.cityFacets().get("Mumbai"));

        // Category = FOOD: city facet narrows to cities that have food; category facet still shows all.
        Result food = PlaceSearchEngine.search(PLACES, q(null, null, InterestType.FOOD, null, null, null, null, 0, 20));
        assertEquals(2, food.totalItems());
        assertNull(food.cityFacets().get("Mumbai"));
        assertEquals(2L, (long) food.categoryFacets().get("FOOD"));
        assertEquals(1L, (long) food.categoryFacets().get("NATURE"));
    }

    @Test
    void distanceSortUsesTheGivenPoint() {
        // Standing in Panaji (Goa): Goa places first.
        Result r = PlaceSearchEngine.search(PLACES, q(null, null, null, null, Sort.DISTANCE, 15.4909, 73.8278, 0, 20));
        assertEquals(5L, ids(r).get(0));
        assertNotNull(r.hits().get(0).distanceKm());
        assertTrue(r.hits().get(0).distanceKm() < r.hits().get(5).distanceKm());
    }

    @Test
    void distanceIsOmittedWithoutAPoint() {
        Result r = PlaceSearchEngine.search(PLACES, q(null, null, null, null, Sort.DISTANCE, null, null, 0, 20));
        assertNull(r.hits().get(0).distanceKm());
        assertEquals(6, r.totalItems());
    }

    @Test
    void nameSortIsAlphabeticalAndCaseInsensitive() {
        Result r = PlaceSearchEngine.search(PLACES, q(null, null, null, null, Sort.NAME, null, null, 0, 20));
        assertEquals(List.of(4L, 5L, 6L, 1L, 2L, 3L), ids(r));
    }

    @Test
    void pagingSplitsResultsAndReportsTotals() {
        Result p0 = PlaceSearchEngine.search(PLACES, q(null, null, null, null, Sort.NAME, null, null, 0, 4));
        Result p1 = PlaceSearchEngine.search(PLACES, q(null, null, null, null, Sort.NAME, null, null, 1, 4));
        Result p9 = PlaceSearchEngine.search(PLACES, q(null, null, null, null, Sort.NAME, null, null, 9, 4));
        assertEquals(4, p0.hits().size());
        assertEquals(2, p1.hits().size());
        assertEquals(0, p9.hits().size());
        assertEquals(6, p0.totalItems());
        assertEquals(2, p0.totalPages());
    }

    @Test
    void pageSizeIsCappedAndDefaulted() {
        Result big = PlaceSearchEngine.search(PLACES, q(null, null, null, null, null, null, null, 0, 10_000));
        assertEquals(PlaceSearchEngine.MAX_PAGE_SIZE, big.size());
        Result def = PlaceSearchEngine.search(PLACES, q(null, null, null, null, null, null, null, -3, 0));
        assertEquals(20, def.size());
        assertEquals(0, def.page());
    }

    @Test
    void sortParsingIsForgiving() {
        assertEquals(Sort.RATING, Sort.parse(" Rating "));
        assertEquals(Sort.RELEVANCE, Sort.parse("nonsense"));
        assertEquals(Sort.RELEVANCE, Sort.parse(null));
    }
}
