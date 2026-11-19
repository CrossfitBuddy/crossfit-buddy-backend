package ru.nsu.crossfitbuddy.core.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.crossfitbuddy.auth.security.annotation.AdminAuthority;
import ru.nsu.crossfitbuddy.core.dto.request.UpdateComplexFormatRequest;
import ru.nsu.crossfitbuddy.core.dto.response.ComplexFormatResponse;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;
import ru.nsu.crossfitbuddy.core.service.ComplexFormatService;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(Paths.COMPLEX)
@RequiredArgsConstructor
public class ComplexController {

    private final ComplexFormatService complexFormatService;

    @GetMapping(Paths.COMPLEX_FORMATS)
    public List<ComplexFormatResponse> getAllComplexFormats() {
        return complexFormatService.getAll();
    }

    @AdminAuthority
    @PatchMapping(Paths.COMPLEX_FORMATS_BY_TYPE)
    public ResponseEntity<ComplexFormatResponse> updateComplexFormatDescription(
            @PathVariable ComplexFormatType type,
            @Valid @RequestBody UpdateComplexFormatRequest request
    ) {
        return ResponseEntity.ok(complexFormatService.updateDescription(type, request.description()));
    }
}
