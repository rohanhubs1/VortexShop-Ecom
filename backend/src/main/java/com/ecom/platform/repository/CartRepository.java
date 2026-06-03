package com.ecom.platform.repository;

import com.ecom.platform.entity.Cart;
import com.ecom.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUser(User user);
    
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserId(Long userId);
}
