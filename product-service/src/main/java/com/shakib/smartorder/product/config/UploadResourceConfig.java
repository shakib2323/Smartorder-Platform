package com.shakib.smartorder.product.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Serves uploaded images read-only under /uploads/products/**.
@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public UploadResourceConfig(@Value("${app.upload-dir:./uploads/products}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/products/**").addResourceLocations(root.toUri().toString());
    }
}
