package ru.nsu.crossfitbuddy.core.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.EquipmentPropertiesRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExerciseEquipmentRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExercisesMetricsRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.GeneralExerciseRequest;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.ExerciseEquipmentResponse;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.ExercisesMetricsResponse;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.GeneralExerciseResponse;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipment;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipmentProperties;
import ru.nsu.crossfitbuddy.core.entity.ExercisesMetrics;
import ru.nsu.crossfitbuddy.core.entity.GeneralExercises;
import ru.nsu.crossfitbuddy.core.entity.S3File;
import ru.nsu.crossfitbuddy.core.exception.exercises.DuplicateEquipmentsNameExeption;
import ru.nsu.crossfitbuddy.core.exception.exercises.DuplicateMetricsNameExeption;
import ru.nsu.crossfitbuddy.core.mapper.ExerciseEquipmentMapper;
import ru.nsu.crossfitbuddy.core.mapper.ExercisesMetricsMapper;
import ru.nsu.crossfitbuddy.core.mapper.GeneralExerciseMapper;
import ru.nsu.crossfitbuddy.core.repository.ExerciseEquipmentRepository;
import ru.nsu.crossfitbuddy.core.repository.ExerciseMetricRepository;
import ru.nsu.crossfitbuddy.core.repository.GeneralExercisesRepository;

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

    private final S3FilesService s3FilesService;

    @Transactional
    public void createExercise(GeneralExerciseRequest request) {

        log.info("Запрос на сохранения упражнения с названием " + request.name());

        requestValidate(request);

        log.warn("Валидация создания упражнения успешна " + request.name());

        var exercises = new GeneralExercises();

        exerciseSetData(exercises, request);

        exercises = generalExercisesRepository.save(exercises);
    }

    @Transactional
    public void updateExercises(GeneralExerciseRequest request) {
        if (request.id() == null) {
            throw new IllegalArgumentException(
                    "In exercises update request filde id must not be null");
        }
        exerciseUpdate(request);
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

    private void exerciseUpdate(GeneralExerciseRequest request) {

        var exercise = generalExercisesRepository.findById(request.id())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Exercise entity with id - " + request.id() + " not found"));

        var newMetricId = request.exercisesMetrics().stream()
                .map(metric -> metric.id())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        exercise.getExercisesMetrics().removeIf(metric -> !newMetricId.contains(metric.getId()));

        var newEquipmentId = request.exercisesEquipments().stream()
                .map(metric -> metric.id())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        exercise.getExercisesMetrics().removeIf(metric -> !newEquipmentId.contains(metric.getId()));

        List<ExercisesMetrics> metrics = request.exercisesMetrics().stream().map(metric -> {
            if (metric.id() != null) {
                var dbMetric = exerciseMetricRepository.findById(metric.id())
                        .orElseThrow(() -> new EntityNotFoundException("Метрика не найдена"));
                return setMetricsData(dbMetric, metric);
            }
            var dbMetric = new ExercisesMetrics();
            return setMetricsData(dbMetric, metric);
        }).toList();

        List<ExerciseEquipment> equipments = request.exercisesEquipments().stream()
                .map(equipment -> {
                    if (equipment.id() != null) {
                        var dbEquipment = exerciseEquipmentRepository.findById(equipment.id())
                                .orElseThrow(
                                        () -> new EntityNotFoundException("Оборудование с id: "
                                                + equipment.id() + " не найдено"));
                        dbEquipment.getEquipmentProperties().clear();
                        return updateEquipmentsData(dbEquipment, equipment);
                    }
                    var dbEquipment = new ExerciseEquipment();
                    return updateEquipmentsData(dbEquipment, equipment);
                }).toList();

        exercise.setName(request.name());
        exercise.setDescription(request.description());
        exercise.setCategory(request.category());
        exercise.setImage(getMedia(request.imageFileId()));
        exercise.setVideo(getMedia(request.videoFileId()));

        exercise.setExerciseEquipments(equipments);
        exercise.setExercisesMetrics(metrics);
    }

    private void exerciseSetData(GeneralExercises exercises, GeneralExerciseRequest request) {
        exercises.setName(request.name());
        exercises.setDescription(request.description());
        exercises.setCategory(request.category());
        exercises.setImage(getMedia(request.imageFileId()));
        exercises.setVideo(getMedia(request.videoFileId()));

        List<ExercisesMetrics> metrics = request.exercisesMetrics().stream().map(metric -> {
            if (metric.id() != null) {
                return exerciseMetricRepository.findById(metric.id())
                        .orElseThrow(() -> new EntityNotFoundException("Метрика не найдена"));
            }
            var dbMetric = new ExercisesMetrics();
            return setMetricsData(dbMetric, metric);
        }).toList();

        List<ExerciseEquipment> equipments = request.exercisesEquipments().stream()
                .map(equipment -> {
                    if (equipment.id() != null) {
                        return exerciseEquipmentRepository.findById(equipment.id())
                                .orElseThrow(
                                        () -> new EntityNotFoundException("Оборудование с id: "
                                                + equipment.id() + " не найдено"));
                    }
                    var dbEquipment = new ExerciseEquipment();
                    return setEquipmentsData(dbEquipment, equipment);
                }).toList();

        exercises.setExerciseEquipments(equipments);
        exercises.setExercisesMetrics(metrics);
    }

    private void requestValidate(GeneralExerciseRequest request) {
        if (isHaveDuplicateEquipmentsName(request.exercisesEquipments())) {
            log.warn("Названия упражнений дублируются");
            throw new DuplicateEquipmentsNameExeption();
        }
        if (isHaveDuplicateMetricsName(request.exercisesMetrics())) {
            log.warn("Названия метрик дублируются");
            throw new DuplicateMetricsNameExeption();
        }
    }

    private S3File getMedia(UUID fileId) {
        var file = s3FilesService.setMediaReadyIfNotNull(fileId);
        if (file.isEmpty()) {
            return null; // Возвращаем null так как если файл и был сохранен то нужно его затереть
                         // так как в дтошек id
                         // не указан
        }
        return file.get();
    }

    private ExercisesMetrics setMetricsData(ExercisesMetrics metric,
            ExercisesMetricsRequest request) {
        metric.setName(request.name());
        metric.setMetricsValueType(request.metricsValueType());
        metric.setUnit(request.unit());
        return metric;
    }

    private ExerciseEquipment updateEquipmentsData(ExerciseEquipment equipment,
            ExerciseEquipmentRequest request) {

        var properties = prepareEquipmentData(equipment, request);
        equipment.getEquipmentProperties().addAll(properties);
        return equipment;
    }

    private ExerciseEquipment setEquipmentsData(ExerciseEquipment equipment,
            ExerciseEquipmentRequest request) {
        var properties = prepareEquipmentData(equipment, request);
        equipment.setEquipmentProperties(properties);

        return equipment;
    }

    private List<ExerciseEquipmentProperties> prepareEquipmentData(ExerciseEquipment equipment,
            ExerciseEquipmentRequest request) {
        equipment.setName(request.name());
        equipment.setImage(getMedia(request.imageFileId()));

        return getEquipmentPropertiesFromRequest(
                request.propertiesRequests(),
                equipment);

    }

    private List<ExerciseEquipmentProperties> getEquipmentPropertiesFromRequest(
            List<EquipmentPropertiesRequest> request, ExerciseEquipment equipment) {
        return request
                .stream().map(property -> {
                    var dbProperty = new ExerciseEquipmentProperties();
                    dbProperty.setMetricsValueType(property.metricsValueType());
                    dbProperty.setName(property.name());
                    dbProperty.setUnit(property.unit());
                    dbProperty.setExerciseEquipment(equipment);
                    return dbProperty;

                }).toList();
    }

    private boolean isHaveDuplicateMetricsName(List<ExercisesMetricsRequest> requests) {

        Set<String> duplicateName = requests.stream()
                .collect(Collectors.groupingBy(
                        ExercisesMetricsRequest::name,
                        Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        return !duplicateName.isEmpty();
    }

    private boolean isHaveDuplicateEquipmentsName(List<ExerciseEquipmentRequest> requests) {

        Set<String> duplicateName = requests.stream()
                .collect(Collectors.groupingBy(
                        ExerciseEquipmentRequest::name,
                        Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        return !duplicateName.isEmpty();
    }
}
