package ru.nsu.crossfitbuddy.core.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record S3UploadURLDTO(
        UUID fileId,
        String uploadUrl,
        Map<String, String> headers,
        LocalDateTime expiresAt) {
}
