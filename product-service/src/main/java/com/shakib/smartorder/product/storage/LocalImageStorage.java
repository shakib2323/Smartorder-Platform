package com.shakib.smartorder.product.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorage.class);
    private static final String URL_PREFIX = "/uploads/products/";

    private final Path root;

    public LocalImageStorage(@Value("${app.upload-dir:./uploads/products}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(String namePrefix, byte[] bytes, String extension) throws IOException {
        Files.createDirectories(root);
        String filename = namePrefix + "-" + UUID.randomUUID() + extension;
        Path destination = root.resolve(filename).normalize();
        if (!destination.startsWith(root)) {
            throw new IOException("Invalid file path");
        }
        Files.write(destination, bytes);
        return URL_PREFIX + filename;
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }
        Path file = root.resolve(url.substring(URL_PREFIX.length())).normalize();
        if (!file.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete image {}: {}", url, e.toString());
        }
    }
}
