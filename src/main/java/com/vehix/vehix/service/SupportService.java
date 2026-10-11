package com.vehix.vehix.service;

import com.vehix.vehix.dto.SupportContactRequest;
import com.vehix.vehix.entity.SupportTicket;
import com.vehix.vehix.exception.ApiException;
import com.vehix.vehix.repository.SupportTicketRepository;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SupportService {

    private final SupportTicketRepository supportTicketRepository;

    public SupportService(SupportTicketRepository supportTicketRepository) {
        this.supportTicketRepository = supportTicketRepository;
    }

    public SupportTicket createTicket(SupportContactRequest request) {
        SupportTicket ticket = new SupportTicket();
        ticket.setFullName(request.getFullName().trim());
        ticket.setEmail(request.getEmail().trim());
        ticket.setTopic(request.getTopic().trim());
        ticket.setSubject(request.getSubject().trim());
        ticket.setMessage(request.getMessage().trim());
        return supportTicketRepository.save(ticket);
    }

    public List<SupportTicket> getTickets() {
        return supportTicketRepository.findAllByOrderByCreatedAtDesc();
    }

    public SupportTicket updateStatus(Long ticketId, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!Set.of("OPEN", "IN_PROGRESS", "RESOLVED").contains(normalizedStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported support ticket status");
        }
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Support ticket not found"));
        ticket.setStatus(normalizedStatus);
        return supportTicketRepository.save(ticket);
    }
}
