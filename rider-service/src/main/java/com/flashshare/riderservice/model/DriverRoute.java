package com.flashshare.riderservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(name = "start_address", nullable = false)
    private String startAddress;

    @Column(name = "end_address", nullable = false)
    private String endAddress;

    @Column(name = "encoded_polyline", columnDefinition = "TEXT", nullable = false)
    private String encodedPolyline;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}