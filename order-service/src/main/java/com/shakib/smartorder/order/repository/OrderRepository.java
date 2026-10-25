package com.shakib.smartorder.order.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shakib.smartorder.order.entity.Order;
import com.shakib.smartorder.order.enums.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);

    // Idempotency keys are per customer: the same key from two customers is two different requests.
    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsByCustomerIdAndIdempotencyKey(Long customerId, String idempotencyKey);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsByIdAndSellerId(Long id, Long sellerId);

    Page<Order> findBySellerId(Long sellerId, Pageable pageable);

    Page<Order> findBySellerIdAndStatus(Long sellerId, OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsByIdAndCustomerId(Long id, Long customerId);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    Page<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /*
      Atomic state transition in one SQL statement. It succeeds only if the order is still
      in the expected "from" state, so two concurrent requests can never both win.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE orders SET status = :to, updated_at = CURRENT_TIMESTAMP(6) "
            + "WHERE id = :id AND status = :from",
            nativeQuery = true)
    int transition(@Param("id") Long id, @Param("from") String from, @Param("to") String to);

    // Reservations whose hold time has run out (oldest first).
    @Query("select o.id from Order o where o.status = :status and o.reserveExpiresAt < :now "
            + "order by o.reserveExpiresAt asc")
    List<Long> findExpiredReservationIds(@Param("status") OrderStatus status, @Param("now") Instant now,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Order o set o.trackingNumber = :tracking, o.shippedAt = :at where o.id = :id")
    int recordShipment(@Param("id") Long id, @Param("tracking") String tracking, @Param("at") Instant at);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Order o set o.deliveredAt = :at where o.id = :id")
    int recordDelivery(@Param("id") Long id, @Param("at") Instant at);

    // One row per status: [status, number of orders, sum of order totals]. 
    @Query("select o.status, count(o), sum(o.totalAmount) from Order o where o.sellerId = :sellerId group by o.status")
    List<Object[]> summarizeBySeller(@Param("sellerId") Long sellerId);
}
