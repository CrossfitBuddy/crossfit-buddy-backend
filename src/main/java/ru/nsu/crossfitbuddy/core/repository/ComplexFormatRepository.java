package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormat;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;

public interface ComplexFormatRepository extends JpaRepository<ComplexFormat, ComplexFormatType> {}