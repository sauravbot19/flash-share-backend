package com.flashshare.riderservice.controller;

import com.flashshare.riderservice.model.Rider;
import com.flashshare.riderservice.service.RiderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/riders")
@RequiredArgsConstructor
public class RiderController {

    private final RiderService riderService;

    @PostMapping
    public ResponseEntity<Rider> createRider(@RequestBody Rider rider) {
        Rider createdRider = riderService.registerRider(rider);
        return new ResponseEntity<>(createdRider, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rider> getRiderById(@PathVariable Long id) {
        Rider rider = riderService.getRiderById(id);
        return ResponseEntity.ok(rider);
    }

    @PatchMapping("/{id}/location")
    public ResponseEntity<String> updateLocation(
            @PathVariable Long id,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {

        riderService.updateLiveLocation(id, latitude, longitude);
        return ResponseEntity.ok("Live location updated successfully in cache and db.");
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<Rider>> getNearbyRiders(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radius) {

        List<Rider> riders = riderService.getNearbyRiders(latitude, longitude, radius);
        return ResponseEntity.ok(riders);
    }
}