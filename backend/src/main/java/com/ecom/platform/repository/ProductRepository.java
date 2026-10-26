package com.ecom.platform.repository;

import com.ecom.platform.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    
    Optional<Product> findBySlugAndDeletedFalse(String slug);
    
    Optional<Product> findByIdAndDeletedFalse(Long id);
    
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    java.util.List<Product> findAllByIdForUpdate(@Param("ids") java.util.List<Long> ids);
    
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :qty, p.salesCount = p.salesCount + :qty WHERE p.id = :id AND p.stockQuantity >= :qty")
    int decrementStockIfAvailable(@Param("id") Long id, @Param("qty") int qty);

    Page<Product> findByDeletedFalse(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.isTopDeal = true AND p.deleted = false AND (p.offerExpiryDate IS NULL OR p.offerExpiryDate > :currentTime)")
    Page<Product> findActiveTopDeals(@Param("currentTime") java.time.LocalDateTime currentTime, Pageable pageable);
    
    boolean existsBySlug(String slug);
    
    boolean existsBySlugAndIdNot(String slug, Long id);
    
    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.deleted = false AND " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:minRating IS NULL OR p.averageRating >= :minRating) AND " +
           "(:isTopDeal IS NULL OR " +
           "  (:isTopDeal = true AND p.isTopDeal = true AND (p.offerExpiryDate IS NULL OR p.offerExpiryDate > :currentTime)) OR " +
           "  (:isTopDeal = false AND (p.isTopDeal = false OR (p.offerExpiryDate IS NOT NULL AND p.offerExpiryDate <= :currentTime)))" +
           ")")
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            @Param("isTopDeal") Boolean isTopDeal,
            @Param("currentTime") java.time.LocalDateTime currentTime,
            Pageable pageable
    );
}
