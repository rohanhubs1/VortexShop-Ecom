package com.ecom.platform.repository;

import com.ecom.platform.entity.SupportTicket;
import com.ecom.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserOrderByCreatedAtDesc(User user);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
