package com.tripfactory.nomad.service;

import java.util.List;

import com.tripfactory.nomad.api.dto.PackageSummaryResponse;

public interface WishlistService {

    /** Saved packages for the given user, most recently saved first. */
    List<PackageSummaryResponse> list(String userEmail);

    /** Ids only - lets the package grid render filled hearts with a single call. */
    List<Long> ids(String userEmail);

    /** Idempotent: saving an already-saved package is a no-op. */
    void add(String userEmail, Long packageId);

    /** Idempotent: removing a package that isn't saved is a no-op. */
    void remove(String userEmail, Long packageId);
}
