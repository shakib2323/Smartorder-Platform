package com.shakib.smartorder.inventory.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shakib.smartorder.inventory.dto.CreateInventoryRequest;
import com.shakib.smartorder.inventory.dto.InventoryResponse;
import com.shakib.smartorder.inventory.dto.ReservationResponse;
import com.shakib.smartorder.inventory.dto.ReserveLine;
import com.shakib.smartorder.inventory.entity.Inventory;
import com.shakib.smartorder.inventory.entity.StockReservation;
import com.shakib.smartorder.inventory.enums.ReservationStatus;
import com.shakib.smartorder.inventory.exception.DuplicateInventoryException;
import com.shakib.smartorder.inventory.exception.InsufficientStockException;
import com.shakib.smartorder.inventory.exception.InventoryNotFoundException;
import com.shakib.smartorder.inventory.exception.ReservationNotFoundException;
import com.shakib.smartorder.inventory.exception.StockAccessDeniedException;
import com.shakib.smartorder.inventory.repository.InventoryRepository;
import com.shakib.smartorder.inventory.repository.StockReservationRepository;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;

    public InventoryService(InventoryRepository inventoryRepository,StockReservationRepository reservationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public InventoryResponse create(CreateInventoryRequest request)
    {
        if (inventoryRepository.existsByProductId(request.productId())) 
        {
            throw new DuplicateInventoryException(request.productId());
        }
        Inventory saved = inventoryRepository.save(
                new Inventory(request.productId(), request.availableQuantity()));
        return InventoryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(Long productId) 
    {
        return InventoryResponse.from(find(productId));
    }

    /*
      Creates the stock row for a new product (called by the ProductCreated event handler).
      Safe to repeat: an existing row is kept, and only gets its owner filled in if it had none.
     */
    @Transactional
    public void createForProduct(Long productId, Long sellerId, int initialStock) 
    {
        inventoryRepository.findByProductId(productId).ifPresentOrElse(existing -> 
        {
            if (existing.getSellerId() == null) 
            {
                existing.assignSeller(sellerId);
            }
        }, () -> inventoryRepository.save(new Inventory(productId, sellerId, Math.max(initialStock, 0))));
    }

    // Relative stock change (restock or manual correction), refused if it would go below zero.
    @Transactional
    public InventoryResponse adjust(Long productId, int delta) 
    {
        if (delta != 0) {
            applyDelta(productId, delta);
        }
        return InventoryResponse.from(find(productId));
    }

    // Same as adjust, but a non-admin may only change stock they own.
    @Transactional
    public InventoryResponse adjustAs(Long productId, int delta, long callerId, boolean admin) 
    {
        Inventory inventory = find(productId);
        if (!admin && (inventory.getSellerId() == null || inventory.getSellerId() != callerId))
        {
            throw new StockAccessDeniedException(productId);
        }
        return adjust(productId, delta);
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> listForSeller(long sellerId, int page, int size)
    {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "id"));
        return inventoryRepository.findBySellerId(sellerId, pageable).map(InventoryResponse::from);
    }

    /*
      Reserves stock for every line of an order, all or nothing.
      Idempotent: if this order already has reservations, they are returned unchanged
      and no stock is taken a second time. Lines are processed in productId order so
      concurrent orders lock rows in the same order and cannot deadlock.
      If any line fails, the exception rolls back the whole transaction, including
      the lines already reserved.
     */
    @Transactional
    public ReservationResponse reserve(Long orderId, List<ReserveLine> lines) 
    {
        List<StockReservation> existing = reservationRepository.findByOrderIdOrderByProductIdAsc(orderId);
        if (!existing.isEmpty()) 
        {
            return ReservationResponse.from(orderId, existing);
        }

        Map<Long, Integer> quantities = new TreeMap<>();
        for (ReserveLine line : lines) {
            quantities.merge(line.productId(), line.quantity(), Integer::sum);
        }

        List<StockReservation> created = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) 
        {
            applyDelta(entry.getKey(), -entry.getValue());
            created.add(new StockReservation(orderId, entry.getKey(), entry.getValue()));
        }
        return ReservationResponse.from(orderId, reservationRepository.saveAll(created));
    }

    /*
      Compensation step: returns reserved stock to inventory.
      Safe to call more than once. The conditional status update lets exactly one
      caller win per reservation, so stock is never added back twice.
     */
    @Transactional
    public ReservationResponse release(Long orderId) 
    {
        List<StockReservation> reservations = reservationRepository.findByOrderIdOrderByProductIdAsc(orderId);
        if (reservations.isEmpty()) 
        {
            throw new ReservationNotFoundException(orderId);
        }
        for (StockReservation reservation : reservations) 
        {
            if (reservation.getStatus() == ReservationStatus.RESERVED
                    && reservationRepository.markReleased(reservation.getId()) == 1) 
            {
                inventoryRepository.adjust(reservation.getProductId(), reservation.getQuantity());
            }
        }
        return ReservationResponse.from(orderId, reservationRepository.findByOrderIdOrderByProductIdAsc(orderId));
    }

    /*
      Like release, but a missing reservation is not an error. The Kafka consumer uses this:
      an order that never reserved any stock simply has nothing to give back.
     */
    @Transactional
    public boolean releaseIfPresent(Long orderId) 
    {
        if (reservationRepository.findByOrderIdOrderByProductIdAsc(orderId).isEmpty()) 
        {
            return false;
        }
        release(orderId);
        return true;
    }

    private void applyDelta(Long productId, int delta) 
    {
        int updated = inventoryRepository.adjust(productId, delta);
        if (updated == 0) {
            if (inventoryRepository.existsByProductId(productId)) 
            {
                throw new InsufficientStockException(productId);
            }
            throw new InventoryNotFoundException(productId);
        }
    }

    private Inventory find(Long productId)
    {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }
}

