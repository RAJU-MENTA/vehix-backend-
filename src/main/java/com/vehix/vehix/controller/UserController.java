package com.vehix.vehix.controller;

import com.vehix.vehix.dto.UserSummary;
import com.vehix.vehix.entity.User;
import com.vehix.vehix.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummary> getCurrentUser(Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(userService.toSummary(currentUser));
    }
}
