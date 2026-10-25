package com.shakib.smartorder.product.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.shakib.smartorder.product.entity.Brand;
import com.shakib.smartorder.product.enums.BrandStatus;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByOwnerUserId(Long ownerUserId);

    boolean existsByOwnerUserId(Long ownerUserId);

    boolean existsByNameIgnoreCase(String name);

    Page<Brand> findByStatus(BrandStatus status, Pageable pageable);
}
