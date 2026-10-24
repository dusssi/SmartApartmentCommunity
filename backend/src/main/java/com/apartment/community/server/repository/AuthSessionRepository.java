package com.apartment.community.server.repository;

import com.apartment.community.server.domain.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);
}
