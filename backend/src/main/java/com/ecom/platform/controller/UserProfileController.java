package com.ecom.platform.controller;

import com.ecom.platform.dto.AddressDto;
import com.ecom.platform.dto.RegisterRequest;
import com.ecom.platform.dto.UserDto;
import com.ecom.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for editing user details and managing address books")
public class UserProfileController {

    private final UserService userService;

    @PutMapping
    @Operation(summary = "Update logged-in user profile details")
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PutMapping("/deactivate")
    @Operation(summary = "Deactivate own account")
    public ResponseEntity<Void> deactivateMyAccount() {
        userService.deactivateMyAccount();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/addresses")
    @Operation(summary = "List all saved addresses for current user")
    public ResponseEntity<List<AddressDto>> getAddresses() {
        return ResponseEntity.ok(userService.getAddresses());
    }

    @PostMapping("/addresses")
    @Operation(summary = "Add a new address to user address book")
    public ResponseEntity<AddressDto> addAddress(@Valid @RequestBody AddressDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addAddress(dto));
    }

    @PutMapping("/addresses/{id}")
    @Operation(summary = "Update an existing address in user address book")
    public ResponseEntity<AddressDto> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressDto dto) {
        return ResponseEntity.ok(userService.updateAddress(id, dto));
    }

    @DeleteMapping("/addresses/{id}")
    @Operation(summary = "Delete an address from user address book")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        userService.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }
}
