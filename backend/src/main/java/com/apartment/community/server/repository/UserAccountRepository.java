package com.apartment.community.server.repository;

import com.apartment.community.server.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findByNormalizedEmail(String normalizedEmail);
}
