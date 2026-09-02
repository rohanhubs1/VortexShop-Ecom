package com.ecom.platform.repository;

import com.ecom.platform.entity.Order;
import com.ecom.platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    Page<Order> findByUser(User user, Pageable pageable);
    
    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByUser(User user);
    
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByOrderNumber(String orderNumber);
    
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndUser(Long id, User user);
    
    boolean existsByOrderNumber(String orderNumber);
    
    long countByStatus(com.ecom.platform.entity.OrderStatus status);
}
