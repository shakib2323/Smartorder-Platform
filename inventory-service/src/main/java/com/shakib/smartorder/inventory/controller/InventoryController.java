package com.shakib.smartorder.inventory.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shakib.smartorder.inventory.dto.AdjustStockRequest;
import com.shakib.smartorder.inventory.dto.CreateInventoryRequest;
import com.shakib.smartorder.inventory.dto.InventoryResponse;
import com.shakib.smartorder.inventory.dto.PageResponse;
import com.shakib.smartorder.inventory.service.InventoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody CreateInventoryRequest request) 
    {
        InventoryResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/inventory/" + created.productId())).body(created);
    }

    @GetMapping("/{productId}")
    public InventoryResponse get(@PathVariable Long productId) 
    {
        return service.get(productId);
    }

    // Admin: any product. Seller: only products they own (checked against the owner stored from ProductCreated).
    @PatchMapping("/{productId}")
    public InventoryResponse adjust(@PathVariable Long productId,
            @Valid @RequestBody AdjustStockRequest request, Authentication authentication) 
    {
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return service.adjustAs(productId, request.delta(), callerId(authentication), admin);
    }

    // The calling seller's own stock levels.
    @GetMapping("/mine")
    public PageResponse<InventoryResponse> mine(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, Authentication authentication) 
    {
        return PageResponse.of(service.listForSeller(callerId(authentication), page, size));
    }

    private long callerId(Authentication authentication) 
    {
        try {
            long id = Long.parseLong(((Jwt) authentication.getPrincipal()).getSubject());
            if (id > 0) {
                return id;
            }
        } catch (RuntimeException ignored) {
            // falls through to the 401 below
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token subject");
    }
}
