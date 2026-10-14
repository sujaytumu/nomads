package com.tripfactory.nomad.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tripfactory.nomad.api.dto.PackageSummaryResponse;

class PackageFilterTest {

    private static PackageSummaryResponse pkg(long id, String name, String desc, String price) {
        PackageSummaryResponse p = new PackageSummaryResponse();
        p.setId(id);
        p.setName(name);
        p.setShortDescription(desc);
        p.setPrice(new BigDecimal(price));
        return p;
    }

    private final List<PackageSummaryResponse> all = List.of(
            pkg(1, "Weekend in Bengaluru", "food, parks and nightlife", "4999"),
            pkg(2, "Mumbai Shoreline", "beaches and bazaars", "5999"),
            pkg(3, "Delhi Heritage Trip", "historical walks and markets", "5499"));

    @Test
    void noFilters_returnsEverythingInOriginalOrder() {
        assertThat(PackageFilter.apply(all, null, null, null, null)).extracting(PackageSummaryResponse::getId)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void query_matchesNameOrDescription_caseInsensitive() {
        assertThat(PackageFilter.apply(all, "  MUMBAI ", null, null, null)).extracting(PackageSummaryResponse::getId)
                .containsExactly(2L);
        assertThat(PackageFilter.apply(all, "markets", null, null, null)).extracting(PackageSummaryResponse::getId)
                .containsExactly(3L);
    }

    @Test
    void priceRange_isInclusive() {
        assertThat(PackageFilter.apply(all, null, new BigDecimal("5499"), new BigDecimal("5999"), null))
                .extracting(PackageSummaryResponse::getId).containsExactly(2L, 3L);
    }

    @Test
    void sortByPrice_ascAndDesc() {
        assertThat(PackageFilter.apply(all, null, null, null, "price_asc")).extracting(PackageSummaryResponse::getId)
                .containsExactly(1L, 3L, 2L);
        assertThat(PackageFilter.apply(all, null, null, null, "price_desc")).extracting(PackageSummaryResponse::getId)
                .containsExactly(2L, 3L, 1L);
    }

    @Test
    void sortByName_alphabetical() {
        assertThat(PackageFilter.apply(all, null, null, null, "name")).extracting(PackageSummaryResponse::getId)
                .containsExactly(3L, 2L, 1L);
    }

    @Test
    void doesNotMutateSource() {
        PackageFilter.apply(all, null, null, null, "price_desc");
        assertThat(all).extracting(PackageSummaryResponse::getId).containsExactly(1L, 2L, 3L);
    }
}
