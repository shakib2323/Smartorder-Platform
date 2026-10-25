package com.shakib.smartorder.product.storage;

import java.io.IOException;

// Where uploaded images live. Local disk today; an S3 implementation can replace it without touching callers. 
public interface ImageStorage {

    // Saves the bytes under a new unique name and returns the public URL path. 
    String store(String namePrefix, byte[] bytes, String extension) throws IOException;

    // Removes a stored image. Unknown or foreign URLs are ignored. 
    void delete(String url);
}
