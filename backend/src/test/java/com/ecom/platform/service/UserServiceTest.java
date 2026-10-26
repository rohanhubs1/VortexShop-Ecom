package com.ecom.platform.service;

import com.ecom.platform.dto.AddressDto;
import com.ecom.platform.dto.RegisterRequest;
import com.ecom.platform.dto.UserDto;
import com.ecom.platform.entity.Address;
import com.ecom.platform.entity.RoleName;
import com.ecom.platform.entity.User;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.repository.AddressRepository;
import com.ecom.platform.repository.OrderRepository;
import com.ecom.platform.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ecom.platform.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private AddressRepository addressRepository;

    @InjectMocks
    private UserService userService;

    private User customer;

    @BeforeEach
    void setUp() {
        customer = customer(1L, "rohan@example.com");
        authenticateAs(customer);
        lenient().when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        lenient().when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private AddressDto addressDto(boolean isDefault) {
        return AddressDto.builder().title("Office").streetAddress("2 Park Lane").city("Delhi")
                .state("DL").zipCode("110001").country("India").isDefault(isDefault).build();
    }

    @Test
    void updateProfileChangesNameAndPhoneOnly() {
        when(userRepository.save(customer)).thenReturn(customer);

        UserDto dto = userService.updateProfile(new RegisterRequest("ignored@example.com", "ignored", "Rohan", "Agarwal", "123"));

        assertThat(dto.getFirstName()).isEqualTo("Rohan");
        assertThat(dto.getPhone()).isEqualTo("123");
        assertThat(customer.getEmail()).isEqualTo("rohan@example.com");
        assertThat(customer.getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void getAllCustomersExcludesAdmins() {
        when(userRepository.findAll()).thenReturn(List.of(customer, user(2L, "admin@example.com", RoleName.ROLE_ADMIN)));

        assertThat(userService.getAllCustomers()).extracting(UserDto::getEmail).containsExactly("rohan@example.com");
    }

    @Test
    void firstAddressBecomesDefaultAutomatically() {
        when(addressRepository.findByUserAndDeletedFalse(customer)).thenReturn(new ArrayList<>());

        AddressDto dto = userService.addAddress(addressDto(false));

        assertThat(dto.isDefault()).isTrue();
    }

    @Test
    void newDefaultAddressClearsPreviousDefault() {
        Address oldDefault = address(5L, customer);
        oldDefault.setDefault(true);
        when(addressRepository.findByUserAndDeletedFalse(customer)).thenReturn(List.of(oldDefault));

        AddressDto dto = userService.addAddress(addressDto(true));

        assertThat(dto.isDefault()).isTrue();
        assertThat(oldDefault.isDefault()).isFalse();
    }

    @Test
    void nonDefaultAddressLeavesExistingDefaultAlone() {
        Address oldDefault = address(5L, customer);
        oldDefault.setDefault(true);
        when(addressRepository.findByUserAndDeletedFalse(customer)).thenReturn(List.of(oldDefault));

        assertThat(userService.addAddress(addressDto(false)).isDefault()).isFalse();
        assertThat(oldDefault.isDefault()).isTrue();
    }

    @Test
    void listsAddresses() {
        when(addressRepository.findByUserAndDeletedFalse(customer)).thenReturn(List.of(address(5L, customer)));

        assertThat(userService.getAddresses()).extracting(AddressDto::getCity).containsExactly("Noida");
    }

    @Test
    void updateAddressCanPromoteToDefault() {
        Address home = address(5L, customer);
        Address office = address(6L, customer);
        office.setDefault(true);
        when(addressRepository.findByIdAndUserAndDeletedFalse(5L, customer)).thenReturn(Optional.of(home));
        when(addressRepository.findByUserAndDeletedFalse(customer)).thenReturn(List.of(home, office));

        AddressDto dto = userService.updateAddress(5L, addressDto(true));

        assertThat(dto.getCity()).isEqualTo("Delhi");
        assertThat(home.isDefault()).isTrue();
        assertThat(office.isDefault()).isFalse();
    }

    @Test
    void updateAddressRejectsOtherUsersAddress() {
        when(addressRepository.findByIdAndUserAndDeletedFalse(5L, customer)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateAddress(5L, addressDto(false))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletingDefaultAddressPromotesAnother() {
        Address home = address(5L, customer);
        home.setDefault(true);
        Address office = address(6L, customer);
        when(addressRepository.findByIdAndUserAndDeletedFalse(5L, customer)).thenReturn(Optional.of(home));
        when(addressRepository.findByUserAndDeletedFalse(customer)).thenReturn(List.of(office));

        userService.deleteAddress(5L);

        assertThat(home.isDeleted()).isTrue();
        assertThat(home.isDefault()).isFalse();
        assertThat(office.isDefault()).isTrue();
    }

    @Test
    void deletingNonDefaultAddressIsSoftDelete() {
        Address office = address(6L, customer);
        when(addressRepository.findByIdAndUserAndDeletedFalse(6L, customer)).thenReturn(Optional.of(office));

        userService.deleteAddress(6L);

        assertThat(office.isDeleted()).isTrue();
        verify(addressRepository, never()).findByUserAndDeletedFalse(any());
    }

    @Test
    void deactivateAccountBlockedWhileOrdersAreActive() {
        when(orderRepository.existsByUserAndStatusIn(any(), anyList())).thenReturn(true);

        assertThatThrownBy(() -> userService.deactivateMyAccount())
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active orders");
        assertThat(customer.isActive()).isTrue();
    }

    @Test
    void deactivateAccountDisablesLogin() {
        when(orderRepository.existsByUserAndStatusIn(any(), anyList())).thenReturn(false);

        userService.deactivateMyAccount();

        assertThat(customer.isActive()).isFalse();
        assertThat(customer.isEnabled()).isFalse();
        verify(userRepository).save(customer);
    }

    @Test
    void adminCanToggleUserStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(userRepository.save(customer)).thenReturn(customer);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThat(userService.setUserStatus(1L, false).isActive()).isFalse();
        assertThatThrownBy(() -> userService.setUserStatus(2L, true)).isInstanceOf(ResourceNotFoundException.class);
    }
}
