package com.vehix.vehix;

import com.vehix.vehix.entity.User;
import com.vehix.vehix.entity.UserRole;
import com.vehix.vehix.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = new User("Admin User", "admin@vehix.com", passwordEncoder.encode("admin123"), "+1-555-0100", UserRole.ADMIN);
                userRepository.save(admin);

                User seller = new User("Sam Seller", "seller@vehix.com", passwordEncoder.encode("seller123"), "+1-555-0101", UserRole.SELLER);
                userRepository.save(seller);

                User customer = new User("Jane Customer", "customer@vehix.com", passwordEncoder.encode("customer123"), "+1-555-0102", UserRole.CUSTOMER);
                userRepository.save(customer);
            }
        };
    }
}
