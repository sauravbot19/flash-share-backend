package com.flashshare.controller;

import com.flashshare.entity.Driver;
import com.flashshare.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverRepository driverRepository;

    @PutMapping("/allocate-available")
    @Transactional
    public ResponseEntity<Driver> allocateAvailableDriver() {
        return driverRepository.findByStatus("AVAILABLE").stream()
                .findFirst()
                .map(driver -> {
                    driver.setStatus("ON_RIDE");
                    Driver updatedDriver = driverRepository.save(driver);
                    return ResponseEntity.ok(updatedDriver);
                })
                .orElse(ResponseEntity.noContent().build());
    }
}