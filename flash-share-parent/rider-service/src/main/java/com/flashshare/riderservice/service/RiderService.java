package com.flashshare.riderservice.service;

import com.flashshare.riderservice.model.Rider;
import com.flashshare.riderservice.repository.RiderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RiderService {

    private final RiderRepository riderRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String REDIS_GEO_KEY = "riders:locations";
    private final RiderEventProducer riderEventProducer;
    private final ObjectMapper objectMapper; // Injected automatically via @RequiredArgsConstructor
    private static final String REDIS_PROFILE_PREFIX = "rider:profile:";

    public Rider registerRider(Rider rider) {
        if (riderRepository.findByEmail(rider.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered: " + rider.getEmail());
        }
        // Save to relational database
        Rider savedRider = riderRepository.save(rider);

        // Construct a simple JSON event payload strings
        String eventPayload = String.format("{\"id\": %d, \"name\": \"%s\", \"action\": \"CREATED\"}",
                savedRider.getId(), savedRider.getName());

        // Asynchronously dispatch event stream out to the ecosystem
        riderEventProducer.sendRiderCreatedEvent(savedRider.getId().toString(), eventPayload);

        return savedRider;
    }


    public Rider getRiderById(Long id) {
        String cacheKey = REDIS_PROFILE_PREFIX + id;

        // 1. Attempt to fetch from Redis Cache
        String cachedRiderJson = redisTemplate.opsForValue().get(cacheKey);

        if (cachedRiderJson != null) {
            try {
                // Cache Hit: Deserialize JSON string back to Rider object
                return objectMapper.readValue(cachedRiderJson, Rider.class);
            } catch (JsonProcessingException e) {
                // Fallback if serialization fails: log and continue to DB
                System.err.println("Failed to deserialize rider cache: " + e.getMessage());
            }
        }

        // 2. Cache Miss: Fetch from MySQL Source of Truth
        Rider databaseRider = riderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rider not found with id: " + id));

        // 3. Populate Redis Cache asynchronously for the next lookup
        try {
            String riderJsonToCache = objectMapper.writeValueAsString(databaseRider);
            // Set a Time-To-Live (TTL) of 10 minutes to prevent stale data
            redisTemplate.opsForValue().set(cacheKey, riderJsonToCache, 10, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize rider for caching: " + e.getMessage());
        }

        return databaseRider;
    }

    public void updateLiveLocation(Long riderId, Double latitude, Double longitude) {
        // 1. Verify rider exists in MySQL source of truth
        Rider rider = getRiderById(riderId);

        // 2. Update persistent storage (MySQL)
        rider.setCurrentLatitude(latitude);
        rider.setCurrentLongitude(longitude);
        riderRepository.save(rider);

        // 3. Push real-time spatial data to Redis Cache
        // Redis Geo expect parameters in order: (longitude, latitude)
        redisTemplate.opsForGeo().add(
                REDIS_GEO_KEY,
                new Point(longitude, latitude),
                riderId.toString()
        );
    }

    public List<Rider> getNearbyRiders(Double latitude, Double longitude, Double radiusInKm) {
        // 1. Define the search area using a circle centered at the coordinates
        Point point = new Point(longitude, latitude); // Redis expects (longitude, latitude)
        Distance distance = new Distance(radiusInKm, RedisGeoCommands.DistanceUnit.KILOMETERS);
        Circle searchArea = new Circle(point, distance);

        // 2. Query Redis for members within that circle
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(REDIS_GEO_KEY, searchArea);

        if (results == null || results.getContent().isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Extract the rider IDs from the Redis results
        List<Long> riderIds = results.getContent().stream()
                .map(result -> Long.parseLong(result.getContent().getName()))
                .collect(Collectors.toList());

        // 4. Hydrate the full profiles from MySQL using the extracted IDs
        return riderRepository.findAllById(riderIds);
    }
}