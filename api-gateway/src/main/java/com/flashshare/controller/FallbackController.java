package com.flashshare.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping; // 👈 Switched to RequestMapping
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class FallbackController {

    // Handles POST, GET, etc. forwarded from the rider-service route
    @RequestMapping("/fallback/riderService")
    public ResponseEntity<Map<String, Object>> riderServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "TEMPORARILY_UNAVAILABLE");
        response.put("message", "Flash-Share Rider Dispatch system is experiencing heavy traffic or maintenance. Your request is safely cached. Please try again in a few moments.");
        response.put("code", 503);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // Handles POST, GET, etc. forwarded from the driver-service route
    @RequestMapping("/fallback/driverService")
    public ResponseEntity<Map<String, Object>> driverServiceFallback() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "MATCH_ENGINE_OFFLINE");
        response.put("message", "We are currently having trouble reaching nearby drivers. Please refresh the map shortly.");
        response.put("code", 503);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}