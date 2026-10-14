package com.tripfactory.nomad.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.PackageSummaryResponse;
import com.tripfactory.nomad.service.WishlistService;

import lombok.RequiredArgsConstructor;

/** Saved ("hearted") packages. Requires authentication - every route is scoped to the caller. */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<PackageSummaryResponse>> list() {
        return ResponseEntity.ok(wishlistService.list(currentEmail()));
    }

    @GetMapping("/ids")
    public ResponseEntity<List<Long>> ids() {
        return ResponseEntity.ok(wishlistService.ids(currentEmail()));
    }

    @PostMapping("/{packageId}")
    public ResponseEntity<Void> add(@PathVariable Long packageId) {
        wishlistService.add(currentEmail(), packageId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{packageId}")
    public ResponseEntity<Void> remove(@PathVariable Long packageId) {
        wishlistService.remove(currentEmail(), packageId);
        return ResponseEntity.noContent().build();
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
