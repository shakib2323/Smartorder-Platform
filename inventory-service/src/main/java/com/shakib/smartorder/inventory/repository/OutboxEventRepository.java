package com.shakib.smartorder.inventory.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.shakib.smartorder.inventory.entity.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    long countByAggregateIdAndEventType(Long aggregateId, String eventType);

    long countByAggregateIdAndStatus(Long aggregateId, String status);

    long countByStatus(String status);

    // Oldest first: events must be published in the order they were written. 
    List<OutboxEvent> findByStatusOrderByIdAsc(String status, Pageable pageable);

    // The status guard makes marking idempotent: a row already SENT is never touched again.
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE outbox_events "
            + "SET status = 'SENT', sent_at = CURRENT_TIMESTAMP(6), attempts = attempts + 1 "
            + "WHERE id = :id AND status = 'PENDING'",
            nativeQuery = true)
    int markSent(@Param("id") Long id);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE outbox_events SET attempts = attempts + 1 WHERE id = :id",
            nativeQuery = true)
    int recordFailure(@Param("id") Long id);
}
