package com.tripfactory.nomad.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tripfactory.nomad.api.dto.PackageDetailResponse;
import com.tripfactory.nomad.api.dto.PackageSummaryResponse;
import com.tripfactory.nomad.domain.entity.User;
import com.tripfactory.nomad.domain.entity.WishlistItem;
import com.tripfactory.nomad.repository.UserRepository;
import com.tripfactory.nomad.repository.WishlistItemRepository;
import com.tripfactory.nomad.service.PackageService;
import com.tripfactory.nomad.service.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PackageService packageService;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7L);
        user.setEmail("a@b.com");
    }

    private static PackageSummaryResponse pkg(long id, String name) {
        PackageSummaryResponse p = new PackageSummaryResponse();
        p.setId(id);
        p.setName(name);
        p.setPrice(new BigDecimal("1000"));
        return p;
    }

    private static WishlistItem item(long packageId) {
        WishlistItem i = new WishlistItem();
        i.setPackageId(packageId);
        return i;
    }

    @Test
    void add_savesNewItem() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(packageService.getPackageById(100L)).thenReturn(new PackageDetailResponse());
        when(wishlistItemRepository.existsByUserIdAndPackageId(7L, 100L)).thenReturn(false);

        wishlistService.add("a@b.com", 100L);

        verify(wishlistItemRepository).save(any(WishlistItem.class));
    }

    @Test
    void add_isIdempotentWhenAlreadySaved() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(packageService.getPackageById(100L)).thenReturn(new PackageDetailResponse());
        when(wishlistItemRepository.existsByUserIdAndPackageId(7L, 100L)).thenReturn(true);

        wishlistService.add("a@b.com", 100L);

        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void add_unknownPackage_throwsNotFound() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(packageService.getPackageById(999L)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.add("a@b.com", 999L));
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void list_returnsPackagesInSavedOrder_skippingStaleIds() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(packageService.getAllPackages()).thenReturn(List.of(pkg(100, "A"), pkg(101, "B")));
        when(wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(item(101), item(555), item(100)));

        assertThat(wishlistService.list("a@b.com")).extracting(PackageSummaryResponse::getId)
                .containsExactly(101L, 100L);
    }

    @Test
    void ids_returnsSavedPackageIds() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(item(100), item(101)));

        assertThat(wishlistService.ids("a@b.com")).containsExactly(100L, 101L);
    }

    @Test
    void remove_deletesForThisUserOnly() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        wishlistService.remove("a@b.com", 100L);

        verify(wishlistItemRepository).deleteByUserIdAndPackageId(7L, 100L);
    }

    @Test
    void unknownUser_throwsNotFound() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.ids("x@y.com"));
    }
}
