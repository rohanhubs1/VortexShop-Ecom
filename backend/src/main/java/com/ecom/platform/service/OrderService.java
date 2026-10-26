package com.ecom.platform.service;

import com.ecom.platform.dto.*;
import com.ecom.platform.entity.*;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final jakarta.persistence.EntityManager entityManager;
    
    private final CartService cartService;
    private final EmailService emailService;
    private final CacheManager cacheManager;

    private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
        throw new BadRequestException("Not authenticated");
    }

    @Transactional
    public OrderDto placeOrder(PlaceOrderRequest request) {
        User user = getAuthenticatedUser();
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place order with an empty cart");
        }

        Address address = addressRepository.findByIdAndUserAndDeletedFalse(request.getShippingAddressId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));

        // Extract and sort Product IDs to prevent MySQL deadlocks during concurrent checkouts
        List<Long> productIds = cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .sorted()
                .collect(Collectors.toList());

        // Acquire Pessimistic Write locks on all products in strict ID order
        List<Product> lockedProducts = productRepository.findAllByIdForUpdate(productIds);
        // Force Hibernate to clear the L1 cache for these entities and reload the freshly locked data
        lockedProducts.forEach(entityManager::refresh);

        java.util.Map<Long, Product> productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // Validate stock and active status for all items using the freshly locked DB state
        for (CartItem item : cart.getItems()) {
            Product freshProduct = productMap.get(item.getProduct().getId());
            if (freshProduct == null || freshProduct.isDeleted()) {
                throw new BadRequestException("Product " + item.getProduct().getName() + " is no longer available for purchase.");
            }
            if (freshProduct.getStockQuantity() < item.getQuantity()) {
                throw new BadRequestException("Product " + freshProduct.getName() + " is out of stock or does not have sufficient quantity (" + freshProduct.getStockQuantity() + " remaining).");
            }
            // Update the cart item to reference the locked, fresh product for later calculations
            item.setProduct(freshProduct);
        }

        // Calculate totals
        BigDecimal subTotal = cart.getItems().stream()
                .map(item -> item.getProduct().getFinalPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        String appliedCouponCode = null;

        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            Coupon coupon = couponRepository.findByCode(request.getCouponCode().toUpperCase())
                    .orElseThrow(() -> new BadRequestException("Invalid coupon code"));

            if (!coupon.isValid()) {
                throw new BadRequestException("This coupon has expired or is inactive");
            }
            
            // Check if user has already used this coupon
            boolean alreadyUsed = orderRepository.findByUser(user).stream()
                    .anyMatch(o -> o.getStatus() != OrderStatus.CANCELLED && 
                                   coupon.getCode().equals(o.getCouponCode()));
            if (alreadyUsed) {
                throw new BadRequestException("Coupon already claimed");
            }

            if (subTotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
                throw new BadRequestException("Minimum order amount of $" + coupon.getMinimumOrderAmount() + " not met for coupon " + coupon.getCode());
            }

            BigDecimal discountCalc = subTotal.multiply(BigDecimal.valueOf(coupon.getDiscountPercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            discount = discountCalc.min(coupon.getMaxDiscount());
            appliedCouponCode = coupon.getCode();
        }

        BigDecimal finalAmount = subTotal.subtract(discount);

        // Generate a clean order number
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = Order.builder()
                .user(user)
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING)
                .totalAmount(subTotal)
                .discountAmount(discount)
                .finalAmount(finalAmount)
                .shippingAddress(address)
                .couponCode(appliedCouponCode)
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        // Save items & decrement stock & increment sales count
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            
            // ATOMIC DATABASE UPDATE - 100% immune to concurrent overwrites
            int updatedRows = productRepository.decrementStockIfAvailable(product.getId(), item.getQuantity());
            if (updatedRows == 0) {
                // If it returns 0, another concurrent transaction bought it milliseconds before us!
                throw new BadRequestException("Product " + product.getName() + " sold out while processing your order! Please try again.");
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .product(product)
                    .quantity(item.getQuantity())
                    .price(product.getFinalPrice())
                    .build();

            orderItems.add(orderItemRepository.save(orderItem));
        }

        savedOrder.setItems(orderItems);

        // Clear user cart
        cartService.clearCart(cart);

        // Send Email notification asynchronously (mocked JavaMailSender delivery to Mailpit)
        emailService.sendOrderConfirmation(savedOrder);

        // Evict caches
        if (cacheManager.getCache("products") != null) {
            cacheManager.getCache("products").clear();
        }
        if (cacheManager.getCache("productSearch") != null) {
            cacheManager.getCache("productSearch").clear();
        }

        return convertToDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> getMyOrderHistory(Pageable pageable) {
        User user = getAuthenticatedUser();
        return orderRepository.findByUser(user, pageable).map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderDetails(Long id) {
        User user = getAuthenticatedUser();
        Order order = orderRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return convertToDto(order);
    }

    @Transactional
    public OrderDto cancelOrder(Long id) {
        User user = getAuthenticatedUser();
        Order order = orderRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot cancel order once it has been " + order.getStatus().name().toLowerCase());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        // Restore stock & decrement sales count
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            product.setSalesCount(product.getSalesCount() - item.getQuantity());
            productRepository.save(product);
        }

        // Evict caches
        if (cacheManager.getCache("products") != null) {
            cacheManager.getCache("products").clear();
        }
        if (cacheManager.getCache("productSearch") != null) {
            cacheManager.getCache("productSearch").clear();
        }

        return convertToDto(saved);
    }

    // Admin Operations
    @Transactional(readOnly = true)
    public Page<OrderDto> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::convertToDto);
    }

    @Transactional
    public OrderDto updateOrderStatus(Long id, String statusStr) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status: " + statusStr);
        }

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot update status of a cancelled order");
        }

        if (currentStatus == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot update status of a delivered order");
        }

        if (newStatus == OrderStatus.CANCELLED) {
            if (currentStatus == OrderStatus.SHIPPED) {
                throw new BadRequestException("Cannot cancel an order that has already been shipped");
            }
            // Restore product stock & decrement sales count
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                product.setSalesCount(product.getSalesCount() - item.getQuantity());
                productRepository.save(product);
            }
        } else {
            if (newStatus.ordinal() <= currentStatus.ordinal()) {
                throw new BadRequestException("Cannot move order status backwards or to the same status");
            }
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        return convertToDto(updated);
    }

    @Transactional(readOnly = true)
    public SalesReportDto generateSalesReport() {
        List<Order> orders = orderRepository.findAll();
        List<Order> successfulOrders = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());

        BigDecimal totalRevenue = successfulOrders.stream()
                .map(Order::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalItemsSold = successfulOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .mapToLong(OrderItem::getQuantity)
                .sum();

        BigDecimal averageOrderValue = successfulOrders.isEmpty() ? BigDecimal.ZERO :
                totalRevenue.divide(BigDecimal.valueOf(successfulOrders.size()), 2, RoundingMode.HALF_UP);

        List<OrderDto> orderDtos = orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return SalesReportDto.builder()
                .totalRevenue(totalRevenue)
                .totalOrdersPlaced(orders.size())
                .totalItemsSold(totalItemsSold)
                .averageOrderValue(averageOrderValue)
                .orders(orderDtos)
                .build();
    }

    private OrderDto convertToDto(Order order) {
        Address shipping = order.getShippingAddress();
        AddressDto shippingDto = AddressDto.builder()
                .id(shipping.getId())
                .title(shipping.getTitle())
                .streetAddress(shipping.getStreetAddress())
                .city(shipping.getCity())
                .state(shipping.getState())
                .zipCode(shipping.getZipCode())
                .country(shipping.getCountry())
                .isDefault(shipping.isDefault())
                .build();

        List<OrderItemDto> items = order.getItems().stream().map(item -> 
            OrderItemDto.builder()
                    .id(item.getId())
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .productSlug(item.getProduct().getSlug())
                    .productImageUrl(item.getProduct().getImageUrl())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .build()
        ).collect(Collectors.toList());

        return OrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .shippingAddress(shippingDto)
                .couponCode(order.getCouponCode())
                .items(items)
                .createdAt(order.getCreatedAt())
                .customerName(order.getUser().getFirstName() + " " + order.getUser().getLastName())
                .customerEmail(order.getUser().getEmail())
                .build();
    }

    public long getPendingOrderCount() {
        return orderRepository.countByStatus(OrderStatus.PENDING);
    }
}
