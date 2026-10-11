package com.vehix.vehix.service;

import com.vehix.vehix.dto.UserSummary;
import com.vehix.vehix.entity.User;
import com.vehix.vehix.entity.UserRole;
import com.vehix.vehix.exception.ApiException;
import com.vehix.vehix.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserSummary toSummary(User user) {
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name());
    }

    public boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }
}
