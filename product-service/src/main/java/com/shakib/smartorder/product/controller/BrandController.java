package com.shakib.smartorder.product.controller;

import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.shakib.smartorder.product.dto.BrandDetailResponse;
import com.shakib.smartorder.product.dto.BrandRequest;
import com.shakib.smartorder.product.dto.BrandResponse;
import com.shakib.smartorder.product.dto.PageResponse;
import com.shakib.smartorder.product.enums.BrandStatus;
import com.shakib.smartorder.product.security.CurrentUser;
import com.shakib.smartorder.product.service.BrandService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/brands")
public class BrandController {

    private final BrandService service;

    public BrandController(BrandService service) {
        this.service = service;
    }

    //seller

    //A seller registers their brand. It is pending until an admin approves it.
    @PostMapping
    public ResponseEntity<BrandDetailResponse> register(@Valid @RequestBody BrandRequest request,
            Authentication authentication) {
        BrandDetailResponse created = service.register(CurrentUser.from(authentication), request);
        return ResponseEntity.created(URI.create("/api/v1/brands/me")).body(created);
    }

    @GetMapping("/me")
    public BrandDetailResponse mine(Authentication authentication)
    {
        return service.mine(CurrentUser.from(authentication));
    }

    @PutMapping("/me")
    public BrandDetailResponse updateMine(@Valid @RequestBody BrandRequest request, Authentication authentication) 
    {
        return service.updateMine(CurrentUser.from(authentication), request);
    }

    @PostMapping(value = "/me/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BrandDetailResponse uploadLogo(@RequestPart("file") MultipartFile file, Authentication authentication) {
        return service.uploadLogo(CurrentUser.from(authentication), file);
    }

    //public

    @GetMapping
    public PageResponse<BrandResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) 
    {
        return service.listApproved(page, size);
    }

    @GetMapping("/{id}")
    public BrandResponse get(@PathVariable Long id)
    {
        return service.getApproved(id);
    }

    //admin

    //All brands optionally filtered by status (for example PENDING to see who is waiting for approval).
    @GetMapping("/admin/all")
    public PageResponse<BrandDetailResponse> adminList(@RequestParam(required = false) BrandStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.adminList(status, page, size);
    }

    @PostMapping("/{id}/approve")
    public BrandDetailResponse approve(@PathVariable Long id)
    {
        return service.approve(id);
    }

    @PostMapping("/{id}/suspend")
    public BrandDetailResponse suspend(@PathVariable Long id) 
    {
        return service.suspend(id);
    }
}
