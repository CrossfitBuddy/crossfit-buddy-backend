package ru.nsu.crossfitbuddy.core.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExerciseEquipmentRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExercisesMetricsRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.GeneralExerciseRequest;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.ExerciseEquipmentResponse;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.ExercisesMetricsResponse;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.GeneralExerciseResponse;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipment;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipmentProperties;
import ru.nsu.crossfitbuddy.core.entity.ExercisesMetrics;
import ru.nsu.crossfitbuddy.core.entity.GeneralExercises;
import ru.nsu.crossfitbuddy.core.exception.exercises.DuplicateEquipmentsNameExeption;
import ru.nsu.crossfitbuddy.core.exception.exercises.DuplicateMetricsNameExeption;
import ru.nsu.crossfitbuddy.core.mapper.ExerciseEquipmentMapper;
import ru.nsu.crossfitbuddy.core.mapper.ExercisesMetricsMapper;
import ru.nsu.crossfitbuddy.core.mapper.GeneralExerciseMapper;
import ru.nsu.crossfitbuddy.core.repository.ExerciseEquipmentRepository;
import ru.nsu.crossfitbuddy.core.repository.ExerciseMetricRepository;
import ru.nsu.crossfitbuddy.core.repository.GeneralExercisesRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneralExercisesService {
    private final GeneralExercisesRepository generalExercisesRepository;
    private final ExerciseEquipmentRepository exerciseEquipmentRepository;
    private final ExerciseMetricRepository exerciseMetricRepository;

    private final GeneralExerciseMapper generalExerciseMapper;
    private final ExerciseEquipmentMapper exerciseEquipmentMapper;
    private final ExercisesMetricsMapper exercisesMetricsMapper;

    private final AvatarStorageService avatarStorageService;

    @Transactional
    public void createExercise(GeneralExerciseRequest request) {
        log.info("Запрос на создание упражнения с названием: {}", request.name());
        requestValidate(request);

        var exercise = new GeneralExercises();
        exerciseSetData(exercise, request);
        handleMediaUpload(exercise, request.image(), request.videoExample());

        generalExercisesRepository.save(exercise);
        log.info("Упражнение успешно создано: {}", exercise.getId());
    }

    @Transactional
    public void updateExercises(GeneralExerciseRequest request) {
        if (request.id() == null) {
            throw new IllegalArgumentException("В запросе обновления упражнения поле id не должно быть null");
        }
        var exercise = generalExercisesRepository.findById(request.id())
            .orElseThrow(() -> new EntityNotFoundException("Упражнение с id - " + request.id() + " не найдено"));

        exerciseSetData(exercise, request);
        handleMediaUpload(exercise, request.image(), request.videoExample());

        log.info("Упражнение успешно обновлено: {}", exercise.getId());
    }

    private void handleMediaUpload(GeneralExercises exercise, MultipartFile imageFile, MultipartFile videoFile) {
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = avatarStorageService.uploadToFolder("exercises/images", imageFile);
            exercise.setImageUrl(imageUrl);
        }

        if (videoFile != null && !videoFile.isEmpty()) {
            String videoUrl = avatarStorageService.uploadToFolder("exercises/videos", videoFile);
            exercise.setVideoUrl(videoUrl);
        }
    }

    @Transactional(readOnly = true)
    public List<GeneralExerciseResponse> getAllExercise() {
        return generalExerciseMapper.toResponseList(generalExercisesRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<ExercisesMetricsResponse> getAllMetrics() {
        return exercisesMetricsMapper.toResponseList(exerciseMetricRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<ExerciseEquipmentResponse> getAllEquipments() {
        return exerciseEquipmentMapper.toResponseList(exerciseEquipmentRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<GeneralExerciseResponse> getExercisesCatalog(String searchName, ExerciseCategory category, UUID equipmentId) {
        Specification<GeneralExercises> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchName != null && !searchName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + searchName.toLowerCase() + "%"));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (equipmentId != null) {
                Join<Object, Object> equipmentJoin = root.join("exerciseEquipments", JoinType.INNER);
                predicates.add(cb.equal(equipmentJoin.get("id"), equipmentId));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.ASC, "name");

        List<GeneralExercises> exercises = generalExercisesRepository.findAll(spec, sort);
        return generalExerciseMapper.toResponseList(exercises);
    }

    @Transactional(readOnly = true)
    public GeneralExerciseResponse getExerciseDetails(UUID id) {
        GeneralExercises exercise = generalExercisesRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new EntityNotFoundException("Упражнение с id " + id + " не найдено"));
        return generalExerciseMapper.toResponse(exercise);
    }


    private void exerciseSetData(GeneralExercises exercises, GeneralExerciseRequest request) {
        exercises.setName(request.name());
        exercises.setDescription(request.description());
        exercises.setCategory(request.category());

        Set<ExercisesMetrics> metrics = request.exercisesMetrics().stream().map(metric -> {
            if (metric.id() != null) {
                return exerciseMetricRepository.findById(metric.id())
                    .orElseThrow(() -> new EntityNotFoundException("Метрика не найдена"));
            }
            var dbMetric = new ExercisesMetrics();
            dbMetric.setName(metric.name());
            dbMetric.setMetricsValueType(metric.metricsValueType());
            dbMetric.setUnit(metric.unit());
            return dbMetric;
        }).collect(Collectors.toSet());

        Set<ExerciseEquipment> equipments = request.exercisesEquipments().stream()
            .map(equipment -> {
                if (equipment.id() != null) {
                    return exerciseEquipmentRepository.findById(equipment.id())
                        .orElseThrow(() -> new EntityNotFoundException("Оборудование с id: " + equipment.id() + " не найдено"));
                }
                var dbEquipment = new ExerciseEquipment();
                dbEquipment.setName(equipment.name());

                var properties = equipment.propertiesRequests().stream().map(property -> {
                    var dbProperty = new ExerciseEquipmentProperties();
                    dbProperty.setMetricsValueType(property.metricsValueType());
                    dbProperty.setName(property.name());
                    dbProperty.setUnit(property.unit());
                    dbProperty.setExerciseEquipment(dbEquipment);
                    return dbProperty;
                }).toList();

                dbEquipment.setEquipmentProperties(properties);
                return dbEquipment;
            }).collect(Collectors.toSet());

        exercises.setExerciseEquipments(equipments);
        exercises.setExercisesMetrics(metrics);
    }

    private void requestValidate(GeneralExerciseRequest request) {
        if (isHaveDuplicateEquipmentsName(request.exercisesEquipments())) {
            log.warn("Названия оборудования дублируются");
            throw new DuplicateEquipmentsNameExeption();
        }
        if (isHaveDuplicateMetricsName(request.exercisesMetrics())) {
            log.warn("Названия метрик дублируются");
            throw new DuplicateMetricsNameExeption();
        }
    }

    private boolean isHaveDuplicateMetricsName(List<ExercisesMetricsRequest> requests) {
        Set<String> duplicateName = requests.stream()
            .collect(Collectors.groupingBy(ExercisesMetricsRequest::name, Collectors.counting()))
            .entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        return !duplicateName.isEmpty();
    }

    private boolean isHaveDuplicateEquipmentsName(List<ExerciseEquipmentRequest> requests) {
        Set<String> duplicateName = requests.stream()
            .collect(Collectors.groupingBy(ExerciseEquipmentRequest::name, Collectors.counting()))
            .entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        return !duplicateName.isEmpty();
    }
}
