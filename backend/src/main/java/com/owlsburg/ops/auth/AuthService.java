package com.owlsburg.ops.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final RefreshTokenBlacklistRepository blacklistRepository;

    public AuthService(RefreshTokenBlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }

    public boolean isTokenBlacklisted(String tokenHash) {
        return blacklistRepository.existsByTokenHash(tokenHash);
    }

    public void blacklistToken(String tokenHash, Instant expiredAt) {
        if (!blacklistRepository.existsByTokenHash(tokenHash)) {
            RefreshTokenBlacklistEntity entry = new RefreshTokenBlacklistEntity();
            entry.setTokenHash(tokenHash);
            entry.setExpiredAt(expiredAt);
            blacklistRepository.save(entry);
        }
    }
}
