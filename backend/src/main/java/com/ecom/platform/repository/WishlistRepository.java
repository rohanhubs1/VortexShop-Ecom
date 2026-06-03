package com.ecom.platform.repository;

import com.ecom.platform.entity.User;
import com.ecom.platform.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByUser(User user);
    Optional<Wishlist> findByUserId(Long userId);
}
