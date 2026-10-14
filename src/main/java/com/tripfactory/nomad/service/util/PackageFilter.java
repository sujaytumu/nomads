package com.tripfactory.nomad.service.util;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.tripfactory.nomad.api.dto.PackageSummaryResponse;

/**
 * Pure, stateless search / price-filter / sort logic for travel packages.
 * Kept free of Spring so it can be unit-tested without a context.
 */
public final class PackageFilter {

    private PackageFilter() {
    }

    public static List<PackageSummaryResponse> apply(List<PackageSummaryResponse> source, String query,
            BigDecimal minPrice, BigDecimal maxPrice, String sort) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        List<PackageSummaryResponse> result = source.stream()
                .filter(p -> q.isEmpty() || matches(p, q))
                .filter(p -> minPrice == null || (p.getPrice() != null && p.getPrice().compareTo(minPrice) >= 0))
                .filter(p -> maxPrice == null || (p.getPrice() != null && p.getPrice().compareTo(maxPrice) <= 0))
                .collect(Collectors.toList());

        Comparator<PackageSummaryResponse> byPrice = Comparator.comparing(PackageSummaryResponse::getPrice,
                Comparator.nullsLast(Comparator.naturalOrder()));
        String s = sort == null ? "" : sort.trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "price_asc" -> result.sort(byPrice);
            case "price_desc" -> result.sort(byPrice.reversed());
            case "name" -> result.sort(Comparator.comparing(p -> p.getName() == null ? "" : p.getName().toLowerCase(Locale.ROOT)));
            default -> {
                // keep catalogue order
            }
        }
        return result;
    }

    private static boolean matches(PackageSummaryResponse p, String q) {
        return contains(p.getName(), q) || contains(p.getShortDescription(), q);
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }
}
