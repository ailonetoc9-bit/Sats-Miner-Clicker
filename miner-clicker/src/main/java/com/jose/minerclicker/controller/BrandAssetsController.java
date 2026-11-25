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
            "miner-club-logo.png", new BrandAsset("static/brand/miner-club-logo.png.b64", MediaType.IMAGE_PNG, true),
            "miner-club-cover.jpg", new BrandAsset("static/brand/miner-club-cover.jpg.b64", MediaType.IMAGE_JPEG, true),
            "miner-club-garage-v2.jpg", new BrandAsset("static/brand/miner-club-garage-v2.jpg", MediaType.IMAGE_JPEG, false)
    );

    @GetMapping(value = "/brand/{fileName:.+}")
    public ResponseEntity<byte[]> getBrandAsset(@PathVariable String fileName) throws IOException {
        BrandAsset asset = ASSETS.get(fileName);
        if (asset == null) return ResponseEntity.notFound().build();
        byte[] content = new ClassPathResource(asset.resourcePath()).getInputStream().readAllBytes();
        byte[] image = asset.base64Encoded()
                ? Base64.getDecoder().decode(new String(content))
                : content;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, asset.mediaType().toString())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(image);
    }

    private record BrandAsset(String resourcePath, MediaType mediaType, boolean base64Encoded) {}
}
