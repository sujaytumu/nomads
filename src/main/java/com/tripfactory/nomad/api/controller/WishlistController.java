package com.tripfactory.nomad.api.controller;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripfactory.nomad.api.dto.PackageSummaryResponse;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.entity.WishlistItem;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.repository.WishlistItemRepository;
import com.tripfactory.nomad.service.PackageService;

import lombok.RequiredArgsConstructor;

/** Per-user saved packages. Always scoped to the authenticated user. */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistItemRepository wishlistRepository;
    private final UserRepository userRepository;
    private final PackageService packageService;

    @GetMapping
    public ResponseEntity<List<PackageSummaryResponse>> list() {
        User user = currentUser();
        if (user == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        Map<Long, PackageSummaryResponse> byId = packageService.getAllPackages().stream()
                .collect(Collectors.toMap(PackageSummaryResponse::getId, Function.identity()));
        List<PackageSummaryResponse> result = wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(w -> byId.get(w.getPackageId()))
                .filter(p -> p != null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{packageId}")
    public ResponseEntity<Void> add(@PathVariable Long packageId) {
        User user = currentUser();
        if (user == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        if (packageService.getPackageById(packageId) == null) return ResponseEntity.notFound().build();
        if (!wishlistRepository.existsByUserIdAndPackageId(user.getId(), packageId)) {
            WishlistItem item = new WishlistItem();
            item.setUser(user);
            item.setPackageId(packageId);
            wishlistRepository.save(item);
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{packageId}")
    public ResponseEntity<Void> remove(@PathVariable Long packageId) {
        User user = currentUser();
        if (user == null) return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        wishlistRepository.deleteByUserIdAndPackageId(user.getId(), packageId);
        return ResponseEntity.noContent().build();
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
}
