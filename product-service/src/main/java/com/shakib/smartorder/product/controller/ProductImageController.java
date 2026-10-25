package com.shakib.smartorder.product.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.shakib.smartorder.product.dto.ImageResponse;
import com.shakib.smartorder.product.security.CurrentUser;
import com.shakib.smartorder.product.service.ProductImageService;

@RestController
@RequestMapping("/api/v1/products/{productId}")
public class ProductImageController {

    private final ProductImageService service;

    public ProductImageController(ProductImageService service) {
        this.service = service;
    }

    // Adds a picture (JPG, PNG or WebP, max 5 MB, up to 8 per product). The first one becomes the cover.
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ImageResponse> add(@PathVariable Long productId, @RequestPart("file") MultipartFile file,
            Authentication authentication) 
    {
        return service.add(productId, CurrentUser.from(authentication), file);
    }

    @DeleteMapping("/images/{imageId}")
    public List<ImageResponse> delete(@PathVariable Long productId, @PathVariable Long imageId,
            Authentication authentication)
    {
        return service.delete(productId, imageId, CurrentUser.from(authentication));
    }

    @PutMapping("/images/{imageId}/primary")
    public List<ImageResponse> makePrimary(@PathVariable Long productId, @PathVariable Long imageId,
            Authentication authentication)
    {
        return service.setPrimary(productId, imageId, CurrentUser.from(authentication));
    }

    // Kept so older clients keep working: same as POST /images, answering with the new cover URL.
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> legacyUpload(@PathVariable Long productId, @RequestPart("file") MultipartFile file,
            Authentication authentication) 
    {
        List<ImageResponse> images = service.add(productId, CurrentUser.from(authentication), file);
        String cover = images.stream().filter(ImageResponse::primary).map(ImageResponse::url).findFirst()
                .orElse(images.get(images.size() - 1).url());
        return Map.of("imageUrl", cover);
    }
}
