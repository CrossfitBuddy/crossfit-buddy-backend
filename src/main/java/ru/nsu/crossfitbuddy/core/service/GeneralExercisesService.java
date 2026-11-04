package ru.nsu.crossfitbuddy.core.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public void setExercise(GeneralExerciseRequest request) {

        log.info("Запрос на сохранения упражнения с названием" + request.name());

        requestValidate(request);

        log.warn("Валидация создания упражнения успешна" + request.name());

        var exercises = new GeneralExercises();
        exercises.setName(request.name());
        exercises.setDescription(request.description());
        exercises.setCategory(request.category());

        List<ExercisesMetrics> metrics = request.exercisesMetrics().stream().map(metric -> {
            if (metric.id() != null) {
                return exerciseMetricRepository.findById(metric.id())
                        .orElseThrow(() -> new EntityNotFoundException("Метрика не найдена"));
            }

            var dbMetric = new ExercisesMetrics();
            dbMetric.setName(metric.name());
            dbMetric.setMetricsValueType(metric.metricsValueType());
            dbMetric.setUnit(metric.unit());
            return dbMetric;
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

                    dbEquipment.setName(equipment.name());

                    List<ExerciseEquipmentProperties> properties = equipment.propertiesRequests()
                            .stream().map(property -> {
                                var dbProperty = new ExerciseEquipmentProperties();
                                dbProperty.setMetricsValueType(property.metricsValueType());
                                dbProperty.setName(property.name());
                                dbProperty.setUnit(property.unit());
                                dbProperty.setExerciseEquipment(dbEquipment);
                                return dbProperty;

                            }).toList();
                    dbEquipment.setEquipmentProperties(properties);
                    return dbEquipment;
                }).toList();

        exercises.setExerciseEquipments(equipments);
        exercises.setExercisesMetrics(metrics);

        generalExercisesRepository.save(exercises);

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
