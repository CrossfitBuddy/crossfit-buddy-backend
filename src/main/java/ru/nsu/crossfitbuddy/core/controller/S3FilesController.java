package ru.nsu.crossfitbuddy.core.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ru.nsu.crossfitbuddy.core.dto.request.S3UploadURLRequestDTO;
import ru.nsu.crossfitbuddy.core.dto.response.S3DownloadURLDTO;
import ru.nsu.crossfitbuddy.core.dto.response.S3UploadURLDTO;
import ru.nsu.crossfitbuddy.core.service.S3FilesService;

@RestController
@RequestMapping(Paths.FILES)
@RequiredArgsConstructor
public class S3FilesController {
    private final S3FilesService s3FilesService;

    @PostMapping(Paths.FILES_UPPLOAD)
    public S3UploadURLDTO pendingRequest(@RequestBody S3UploadURLRequestDTO request) {
        return s3FilesService.requestPending(request);

    }

    @GetMapping(Paths.FILES_DOWNLOAD)
    public S3DownloadURLDTO getDownloadLink(@PathVariable("fileId") UUID fileId) {
        return s3FilesService.getDownloadLink(fileId);
    }

}
