package com.ecom.platform.repository;

import com.ecom.platform.entity.SupportMessage;
import com.ecom.platform.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findByTicketOrderByCreatedAtAsc(SupportTicket ticket);
}
