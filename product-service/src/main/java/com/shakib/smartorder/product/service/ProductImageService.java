package com.shakib.smartorder.product.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.shakib.smartorder.product.dto.ImageResponse;
import com.shakib.smartorder.product.entity.ProductImage;
import com.shakib.smartorder.product.exception.ApiException;
import com.shakib.smartorder.product.repository.ProductImageRepository;
import com.shakib.smartorder.product.security.CurrentUser;
import com.shakib.smartorder.product.storage.ImageStorage;

@Service
public class ProductImageService {

    private final ProductService productService;
    private final ProductImageRepository imageRepository;
    private final ImageStorage imageStorage;
    private final int maxPerProduct;

    public ProductImageService(ProductService productService, ProductImageRepository imageRepository,
            ImageStorage imageStorage, @Value("${app.images.max-per-product:8}") int maxPerProduct) {
        this.productService = productService;
        this.imageRepository = imageRepository;
        this.imageStorage = imageStorage;
        this.maxPerProduct = maxPerProduct;
    }

    @Transactional
    public List<ImageResponse> add(Long productId, CurrentUser user, MultipartFile file)
    {
        productService.requireOwned(productId, user);
        byte[] bytes = BrandService.readImage(file);

        // The type comes from the file's own bytes, never from the client-supplied header or name.
        String extension = ImageTypeDetector.detectExtension(bytes);
        if (extension == null)
        {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPG, PNG and WebP images are supported");
        }
        long existing = imageRepository.countByProductId(productId);
        if (existing >= maxPerProduct)
        {
            throw ApiException.conflict("A product can have at most " + maxPerProduct + " images");
        }

        String url;
        try {
            url = imageStorage.store("p" + productId, bytes, extension);
        } catch (IOException e)
        {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the image");
        }
        ImageCleanup.deleteIfRolledBack(imageStorage, url);
        imageRepository.save(new ProductImage(productId, url, (int) existing, existing == 0));
        return list(productId);
    }

    @Transactional
    public List<ImageResponse> delete(Long productId, Long imageId, CurrentUser user)
    {
        productService.requireOwned(productId, user);
        ProductImage image = findImage(productId, imageId);
        imageRepository.delete(image);
        imageRepository.flush();
        ImageCleanup.deleteAfterCommit(imageStorage, image.getUrl());

        if (image.isPrimary()) 
        {
            // Promote the next image so the product always has a cover picture while it has any image.
            imageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId).stream().findFirst()
                    .ifPresent(next -> next.setPrimary(true));
        }
        return list(productId);
    }

    @Transactional
    public List<ImageResponse> setPrimary(Long productId, Long imageId, CurrentUser user)
    {
        productService.requireOwned(productId, user);
        findImage(productId, imageId);
        for (ProductImage image : imageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId))
        {
            image.setPrimary(image.getId().equals(imageId));
        }
        imageRepository.flush();
        return list(productId);
    }

    private ProductImage findImage(Long productId, Long imageId) 
    {
        return imageRepository.findById(imageId)
                .filter(i -> i.getProductId().equals(productId))
                .orElseThrow(() -> ApiException.notFound("Image not found with id " + imageId));
    }

    private List<ImageResponse> list(Long productId)
    {
        return imageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId).stream()
                .map(ImageResponse::from).toList();
    }
}
