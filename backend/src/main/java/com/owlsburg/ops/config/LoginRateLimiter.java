package com.owlsburg.ops.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1)))
                .build());
    }
}
