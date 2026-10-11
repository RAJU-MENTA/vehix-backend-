package com.vehix.vehix.service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() throws IOException {
        Path path = Path.of(uploadDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String original = file.getOriginalFilename();
        String extension = "";
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf('.'));
        }

        String fileName = UUID.randomUUID() + extension;
        Path targetPath = Path.of(uploadDir, fileName).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store uploaded file", e);
        }
        return "/uploads/" + fileName;
    }

    public void deleteFile(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }

        String normalized = storedPath.startsWith("/") ? storedPath.substring(1) : storedPath;
        Path target = Paths.get(uploadDir, normalized.replace("/uploads/", "")).normalize();
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to delete uploaded file", e);
        }
    }
}
