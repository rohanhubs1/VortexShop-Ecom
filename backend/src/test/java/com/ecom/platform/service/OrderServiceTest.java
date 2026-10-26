package com.ecom.platform.service;

import com.ecom.platform.dto.OrderDto;
import com.ecom.platform.dto.PlaceOrderRequest;
import com.ecom.platform.dto.SalesReportDto;
import com.ecom.platform.entity.*;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ecom.platform.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private UserRepository userRepository;
    @Mock private EntityManager entityManager;
    @Mock private CartService cartService;
    @Mock private EmailService emailService;
    @Mock private CacheManager cacheManager;

    @InjectMocks
    private OrderService orderService;

    private User customer;
    private Address address;

    @BeforeEach
    void setUp() {
        customer = customer(1L, "rohan@example.com");
        address = address(5L, customer);
        authenticateAs(customer);
        lenient().when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Cart cartWith(Product... products) {
        Cart cart = Cart.builder().id(20L).user(customer).build();
        long itemId = 100;
        for (Product product : products) {
            cart.getItems().add(CartItem.builder().id(itemId++).cart(cart).product(product).quantity(2).build());
        }
        return cart;
    }

    private Order order(OrderStatus status, Product product, int quantity) {
        Order order = Order.builder()
                .id(40L)
                .user(customer)
                .orderNumber("ORD-TEST")
                .status(status)
                .totalAmount(new BigDecimal("100.00"))
                .finalAmount(new BigDecimal("100.00"))
                .shippingAddress(address)
                .createdAt(LocalDateTime.now())
                .build();
        order.getItems().add(OrderItem.builder().id(41L).order(order).product(product).quantity(quantity).price(product.getPrice()).build());
        return order;
    }

    @Nested
    class PlaceOrder {

        private Product phone;
        private Product charger;

        @BeforeEach
        void setUp() {
            phone = product(7L, "Phone", "100.00", 10);
            charger = product(3L, "Charger", "25.00", 10);
        }

        /** Stubs the happy path up to and including order persistence. */
        private void stubCheckout(Cart cart, List<Product> lockedProducts) {
            when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));
            when(addressRepository.findByIdAndUserAndDeletedFalse(5L, customer)).thenReturn(Optional.of(address));
            when(productRepository.findAllByIdForUpdate(any())).thenReturn(lockedProducts);
        }

        private void stubPersistence() {
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(99L);
                return o;
            });
            when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.decrementStockIfAvailable(anyLong(), anyInt())).thenReturn(1);
        }

        @Test
        void placesOrderLocksProductsInIdOrderAndClearsCart() {
            Cart cart = cartWith(phone, charger);
            stubCheckout(cart, List.of(charger, phone));
            stubPersistence();
            Cache productsCache = mock(Cache.class);
            when(cacheManager.getCache("products")).thenReturn(productsCache);

            OrderDto result = orderService.placeOrder(new PlaceOrderRequest(5L, null));

            // 2 x 100 + 2 x 25
            assertThat(result.getTotalAmount()).isEqualByComparingTo("250.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("0");
            assertThat(result.getFinalAmount()).isEqualByComparingTo("250.00");
            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getOrderNumber()).startsWith("ORD-").hasSize(12);
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getCustomerEmail()).isEqualTo("rohan@example.com");

            // ids are sorted before locking to avoid deadlocks between concurrent checkouts
            verify(productRepository).findAllByIdForUpdate(List.of(3L, 7L));
            verify(entityManager).refresh(phone);
            verify(entityManager).refresh(charger);
            verify(productRepository).decrementStockIfAvailable(7L, 2);
            verify(productRepository).decrementStockIfAvailable(3L, 2);
            verify(cartService).clearCart(cart);
            verify(emailService).sendOrderConfirmation(any(Order.class));
            verify(productsCache).clear();
        }

        @Test
        void usesDiscountedPriceForActiveTopDeals() {
            phone.setTopDeal(true);
            phone.setDiscountPercentage(25);
            Cart cart = cartWith(phone);
            stubCheckout(cart, List.of(phone));
            stubPersistence();

            OrderDto result = orderService.placeOrder(new PlaceOrderRequest(5L, null));

            // 2 x (100 - 25%)
            assertThat(result.getTotalAmount()).isEqualByComparingTo("150.00");
            assertThat(result.getItems().get(0).getPrice()).isEqualByComparingTo("75.00");
        }

        @Test
        void appliesPercentageCoupon() {
            Cart cart = cartWith(phone);
            stubCheckout(cart, List.of(phone));
            stubPersistence();
            when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon("SAVE10", 10, "50.00", "100.00")));
            when(orderRepository.findByUser(customer)).thenReturn(List.of());

            OrderDto result = orderService.placeOrder(new PlaceOrderRequest(5L, "save10"));

            assertThat(result.getTotalAmount()).isEqualByComparingTo("200.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("20.00");
            assertThat(result.getFinalAmount()).isEqualByComparingTo("180.00");
            assertThat(result.getCouponCode()).isEqualTo("SAVE10");
        }

        @Test
        void capsCouponDiscountAtMaxDiscount() {
            Cart cart = cartWith(phone);
            stubCheckout(cart, List.of(phone));
            stubPersistence();
            when(couponRepository.findByCode("HALF")).thenReturn(Optional.of(coupon("HALF", 50, "30.00", "0")));
            when(orderRepository.findByUser(customer)).thenReturn(List.of());

            OrderDto result = orderService.placeOrder(new PlaceOrderRequest(5L, "HALF"));

            assertThat(result.getDiscountAmount()).isEqualByComparingTo("30.00");
            assertThat(result.getFinalAmount()).isEqualByComparingTo("170.00");
        }

        @Test
        void allowsReusingCouponFromCancelledOrder() {
            Cart cart = cartWith(phone);
            stubCheckout(cart, List.of(phone));
            stubPersistence();
            when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon("SAVE10", 10, "50.00", "0")));
            Order cancelled = order(OrderStatus.CANCELLED, phone, 1);
            cancelled.setCouponCode("SAVE10");
            when(orderRepository.findByUser(customer)).thenReturn(List.of(cancelled));

            OrderDto result = orderService.placeOrder(new PlaceOrderRequest(5L, "SAVE10"));

            assertThat(result.getCouponCode()).isEqualTo("SAVE10");
        }

        @Test
        void rejectsCouponAlreadyUsedOnAnotherOrder() {
            Cart cart = cartWith(phone);
            stubCheckout(cart, List.of(phone));
            when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon("SAVE10", 10, "50.00", "0")));
            Order delivered = order(OrderStatus.DELIVERED, phone, 1);
            delivered.setCouponCode("SAVE10");
            when(orderRepository.findByUser(customer)).thenReturn(List.of(delivered));

            assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(5L, "SAVE10")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Coupon already claimed");
            verify(orderRepository, never()).save(any());
        }

        @Test
        void rejectsUnknownCoupon() {
            stubCheckout(cartWith(phone), List.of(phone));
            when(couponRepository.findByCode("NOPE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(5L, "nope")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Invalid coupon code");
        }

        @Test
        void rejectsExpiredCoupon() {
            stubCheckout(cartWith(phone), List.of(phone));
            Coupon expired = coupon("OLD", 10, "50.00", "0");
            expired.setExpiryDate(LocalDateTime.now().minusDays(1));
            when(couponRepository.findByCode("OLD")).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(5L, "OLD")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired or is inactive");
        }

        @Test
        void rejectsCouponWhenMinimumOrderNotMet() {
            stubCheckout(cartWith(phone), List.of(phone));
            when(couponRepository.findByCode("BIG")).thenReturn(Optional.of(coupon("BIG", 10, "50.00", "500.00")));
            when(orderRepository.findByUser(customer)).thenReturn(List.of());

            assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(5L, "BIG")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Minimum order amount");
        }

        @Test
        void rejectsEmptyCart() {
            when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cartWith()));

            assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(5L, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("empty cart");
        }

        @Test
        void rejectsMissingCart() {
            when(cartRepository.findByUser(customer)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(5L, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void rejectsAddressThatDoesNotBelongToUser() {
            when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cartWith(phone)));
            when(addressRepository.findByIdAndUserAndDeletedFalse(5L, customer)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(5L, null)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Shipping address not found");
        }

        @Test
        void rejectsProductDeletedSinceItWasAddedToCart() {
            Product lockedCopy = product(7L, "Phone", "100.00", 10);
            lockedCopy.setDeleted(true);
            stubCheckout(cartWith(phone), List.of(lockedCopy));

            assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(5L, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("no longer available");
            verify(orderRepository, never()).save(any());
        }

        @Test
        void checksStockAgainstFreshlyLockedRowNotStaleCartCopy() {
            // the cart still thinks there are 10 in stock, but the locked row says 1
            Product lockedCopy = product(7L, "Phone", "100.00", 1);
            stubCheckout(cartWith(phone), List.of(lockedCopy));

            assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(5L, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("1 remaining");
            verify(orderRepository, never()).save(any());
        }

        @Test
        void failsWhenAtomicStockDecrementLosesRace() {
            stubCheckout(cartWith(phone), List.of(phone));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.decrementStockIfAvailable(7L, 2)).thenReturn(0);

            assertThatThrownBy(() -> orderService.placeOrder(new PlaceOrderRequest(5L, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("sold out while processing");
            verify(cartService, never()).clearCart(any());
            verify(emailService, never()).sendOrderConfirmation(any());
        }
    }

    @Nested
    class CancelOrder {

        @Test
        void cancelsPendingOrderAndRestoresStock() {
            Product phone = product(7L, "Phone", "100.00", 3);
            phone.setSalesCount(5);
            Order pending = order(OrderStatus.PENDING, phone, 2);
            when(orderRepository.findByIdAndUser(40L, customer)).thenReturn(Optional.of(pending));
            when(orderRepository.save(pending)).thenReturn(pending);

            OrderDto result = orderService.cancelOrder(40L);

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
            assertThat(phone.getStockQuantity()).isEqualTo(5);
            assertThat(phone.getSalesCount()).isEqualTo(3);
            verify(productRepository).save(phone);
        }

        @Test
        void refusesToCancelShippedOrder() {
            Order shipped = order(OrderStatus.SHIPPED, product(7L, "Phone", "100.00", 3), 1);
            when(orderRepository.findByIdAndUser(40L, customer)).thenReturn(Optional.of(shipped));

            assertThatThrownBy(() -> orderService.cancelOrder(40L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Cannot cancel order once it has been shipped");
            verify(productRepository, never()).save(any());
        }

        @Test
        void cannotCancelSomeoneElsesOrder() {
            when(orderRepository.findByIdAndUser(40L, customer)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(40L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class UpdateOrderStatus {

        private Product phone;

        @BeforeEach
        void setUp() {
            phone = product(7L, "Phone", "100.00", 3);
            phone.setSalesCount(4);
        }

        @Test
        void movesOrderForward() {
            Order pending = order(OrderStatus.PENDING, phone, 1);
            when(orderRepository.findById(40L)).thenReturn(Optional.of(pending));
            when(orderRepository.save(pending)).thenReturn(pending);

            assertThat(orderService.updateOrderStatus(40L, "shipped").getStatus()).isEqualTo("SHIPPED");
        }

        @Test
        void adminCancelRestoresStock() {
            Order confirmed = order(OrderStatus.CONFIRMED, phone, 2);
            when(orderRepository.findById(40L)).thenReturn(Optional.of(confirmed));
            when(orderRepository.save(confirmed)).thenReturn(confirmed);

            orderService.updateOrderStatus(40L, "CANCELLED");

            assertThat(phone.getStockQuantity()).isEqualTo(5);
            assertThat(phone.getSalesCount()).isEqualTo(2);
        }

        @Test
        void rejectsUnknownStatus() {
            when(orderRepository.findById(40L)).thenReturn(Optional.of(order(OrderStatus.PENDING, phone, 1)));

            assertThatThrownBy(() -> orderService.updateOrderStatus(40L, "LOST"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Invalid order status: LOST");
        }

        @Test
        void rejectsMovingBackwards() {
            when(orderRepository.findById(40L)).thenReturn(Optional.of(order(OrderStatus.PACKED, phone, 1)));

            assertThatThrownBy(() -> orderService.updateOrderStatus(40L, "CONFIRMED"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("backwards");
        }

        @Test
        void rejectsChangesToCancelledOrder() {
            when(orderRepository.findById(40L)).thenReturn(Optional.of(order(OrderStatus.CANCELLED, phone, 1)));

            assertThatThrownBy(() -> orderService.updateOrderStatus(40L, "SHIPPED"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cancelled order");
        }

        @Test
        void rejectsChangesToDeliveredOrder() {
            when(orderRepository.findById(40L)).thenReturn(Optional.of(order(OrderStatus.DELIVERED, phone, 1)));

            assertThatThrownBy(() -> orderService.updateOrderStatus(40L, "CANCELLED"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("delivered order");
        }

        @Test
        void rejectsCancellingShippedOrder() {
            when(orderRepository.findById(40L)).thenReturn(Optional.of(order(OrderStatus.SHIPPED, phone, 1)));

            assertThatThrownBy(() -> orderService.updateOrderStatus(40L, "CANCELLED"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already been shipped");
            assertThat(phone.getStockQuantity()).isEqualTo(3);
        }

        @Test
        void rejectsMissingOrder() {
            when(orderRepository.findById(40L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateOrderStatus(40L, "SHIPPED"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    void salesReportExcludesCancelledOrdersFromRevenue() {
        Product phone = product(7L, "Phone", "100.00", 3);
        Order delivered = order(OrderStatus.DELIVERED, phone, 2);
        delivered.setFinalAmount(new BigDecimal("200.00"));
        Order pending = order(OrderStatus.PENDING, phone, 1);
        pending.setFinalAmount(new BigDecimal("100.00"));
        Order cancelled = order(OrderStatus.CANCELLED, phone, 5);
        cancelled.setFinalAmount(new BigDecimal("500.00"));
        when(orderRepository.findAll()).thenReturn(List.of(delivered, pending, cancelled));

        SalesReportDto report = orderService.generateSalesReport();

        assertThat(report.getTotalRevenue()).isEqualByComparingTo("300.00");
        assertThat(report.getTotalOrdersPlaced()).isEqualTo(3);
        assertThat(report.getTotalItemsSold()).isEqualTo(3);
        assertThat(report.getAverageOrderValue()).isEqualByComparingTo("150.00");
        assertThat(report.getOrders()).hasSize(3);
    }

    @Test
    void salesReportHandlesNoOrders() {
        when(orderRepository.findAll()).thenReturn(new ArrayList<>());

        SalesReportDto report = orderService.generateSalesReport();

        assertThat(report.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(report.getAverageOrderValue()).isEqualByComparingTo("0");
    }

    @Test
    void orderHistoryAndDetailsAreScopedToCurrentUser() {
        Order order = order(OrderStatus.PENDING, product(7L, "Phone", "100.00", 3), 1);
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findByUser(customer, pageable)).thenReturn(new PageImpl<>(List.of(order)));
        when(orderRepository.findByIdAndUser(40L, customer)).thenReturn(Optional.of(order));

        Page<OrderDto> history = orderService.getMyOrderHistory(pageable);
        OrderDto details = orderService.getOrderDetails(40L);

        assertThat(history.getContent()).extracting(OrderDto::getOrderNumber).containsExactly("ORD-TEST");
        assertThat(details.getShippingAddress().getCity()).isEqualTo("Noida");
        assertThat(details.getCustomerName()).isEqualTo("Test User");
    }

    @Test
    void adminCanListAllOrdersAndCountPending() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order(OrderStatus.PENDING, product(7L, "Phone", "100.00", 3), 1))));
        when(orderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(4L);

        assertThat(orderService.getAllOrders(pageable).getTotalElements()).isEqualTo(1);
        assertThat(orderService.getPendingOrderCount()).isEqualTo(4L);
    }

    @Test
    void rejectsCallsWithoutAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.AnonymousAuthenticationToken(
                        "key", "anonymousUser", org.springframework.security.core.authority.AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThatThrownBy(() -> orderService.getOrderDetails(40L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Not authenticated");
    }

    @Test
    void savedOrderCarriesSubtotalDiscountAndAddress() {
        Product phone = product(7L, "Phone", "100.00", 10);
        Cart cart = cartWith(phone);
        when(cartRepository.findByUser(customer)).thenReturn(Optional.of(cart));
        when(addressRepository.findByIdAndUserAndDeletedFalse(5L, customer)).thenReturn(Optional.of(address));
        when(productRepository.findAllByIdForUpdate(any())).thenReturn(List.of(phone));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.decrementStockIfAvailable(7L, 2)).thenReturn(1);

        orderService.placeOrder(new PlaceOrderRequest(5L, "   "));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(customer);
        assertThat(saved.getShippingAddress()).isSameAs(address);
        assertThat(saved.getCouponCode()).isNull();
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("200.00");
        verifyNoInteractions(couponRepository);
    }
}
