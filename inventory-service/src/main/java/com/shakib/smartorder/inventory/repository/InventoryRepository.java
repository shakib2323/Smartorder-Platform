package com.shakib.smartorder.inventory.repository;


import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shakib.smartorder.inventory.entity.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    Page<Inventory> findBySellerId(Long sellerId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE inventory "
            + "SET available_quantity = available_quantity + :delta, updated_at = CURRENT_TIMESTAMP(6) "
            + "WHERE product_id = :productId AND available_quantity + :delta >= 0",
            nativeQuery = true)
    int adjust(@Param("productId") Long productId, @Param("delta") int delta);
}

