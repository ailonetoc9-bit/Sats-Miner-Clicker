package com.jose.minerclicker.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class BrandAssetsController {
    private static final Map<String, String> ASSETS = Map.of(
            "sats-miner-club-logo.png", "static/brand/sats-miner-club-logo.png.b64",
            "sats-miner-club-cover.png", "static/brand/sats-miner-club-cover.png.b64"
    );

    @GetMapping(value = "/brand/{fileName:.+}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getBrandAsset(@PathVariable String fileName) throws IOException {
        String resourcePath = ASSETS.get(fileName);
        if (resourcePath == null) return ResponseEntity.notFound().build();
        String encoded = new String(new ClassPathResource(resourcePath).getInputStream().readAllBytes());
        byte[] image = Base64.getDecoder().decode(encoded);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(image);
    }
}
