package com.vehix.vehix.controller;

import com.vehix.vehix.dto.SupportContactRequest;
import com.vehix.vehix.entity.SupportTicket;
import com.vehix.vehix.service.SupportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
@CrossOrigin(origins = "*")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping("/contact")
    public ResponseEntity<SupportTicket> createTicket(@Valid @RequestBody SupportContactRequest request) {
        return ResponseEntity.status(201).body(supportService.createTicket(request));
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SupportTicket> getTickets() {
        return supportService.getTickets();
    }

    @PutMapping("/tickets/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public SupportTicket updateTicketStatus(@PathVariable Long id, @RequestParam String status) {
        return supportService.updateStatus(id, status);
    }
}
