package com.shakib.smartorder.inventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shakib.smartorder.inventory.entity.StockReservation;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    // Ordered by productId so every transaction locks rows in the same order (no deadlocks).
    List<StockReservation> findByOrderIdOrderByProductIdAsc(Long orderId);

    /*
      Only one caller can flip RESERVED to RELEASED. Returns 1 for the winner and 0
      for everyone else, so stock is returned exactly once even if the same release
      request arrives twice or concurrently.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE stock_reservations "
            + "SET status = 'RELEASED', updated_at = CURRENT_TIMESTAMP(6) "
            + "WHERE id = :id AND status = 'RESERVED'",
            nativeQuery = true)
    int markReleased(@Param("id") Long id);
}
