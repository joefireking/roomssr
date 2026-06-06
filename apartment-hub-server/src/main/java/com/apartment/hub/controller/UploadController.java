package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");

    @Value("${upload.path:uploads}")
    private String uploadPath;

    @OperationLog(module = "File Management", operation = "Upload File")
    @PostMapping
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
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

        String filename = UUID.randomUUID() + ext;

        Path baseDir = Paths.get(uploadPath);
        if (!baseDir.isAbsolute()) {
            baseDir = Paths.get(System.getProperty("user.dir")).resolve(uploadPath);
        }
        Path dir = baseDir.resolve("rooms");
        Files.createDirectories(dir);
        file.transferTo(dir.resolve(filename).toFile());

        String url = "/uploads/rooms/" + filename;

        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        data.put("name", originalName);
        return Result.success(data);
    }
}
