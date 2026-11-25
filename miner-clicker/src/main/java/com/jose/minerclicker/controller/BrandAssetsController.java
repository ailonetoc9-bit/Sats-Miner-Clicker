package com.jose.minerclicker.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class BrandAssetsController {
    private static final Map<String, BrandAsset> ASSETS = Map.of(
            "miner-club-logo.png", new BrandAsset("static/brand/miner-club-logo.png.b64", MediaType.IMAGE_PNG),
            "miner-club-cover.jpg", new BrandAsset("static/brand/miner-club-cover.jpg.b64", MediaType.IMAGE_JPEG)
    );

    @GetMapping(value = "/brand/{fileName:.+}")
    public ResponseEntity<byte[]> getBrandAsset(@PathVariable String fileName) throws IOException {
        BrandAsset asset = ASSETS.get(fileName);
        if (asset == null) return ResponseEntity.notFound().build();
        String encoded = new String(new ClassPathResource(asset.resourcePath()).getInputStream().readAllBytes());
        byte[] image = Base64.getDecoder().decode(encoded);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, asset.mediaType().toString())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(image);
    }

    private record BrandAsset(String resourcePath, MediaType mediaType) {}
}
