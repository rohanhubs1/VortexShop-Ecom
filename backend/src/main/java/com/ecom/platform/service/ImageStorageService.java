package com.ecom.platform.service;

import com.ecom.platform.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class ImageStorageService {

    private final Path fileStorageLocation;

    public ImageStorageService() {
        // Store images in scratch/ecom-platform/backend/uploads
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            log.error("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file.");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new BadRequestException("Invalid file name.");
        }

        String fileExtension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            fileExtension = originalFileName.substring(i);
        }

        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // Check if the filename contains invalid characters
            if (fileName.contains("..")) {
                throw new BadRequestException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File successfully uploaded: {}", fileName);
            return "/api/products/images/" + fileName;
        } catch (IOException ex) {
            throw new BadRequestException("Could not store file " + fileName + ". Please try again!");
        }
    }

    public byte[] loadFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (Files.exists(filePath)) {
                return Files.readAllBytes(filePath);
            } else {
                throw new BadRequestException("File not found: " + fileName);
            }
        } catch (IOException ex) {
            throw new BadRequestException("File read error: " + fileName);
        }
    }
}
