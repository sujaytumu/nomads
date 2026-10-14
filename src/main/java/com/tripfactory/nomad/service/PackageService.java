package com.tripfactory.nomad.service;

import java.math.BigDecimal;
import java.util.List;

import com.tripfactory.nomad.api.dto.PackageDetailResponse;
import com.tripfactory.nomad.api.dto.PackageSummaryResponse;

public interface PackageService {
    List<PackageSummaryResponse> getHomepagePackages();
    List<PackageSummaryResponse> getAllPackages();
    List<PackageSummaryResponse> searchPackages(String query, BigDecimal minPrice, BigDecimal maxPrice, String sort);
    PackageDetailResponse getPackageById(Long id);
}
