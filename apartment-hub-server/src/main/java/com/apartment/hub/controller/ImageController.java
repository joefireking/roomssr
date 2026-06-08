package com.apartment.hub.controller;

import com.apartment.hub.service.MinioService;
import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final MinioService minioService;

    public ImageController(MinioService minioService) {
        this.minioService = minioService;
    }

    @GetMapping("/{objectPath:.*}")
    public void getImage(@PathVariable String objectPath, HttpServletResponse response) {
        try {
            StatObjectResponse stat = minioService.statObject(objectPath);
            response.setContentType(stat.contentType());
            response.setContentLengthLong(stat.size());
            response.setHeader("Cache-Control", "public, max-age=86400");

            try (InputStream is = minioService.getObject(objectPath);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            }
        } catch (Exception e) {
            response.setStatus(404);
        }
    }
}
