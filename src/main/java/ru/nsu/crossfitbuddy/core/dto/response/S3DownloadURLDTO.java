package ru.nsu.crossfitbuddy.core.dto.response;

import java.time.Instant;
import java.util.UUID;

public record S3DownloadURLDTO(
        UUID fileId,
        String downloadUrl,
        Instant expiresAt) {
}
