package com.tripfactory.nomad.service;

import java.util.List;

import com.tripfactory.nomad.api.dto.PackageDetailResponse;
import com.tripfactory.nomad.api.dto.PackageSummaryResponse;

public interface PackageService {
    List<PackageSummaryResponse> getHomepagePackages();
    List<PackageSummaryResponse> getAllPackages();
    PackageDetailResponse getPackageById(Long id);
}
