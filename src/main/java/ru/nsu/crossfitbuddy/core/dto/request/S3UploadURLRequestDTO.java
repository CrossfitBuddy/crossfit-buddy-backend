package ru.nsu.crossfitbuddy.core.dto.request;

import ru.nsu.crossfitbuddy.core.entity.MimoType;

public record S3UploadURLRequestDTO(
        String fileName,
        MimoType contentType,
        Long size) {
}
