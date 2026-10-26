package com.ecom.platform.controller;

import com.ecom.platform.dto.*;
import com.ecom.platform.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Operations", description = "Endpoints restricted to administrator role (ROLE_ADMIN)")
public class AdminController {

    private final DashboardService dashboardService;
    private final OrderService orderService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final CouponService couponService;
    private final UserService userService;
    private final ImageStorageService imageStorageService;
    private final SupportService supportService;

    // Dashboard Statistics
    @GetMapping("/dashboard")
    @Operation(summary = "Get overview metrics for admin dashboard")
    public ResponseEntity<AdminDashboardDto> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    // Sales Report
    @GetMapping("/sales-report")
    @Operation(summary = "Generate full sales report of all transactions")
    public ResponseEntity<SalesReportDto> generateSalesReport() {
        return ResponseEntity.ok(orderService.generateSalesReport());
    }

    // Customer list
    @GetMapping("/customers")
    @Operation(summary = "List all registered customers")
    public ResponseEntity<List<UserDto>> getCustomers() {
        return ResponseEntity.ok(userService.getAllCustomers());
    }

    @PutMapping("/customers/{id}/status")
    @Operation(summary = "Update customer active status")
    public ResponseEntity<UserDto> setCustomerStatus(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(userService.setUserStatus(id, active));
    }

    // Product CRUD
    @PostMapping("/products")
    @Operation(summary = "Create a new product in the catalog")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/products/{id}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Soft delete a product from catalog")
    public ResponseEntity<Void> softDeleteProduct(@PathVariable Long id) {
        productService.softDeleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/products/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload product image to local storage")
    public ResponseEntity<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = imageStorageService.storeFile(file);
        Map<String, String> response = new HashMap<>();
        response.put("imageUrl", imageUrl);
        return ResponseEntity.ok(response);
    }

    // Category CRUD
    @PostMapping("/categories")
    @Operation(summary = "Create a new product category")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(dto));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update a product category")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryDto dto) {
        return ResponseEntity.ok(categoryService.updateCategory(id, dto));
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // Order Management
    @GetMapping("/orders")
    @Operation(summary = "List all customer orders placed (paginated)")
    public ResponseEntity<Page<OrderDto>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @PutMapping("/orders/{id}/status")
    @Operation(summary = "Update the processing status of a customer order")
    public ResponseEntity<OrderDto> updateOrderStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus()));
    }

    // Coupon CRUD
    @GetMapping("/coupons")
    @Operation(summary = "List all promotional discount coupons")
    public ResponseEntity<List<CouponDto>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @PostMapping("/coupons")
    @Operation(summary = "Create a new discount coupon")
    public ResponseEntity<CouponDto> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(request));
    }

    @PutMapping("/coupons/{id}")
    @Operation(summary = "Update a discount coupon details")
    public ResponseEntity<CouponDto> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponCreateRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(id, request));
    }

    @DeleteMapping("/coupons/{id}")
    @Operation(summary = "Delete a discount coupon")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    // Support Tickets
    @GetMapping("/support")
    @Operation(summary = "List all customer support tickets")
    public ResponseEntity<List<SupportTicketDto>> getAllTickets() {
        return ResponseEntity.ok(supportService.getAllTickets());
    }

    @PutMapping("/support/{id}/resolve")
    @Operation(summary = "Mark a support ticket as resolved")
    public ResponseEntity<SupportTicketDto> resolveTicket(@PathVariable Long id) {
        return ResponseEntity.ok(supportService.resolveTicket(id));
    }

    @PostMapping("/support/{id}/reply")
    @Operation(summary = "Admin reply to a support ticket")
    public ResponseEntity<SupportTicketDto> replyTicket(@PathVariable Long id, @Valid @RequestBody ReplyMessageRequest request) {
        return ResponseEntity.ok(supportService.replyToTicket(id, request, "ADMIN"));
    }

    // Notifications
    @GetMapping("/notifications/summary")
    @Operation(summary = "Get notification summary for admin dashboard")
    public ResponseEntity<Map<String, Long>> getNotificationSummary() {
        Map<String, Long> summary = new HashMap<>();
        summary.put("pendingOrders", orderService.getPendingOrderCount());
        summary.put("openTickets", supportService.getOpenTicketCount());
        return ResponseEntity.ok(summary);
    }
}
