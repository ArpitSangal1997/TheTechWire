package com.wireblog.controller;

import com.wireblog.exception.ApiException;
import com.wireblog.service.CurrentUserResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Arrays;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    private final CurrentUserResolver currentUserResolver;

    public UploadController(CurrentUserResolver currentUserResolver) { this.currentUserResolver = currentUserResolver; }

    @PostMapping
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        currentUserResolver.requireCurrentUser();
        if (file.isEmpty()) {
            throw ApiException.badRequest("Choose an image to upload.");
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw ApiException.badRequest("Only JPEG, PNG, GIF, and WebP images are supported.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw ApiException.badRequest("Images must be 5 MB or smaller.");
        }
        byte[] header = file.getBytes();
        if (!matchesSignature(contentType, header)) throw ApiException.badRequest("The uploaded file does not match its image type.");

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String fileName = UUID.randomUUID() + extensionFor(contentType);
        Path target = dir.resolve(fileName).normalize();
        file.transferTo(target);
        return ResponseEntity.ok(Map.of("url", baseUrl + "/" + fileName));
    }

    private boolean matchesSignature(String contentType, byte[] bytes) {
        if (contentType.equals("image/jpeg")) return bytes.length > 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8;
        if (contentType.equals("image/png")) return bytes.length > 8 && Arrays.equals(Arrays.copyOf(bytes, 8), new byte[]{(byte)137,80,78,71,13,10,26,10});
        if (contentType.equals("image/gif")) return bytes.length > 6 && new String(bytes, 0, 6).matches("GIF8[79]a");
        return bytes.length > 12 && new String(bytes, 0, 4).equals("RIFF") && new String(bytes, 8, 4).equals("WEBP");
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> throw ApiException.badRequest("Unsupported image type.");
        };
    }
}
