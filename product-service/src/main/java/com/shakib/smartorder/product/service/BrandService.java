package com.shakib.smartorder.product.service;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.shakib.smartorder.product.dto.BrandDetailResponse;
import com.shakib.smartorder.product.dto.BrandRequest;
import com.shakib.smartorder.product.dto.BrandResponse;
import com.shakib.smartorder.product.dto.PageResponse;
import com.shakib.smartorder.product.entity.Brand;
import com.shakib.smartorder.product.enums.BrandStatus;
import com.shakib.smartorder.product.exception.ApiException;
import com.shakib.smartorder.product.repository.BrandRepository;
import com.shakib.smartorder.product.security.CurrentUser;
import com.shakib.smartorder.product.storage.ImageStorage;

@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final ImageStorage imageStorage;

    public BrandService(BrandRepository brandRepository, ImageStorage imageStorage) {
        this.brandRepository = brandRepository;
        this.imageStorage = imageStorage;
    }

    // A seller registers their brand. It stays PENDING until an admin approves it.
    @Transactional
    public BrandDetailResponse register(CurrentUser user, BrandRequest request)
    {
        if (brandRepository.existsByOwnerUserId(user.id())) {
            throw ApiException.conflict("You have already registered a brand");
        }
        if (brandRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw ApiException.conflict("That brand name is already taken");
        }
        Brand brand = brandRepository.save(new Brand(user.id(), request.name(), request.legalName(),
                request.description(), request.contactEmail(), request.gstin(), request.city(), request.state()));
        return BrandDetailResponse.from(brand);
    }

    @Transactional(readOnly = true)
    public BrandDetailResponse mine(CurrentUser user) {
        return BrandDetailResponse.from(requireMine(user));
    }

    @Transactional
    public BrandDetailResponse updateMine(CurrentUser user, BrandRequest request)
    {
        Brand brand = requireMine(user);
        String newName = request.name().trim();
        if (!newName.equalsIgnoreCase(brand.getName()) && brandRepository.existsByNameIgnoreCase(newName)) {
            throw ApiException.conflict("That brand name is already taken");
        }
        brand.update(request.name(), request.legalName(), request.description(), request.contactEmail(),
                request.gstin(), request.city(), request.state());
        return BrandDetailResponse.from(brand);
    }

    @Transactional
    public BrandDetailResponse uploadLogo(CurrentUser user, MultipartFile file) 
    {
        Brand brand = requireMine(user);
        byte[] bytes = readImage(file);
        String extension = ImageTypeDetector.detectExtension(bytes);
        if (extension == null) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPG, PNG and WebP images are supported");
        }
        String oldUrl = brand.getLogoUrl();
        String newUrl;
        try {
            newUrl = imageStorage.store("brand-" + brand.getId(), bytes, extension);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the logo");
        }
        brand.setLogoUrl(newUrl);
        ImageCleanup.deleteAfterCommit(imageStorage, oldUrl);
        ImageCleanup.deleteIfRolledBack(imageStorage, newUrl);
        return BrandDetailResponse.from(brand);
    }

    //public

    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> listApproved(int page, int size) 
    {
        Page<Brand> result = brandRepository.findByStatus(BrandStatus.APPROVED, pageable(page, size));
        return PageResponse.of(result, result.getContent().stream().map(BrandResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public BrandResponse getApproved(Long id)
    {
        Brand brand = brandRepository.findById(id)
                .filter(Brand::isApproved)
                .orElseThrow(() -> ApiException.notFound("Brand not found with id " + id));
        return BrandResponse.from(brand);
    }

    // admin

    @Transactional(readOnly = true)
    public PageResponse<BrandDetailResponse> adminList(BrandStatus status, int page, int size) {
        PageRequest pageable = pageable(page, size);
        Page<Brand> result = status == null ? brandRepository.findAll(pageable)
                : brandRepository.findByStatus(status, pageable);
        return PageResponse.of(result, result.getContent().stream().map(BrandDetailResponse::from).toList());
    }

    @Transactional
    public BrandDetailResponse approve(Long id)
    {
        Brand brand = find(id);
        brand.approve();
        return BrandDetailResponse.from(brand);
    }

    @Transactional
    public BrandDetailResponse suspend(Long id) 
    {
        Brand brand = find(id);
        if (brand.getOwnerUserId() == Brand.PLATFORM_OWNER_ID)
        {
            throw ApiException.conflict("The platform brand cannot be suspended");
        }
        brand.suspend();
        return BrandDetailResponse.from(brand);
    }

    //  helpers

    private Brand find(Long id)
    {
        return brandRepository.findById(id).orElseThrow(() -> ApiException.notFound("Brand not found with id " + id));
    }

    private Brand requireMine(CurrentUser user) 
    {
        return brandRepository.findByOwnerUserId(user.id())
                .orElseThrow(() -> ApiException.notFound("You have not registered a brand yet"));
    }

    private PageRequest pageable(int page, int size)
    {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
    }

    static byte[] readImage(MultipartFile file) 
    {
        if (file == null || file.isEmpty()) 
        {
            throw ApiException.badRequest("Choose an image file");
        }
        try {
            return file.getBytes();
        } catch (IOException e)
        {
            throw ApiException.badRequest("Could not read the uploaded file");
        }
    }
}
