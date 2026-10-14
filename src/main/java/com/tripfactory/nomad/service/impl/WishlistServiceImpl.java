package com.tripfactory.nomad.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.api.dto.PackageSummaryResponse;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.entity.WishlistItem;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.repository.WishlistItemRepository;
import com.tripfactory.nomad.service.PackageService;
import com.tripfactory.nomad.service.WishlistService;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final PackageService packageService;

    @Override
    @Transactional(readOnly = true)
    public List<PackageSummaryResponse> list(String userEmail) {
        User user = requireUser(userEmail);
        Map<Long, PackageSummaryResponse> byId = packageService.getAllPackages().stream()
                .collect(Collectors.toMap(PackageSummaryResponse::getId, Function.identity()));
        return wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(item -> byId.get(item.getPackageId()))
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> ids(String userEmail) {
        User user = requireUser(userEmail);
        return wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(WishlistItem::getPackageId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void add(String userEmail, Long packageId) {
        User user = requireUser(userEmail);
        if (packageService.getPackageById(packageId) == null) {
            throw new ResourceNotFoundException("Package not found");
        }
        if (wishlistItemRepository.existsByUserIdAndPackageId(user.getId(), packageId)) {
            return;
        }
        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setPackageId(packageId);
        wishlistItemRepository.save(item);
    }

    @Override
    @Transactional
    public void remove(String userEmail, Long packageId) {
        User user = requireUser(userEmail);
        wishlistItemRepository.deleteByUserIdAndPackageId(user.getId(), packageId);
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
