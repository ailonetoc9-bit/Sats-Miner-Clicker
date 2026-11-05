package com.jose.minerclicker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GameExceptionHandler {
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> tratarRegraDoJogo(RuntimeException erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", erro.getMessage()));
    }
}
