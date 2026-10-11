package com.vehix.vehix.repository;

import com.vehix.vehix.entity.SupportTicket;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findAllByOrderByCreatedAtDesc();
}
