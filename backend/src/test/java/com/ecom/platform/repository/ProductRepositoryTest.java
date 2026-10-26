package com.ecom.platform.repository;

import com.ecom.platform.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the hand-written JPQL in ProductRepository and OrderRepository against an in-memory H2 database,
 * so no local MySQL is needed. The schema comes from the JPA entities; the MySQL Flyway scripts are skipped.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ProductRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;

    private final Pageable byName = PageRequest.of(0, 10, Sort.by("name"));

    private Category peripherals;
    private Category keyboards;
    private Product wirelessMouse;
    private Product gamingMouse;
    private Product expiredDealKeyboard;
    private Product deletedMouse;

    private Product persist(String name, Category category, String price, int stock) {
        Product product = Product.builder()
                .category(category)
                .name(name)
                .slug(name.toLowerCase().replace(' ', '-'))
                .description(name + " for your desk")
                .price(new BigDecimal(price))
                .stockQuantity(stock)
                .build();
        return em.persist(product);
    }

    @BeforeEach
    void setUp() {
        peripherals = em.persist(Category.builder().name("Peripherals").slug("peripherals").build());
        keyboards = em.persist(Category.builder().name("Keyboards").slug("keyboards").build());

        wirelessMouse = persist("Wireless Mouse", peripherals, "20.00", 5);

        gamingMouse = persist("Gaming Mouse", peripherals, "60.00", 10);
        gamingMouse.setTopDeal(true);
        gamingMouse.setDiscountPercentage(20);
        gamingMouse.setOfferExpiryDate(LocalDateTime.now().plusDays(3));
        gamingMouse.setAverageRating(4.5);

        expiredDealKeyboard = persist("Mechanical Keyboard", keyboards, "40.00", 8);
        expiredDealKeyboard.setTopDeal(true);
        expiredDealKeyboard.setOfferExpiryDate(LocalDateTime.now().minusDays(1));

        deletedMouse = persist("Retired Mouse", peripherals, "15.00", 1);
        deletedMouse.setDeleted(true);

        em.flush();
        em.clear();
    }

    private List<String> search(String keyword, Long categoryId, String minPrice, String maxPrice, Double minRating, Boolean topDeal) {
        return productRepository.searchProducts(keyword, categoryId,
                        minPrice == null ? null : new BigDecimal(minPrice),
                        maxPrice == null ? null : new BigDecimal(maxPrice),
                        minRating, topDeal, LocalDateTime.now(), byName)
                .map(Product::getName)
                .getContent();
    }

    @Test
    void searchWithNoFiltersReturnsAllLiveProducts() {
        assertThat(search(null, null, null, null, null, null))
                .containsExactly("Gaming Mouse", "Mechanical Keyboard", "Wireless Mouse");
    }

    @Test
    void searchMatchesKeywordCaseInsensitivelyInNameOrDescription() {
        assertThat(search("MOUSE", null, null, null, null, null)).containsExactly("Gaming Mouse", "Wireless Mouse");
        assertThat(search("desk", null, null, null, null, null)).hasSize(3);
    }

    @Test
    void searchFiltersByCategoryPriceAndRating() {
        assertThat(search(null, keyboards.getId(), null, null, null, null)).containsExactly("Mechanical Keyboard");
        assertThat(search(null, null, "30", "50", null, null)).containsExactly("Mechanical Keyboard");
        assertThat(search(null, null, null, null, 4.0, null)).containsExactly("Gaming Mouse");
    }

    @Test
    void expiredOfferCountsAsNotATopDeal() {
        assertThat(search(null, null, null, null, null, true)).containsExactly("Gaming Mouse");
        assertThat(search(null, null, null, null, null, false)).containsExactly("Mechanical Keyboard", "Wireless Mouse");
        assertThat(productRepository.findActiveTopDeals(LocalDateTime.now(), byName).map(Product::getName))
                .containsExactly("Gaming Mouse");
    }

    @Test
    void softDeletedProductsAreHidden() {
        assertThat(productRepository.findByIdAndDeletedFalse(deletedMouse.getId())).isEmpty();
        assertThat(productRepository.findByIdAndDeletedFalse(wirelessMouse.getId())).isPresent();
        assertThat(productRepository.findByDeletedFalse(byName).getTotalElements()).isEqualTo(3);
    }

    @Test
    void decrementStockOnlySucceedsWhenEnoughStock() {
        assertThat(productRepository.decrementStockIfAvailable(wirelessMouse.getId(), 3)).isEqualTo(1);
        assertThat(productRepository.decrementStockIfAvailable(wirelessMouse.getId(), 3)).isZero();
        em.clear();

        Product reloaded = em.find(Product.class, wirelessMouse.getId());
        assertThat(reloaded.getStockQuantity()).isEqualTo(2);
        assertThat(reloaded.getSalesCount()).isEqualTo(3);
    }

    @Test
    void lockingQueryReturnsRequestedRows() {
        List<Product> locked = productRepository.findAllByIdForUpdate(List.of(gamingMouse.getId(), wirelessMouse.getId()));

        assertThat(locked).extracting(Product::getName).containsExactlyInAnyOrder("Gaming Mouse", "Wireless Mouse");
    }

    @Test
    void slugAndCategoryExistenceChecks() {
        assertThat(productRepository.existsBySlug("gaming-mouse")).isTrue();
        assertThat(productRepository.existsBySlugAndIdNot("gaming-mouse", gamingMouse.getId())).isFalse();
        assertThat(productRepository.existsBySlugAndIdNot("gaming-mouse", wirelessMouse.getId())).isTrue();
        assertThat(productRepository.existsByCategoryId(keyboards.getId())).isTrue();

        Category empty = em.persistAndFlush(Category.builder().name("Empty").slug("empty").build());
        assertThat(productRepository.existsByCategoryId(empty.getId())).isFalse();
    }

    @Test
    void activeOrderCheckIgnoresFinishedOrders() {
        User user = em.persist(User.builder().email("buyer@example.com").password("x").firstName("B").lastName("U").build());
        Address address = em.persist(Address.builder().user(user).title("Home").streetAddress("1 Main St").city("Noida")
                .state("UP").zipCode("201301").country("India").build());
        em.persist(Order.builder().user(user).orderNumber("ORD-1").status(OrderStatus.DELIVERED)
                .totalAmount(BigDecimal.TEN).finalAmount(BigDecimal.TEN).shippingAddress(address).build());
        em.flush();

        List<OrderStatus> active = List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PACKED, OrderStatus.SHIPPED);
        assertThat(orderRepository.existsByUserAndStatusIn(user, active)).isFalse();

        em.persistAndFlush(Order.builder().user(user).orderNumber("ORD-2").status(OrderStatus.SHIPPED)
                .totalAmount(BigDecimal.TEN).finalAmount(BigDecimal.TEN).shippingAddress(address).build());
        assertThat(orderRepository.existsByUserAndStatusIn(user, active)).isTrue();
    }
}
