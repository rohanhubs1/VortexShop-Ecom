package com.ecom.platform.repository;

import com.ecom.platform.entity.VerificationToken;
import com.ecom.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUser(User user);
}
