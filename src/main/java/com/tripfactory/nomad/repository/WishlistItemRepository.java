package com.tripfactory.nomad.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.tripfactory.nomad.domain.entity.WishlistItem;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndPackageId(Long userId, Long packageId);

    @Transactional
    long deleteByUserIdAndPackageId(Long userId, Long packageId);
}
