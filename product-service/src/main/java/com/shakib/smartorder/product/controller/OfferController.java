package com.shakib.smartorder.product.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shakib.smartorder.product.dto.OfferRequest;
import com.shakib.smartorder.product.dto.OfferResponse;
import com.shakib.smartorder.product.security.CurrentUser;
import com.shakib.smartorder.product.service.OfferService;

import jakarta.validation.Valid;

// A seller's discounts. All endpoints belong to the calling seller's own brand.
@RestController
@RequestMapping("/api/v1/brands/me/offers")
public class OfferController {

    private final OfferService service;

    public OfferController(OfferService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OfferResponse> create(@Valid @RequestBody OfferRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(CurrentUser.from(authentication), request));
    }

    @GetMapping
    public List<OfferResponse> list(Authentication authentication)
    {
        return service.listMine(CurrentUser.from(authentication));
    }

    // Switches the offer off. It stays in the list so the seller can see their history.
    @DeleteMapping("/{offerId}")
    public ResponseEntity<Void> deactivate(@PathVariable Long offerId, Authentication authentication)
    {
        service.deactivate(CurrentUser.from(authentication), offerId);
        return ResponseEntity.noContent().build();
    }
}
