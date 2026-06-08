package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.Result;
import com.apartment.hub.service.MinioService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    private final MinioService minioService;

    public UploadController(MinioService minioService) {
        this.minioService = minioService;
    }

    @OperationLog(module = "File Management", operation = "Upload File")
    @PostMapping
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.fail("File size exceeds 5MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail("Only image files are allowed");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!ALLOWED_EXT.contains(ext)) {
            return Result.fail("Unsupported file type, allowed: jpg, png, gif, webp");
        }

        try {
            String objectName = minioService.upload(file, "rooms");
            String url = "/api/images/" + objectName;
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            data.put("name", originalName);
            return Result.success(data);
        } catch (Exception e) {
            return Result.fail("Upload failed: " + e.getMessage());
        }
    }
}
