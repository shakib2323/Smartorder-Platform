package com.shakib.smartorder.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ImageTypeDetectorTest {

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }

    @Test
    void recognisesJpeg() {
        assertEquals(".jpg", ImageTypeDetector.detectExtension(
                bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1)));
    }

    @Test
    void recognisesPng() {
        assertEquals(".png", ImageTypeDetector.detectExtension(
                bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13)));
    }

    @Test
    void recognisesWebp() {
        assertEquals(".webp", ImageTypeDetector.detectExtension(
                bytes('R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P')));
    }

    @Test
    void aWavFileInARiffContainerIsNotWebp() {
        assertNull(ImageTypeDetector.detectExtension(bytes('R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'A', 'V', 'E')));
    }

    @Test
    void rejectsAScriptRenamedToJpg() {
        byte[] php = "<?php system($_GET['c']); ?>".getBytes(StandardCharsets.UTF_8);
        assertNull(ImageTypeDetector.detectExtension(php));
    }

    @Test
    void rejectsGifAndTinyOrMissingInput() {
        assertNull(ImageTypeDetector.detectExtension("GIF89a......".getBytes(StandardCharsets.US_ASCII)));
        assertNull(ImageTypeDetector.detectExtension(bytes(0xFF, 0xD8, 0xFF)));
        assertNull(ImageTypeDetector.detectExtension(null));
    }
}
