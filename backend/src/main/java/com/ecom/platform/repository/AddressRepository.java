package com.ecom.platform.repository;

import com.ecom.platform.entity.Address;
import com.ecom.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserAndDeletedFalse(User user);
    Optional<Address> findByIdAndUserAndDeletedFalse(Long id, User user);
}
