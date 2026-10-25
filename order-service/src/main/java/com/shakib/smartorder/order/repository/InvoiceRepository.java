package com.shakib.smartorder.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shakib.smartorder.order.entity.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByOrderId(Long orderId);
}
