package com.ecom.platform.service;

import com.ecom.platform.dto.AddressDto;
import com.ecom.platform.dto.UserDto;
import com.ecom.platform.dto.RegisterRequest;
import com.ecom.platform.entity.Address;
import com.ecom.platform.entity.User;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.AddressRepository;
import com.ecom.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final com.ecom.platform.repository.OrderRepository orderRepository;
    private final AddressRepository addressRepository;

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
    public UserDto updateProfile(RegisterRequest request) {
        User user = getAuthenticatedUser();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        User updated = userRepository.save(user);
        return convertToUserDto(updated);
    }

    public List<UserDto> getAllCustomers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_CUSTOMER")))
                .map(this::convertToUserDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AddressDto> getAddresses() {
        User user = getAuthenticatedUser();
        return addressRepository.findByUserAndDeletedFalse(user).stream()
                .map(this::convertToAddressDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressDto addAddress(AddressDto dto) {
        User user = getAuthenticatedUser();

        List<Address> currentAddresses = addressRepository.findByUserAndDeletedFalse(user);
        boolean isFirst = currentAddresses.isEmpty();

        Address address = Address.builder()
                .user(user)
                .title(dto.getTitle())
                .streetAddress(dto.getStreetAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .zipCode(dto.getZipCode())
                .country(dto.getCountry())
                .isDefault(isFirst || dto.isDefault())
                .build();

        if (address.isDefault()) {
            resetDefaultAddresses(user);
        }

        Address saved = addressRepository.save(address);
        return convertToAddressDto(saved);
    }

    @Transactional
    public AddressDto updateAddress(Long addressId, AddressDto dto) {
        User user = getAuthenticatedUser();
        Address address = addressRepository.findByIdAndUserAndDeletedFalse(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found for this user."));

        address.setTitle(dto.getTitle());
        address.setStreetAddress(dto.getStreetAddress());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipCode(dto.getZipCode());
        address.setCountry(dto.getCountry());

        if (dto.isDefault() && !address.isDefault()) {
            resetDefaultAddresses(user);
            address.setDefault(true);
        }

        Address updated = addressRepository.save(address);
        return convertToAddressDto(updated);
    }

    @Transactional
    public void deleteAddress(Long addressId) {
        User user = getAuthenticatedUser();
        Address address = addressRepository.findByIdAndUserAndDeletedFalse(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found for this user."));

        address.setDeleted(true);
        addressRepository.save(address);

        // If the deleted address was default, set another address as default
        if (address.isDefault()) {
            address.setDefault(false);
            List<Address> remaining = addressRepository.findByUserAndDeletedFalse(user);
            if (!remaining.isEmpty()) {
                Address first = remaining.get(0);
                first.setDefault(true);
                addressRepository.save(first);
            }
        }
    }

    private void resetDefaultAddresses(User user) {
        List<Address> addresses = addressRepository.findByUserAndDeletedFalse(user);
        for (Address addr : addresses) {
            if (addr.isDefault()) {
                addr.setDefault(false);
                addressRepository.save(addr);
            }
        }
    }

    private AddressDto convertToAddressDto(Address address) {
        return AddressDto.builder()
                .id(address.getId())
                .title(address.getTitle())
                .streetAddress(address.getStreetAddress())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .isDefault(address.isDefault())
                .build();
    }

    @Transactional
    public void deactivateMyAccount() {
        User user = getAuthenticatedUser();
        boolean hasActiveOrders = orderRepository.existsByUserAndStatusIn(user, java.util.Arrays.asList(com.ecom.platform.entity.OrderStatus.PENDING, com.ecom.platform.entity.OrderStatus.CONFIRMED, com.ecom.platform.entity.OrderStatus.PACKED, com.ecom.platform.entity.OrderStatus.SHIPPED));
        if (hasActiveOrders) {
            throw new BadRequestException("Cannot deactivate account while there are active orders.");
        }
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public UserDto setUserStatus(Long id, boolean active) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(active);
        return convertToUserDto(userRepository.save(user));
    }

    private UserDto convertToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toList()))
                .isVerified(user.isVerified()).active(user.isActive())
                .build();
    }
}
