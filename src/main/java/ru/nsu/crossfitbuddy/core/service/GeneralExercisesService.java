package ru.nsu.crossfitbuddy.core.service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.entity.Exercise;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipment;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipmentProperties;
import ru.nsu.crossfitbuddy.core.entity.ExercisesMetrics;
import ru.nsu.crossfitbuddy.core.entity.S3File;
import ru.nsu.crossfitbuddy.core.exception.exercises.DuplicateEquipmentsNameExeption;
import ru.nsu.crossfitbuddy.core.exception.exercises.DuplicateMetricsNameExeption;
import ru.nsu.crossfitbuddy.core.exception.exercises.DuplicatePropertiesNameException;
import ru.nsu.crossfitbuddy.core.mapper.ExerciseEquipmentMapper;
import ru.nsu.crossfitbuddy.core.mapper.ExercisesMetricsMapper;
import ru.nsu.crossfitbuddy.core.mapper.GeneralExerciseMapper;
import ru.nsu.crossfitbuddy.core.repository.ExerciseEquipmentRepository;
import ru.nsu.crossfitbuddy.core.repository.ExerciseMetricRepository;
import ru.nsu.crossfitbuddy.core.repository.GeneralExercisesRepository;
import ru.nsu.crossfitbuddy.core.specification.ExerciseSpecification;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneralExercisesService {
  private final ProfileService profileService;

  private final GeneralExercisesRepository generalExercisesRepository;
  private final ExerciseEquipmentRepository exerciseEquipmentRepository;
  private final ExerciseMetricRepository exerciseMetricRepository;

  private final GeneralExerciseMapper generalExerciseMapper;
  private final ExerciseEquipmentMapper exerciseEquipmentMapper;
  private final ExercisesMetricsMapper exercisesMetricsMapper;

  private final S3FilesService s3FilesService;

  @Transactional
  public Exercise createExercise(GeneralExerciseRequest request, CurrentUser account) {

    log.info("Запрос на сохранения упражнения с названием " + request.name());

    requestValidate(request);

    log.warn("Валидация создания упражнения успешна " + request.name());

    var exercises = new Exercise();

    createExerciseData(exercises, request, account);

    exercises = generalExercisesRepository.save(exercises);

    return exercises;
  }

  @Transactional
  public void updateExercises(GeneralExerciseRequest request) {
    if (request.id() == null) {
      throw new IllegalArgumentException(
          "In exercises update request filde id must not be null");
    }
    requestValidate(request);

    log.warn("Валидация обнавления упражнения успешна " + request.name());

    updateExerciseData(request);
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
    Specification<Exercise> spec = Specification.allOf(
            ExerciseSpecification.hasNameLike(searchName),
            ExerciseSpecification.hasCategory(category),
            ExerciseSpecification.hasEquipment(equipmentId)
    );

    Sort sort = Sort.by(Sort.Direction.ASC, "name");

    Specification<Exercise> distinctSpec = (root, query, cb) -> {
      if (query != null) query.distinct(true);
      return spec.toPredicate(root, query, cb);
    };

    List<Exercise> exercises = generalExercisesRepository.findAll(distinctSpec, sort);
    return generalExerciseMapper.toResponseList(exercises);
  }

  @Transactional(readOnly = true)
  public GeneralExerciseResponse getExerciseDetails(UUID id) {
    Exercise exercise = generalExercisesRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Упражнение с id " + id + " не найдено"));
    return generalExerciseMapper.toResponse(exercise);
  }

  private void updateExerciseData(GeneralExerciseRequest request) {

    var exercise = generalExercisesRepository.findById(request.id())
        .orElseThrow(() -> new EntityNotFoundException(
            "Exercise entity with id - " + request.id() + " not found"));

    removeExscessMetric(exercise, request.exercisesMetrics());

    removeExscessEquipments(exercise, request.exercisesEquipments());

    updateExerciseMetrics(exercise, request.exercisesMetrics());

    updateExerciseEquipments(exercise, request.exercisesEquipments());

    exercise.setName(request.name());
    exercise.setDescription(request.description());
    exercise.setCategory(request.category());
    exercise.setImage(getMedia(request.imageFileId()));
    exercise.setVideo(getMedia(request.videoFileId()));
    exercise.setUpdatedAt(Instant.now());
  }

  private void createExerciseData(Exercise exercise, GeneralExerciseRequest request,
      CurrentUser user) {
    var accountDetails = profileService.findProfileOrThrow(user.id(), true);
    exercise.setName(request.name());
    exercise.setDescription(request.description());
    exercise.setCategory(request.category());
    exercise.setImage(getMedia(request.imageFileId()));
    exercise.setVideo(getMedia(request.videoFileId()));
    exercise.setCreatedBy(accountDetails.getUser());
    exercise.setUpdatedAt(Instant.now());
    exercise.setCreatedAt(Instant.now());

    Set<ExercisesMetrics> metrics = createExercisesMetrics(request.exercisesMetrics());
    exercise.setExercisesMetrics(metrics);

    Set<ExerciseEquipment> equipments = createExerciseEquipments(request.exercisesEquipments());
    exercise.setExerciseEquipments(equipments);
  }

  private S3File getMedia(UUID fileId) {
    if (fileId == null) {
      return null;// Возвращаем null так как если файл и был сохранен то нужно его затереть
                  // так как в дтошек id
                  // не указан
    }
    return s3FilesService.setFileReadyAndGet(fileId);
  }

  private void updateExerciseEquipments(Exercise exercise, List<ExerciseEquipmentRequest> request) {

    if (request == null) {
      return;
    }

    request
        .forEach(equipment -> {
          if (equipment.id() != null) {
            var dbEquipment = exerciseEquipmentRepository.findById(equipment.id())
                .orElseThrow(
                    () -> new EntityNotFoundException("Оборудование с id: "
                        + equipment.id() + " не найдено"));
            dbEquipment.getEquipmentProperties().clear();
            updateEquipmentsData(dbEquipment, equipment);
            if (exercise.getExerciseEquipments().stream().noneMatch(m -> m.getId() == dbEquipment.getId())) {
              exercise.getExerciseEquipments().add(dbEquipment);
            }

            return;
          }
          var newEquipment = new ExerciseEquipment();
          exercise.getExerciseEquipments().add(setEquipmentsData(newEquipment, equipment));
        });
  }

  private void updateExerciseMetrics(Exercise exercise,
      List<ExercisesMetricsRequest> exercisesMetricsRequests) {

    if (exercisesMetricsRequests == null) {
      return;
    }

    exercisesMetricsRequests.forEach(metric -> {
      if (metric.id() != null) {
        var dbMetric = exerciseMetricRepository.findById(metric.id())
            .orElseThrow(() -> new EntityNotFoundException("Метрика не найдена"));
        setMetricsData(dbMetric, metric);
        if (exercise.getExercisesMetrics().stream().noneMatch(m -> m.getId() == dbMetric.getId())) {
          exercise.getExercisesMetrics().add(dbMetric);
        }
        return;
      }
      var dbMetric = new ExercisesMetrics();
      exercise.getExercisesMetrics().add(setMetricsData(dbMetric, metric));
    });
  }

  private void removeExscessMetric(Exercise exercise, List<ExercisesMetricsRequest> request) {
    var newMetricId = request.stream()
        .map(ExercisesMetricsRequest::id)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    exercise.getExercisesMetrics()
        .removeIf(metric -> !newMetricId.contains(metric.getId()));
  }

  private void removeExscessEquipments(Exercise exercise, List<ExerciseEquipmentRequest> request) {
      var newEquipmentId = new HashSet<>();
      for (ExerciseEquipmentRequest exerciseEquipmentRequest : request) {
          UUID id = exerciseEquipmentRequest.id();
          if (id != null) {
              newEquipmentId.add(id);
          }
      }

      exercise.getExerciseEquipments()
        .removeIf(metric -> !newEquipmentId.contains(metric.getId()));

  }

  private Set<ExercisesMetrics> createExercisesMetrics(List<ExercisesMetricsRequest> requests) {
    if (requests == null) {
      return new java.util.HashSet<>();
    }

    return requests.stream().map(metric -> {
      if (metric.id() != null) {
        return exerciseMetricRepository.findById(metric.id())
            .orElseThrow(() -> new EntityNotFoundException("Метрика не найдена"));
      }
      var dbMetric = new ExercisesMetrics();
      return setMetricsData(dbMetric, metric);
    }).collect(java.util.stream.Collectors.toSet());
  }

  private Set<ExerciseEquipment> createExerciseEquipments(List<ExerciseEquipmentRequest> requests) {
    if (requests == null) {
      return new java.util.HashSet<>();
    }

    return requests.stream().map(equipment -> {
      if (equipment.id() != null) {
        return exerciseEquipmentRepository.findById(equipment.id())
                .orElseThrow(() -> new EntityNotFoundException("Оборудование с id: " + equipment.id() + " не найдено"));
      }
      var dbEquipment = new ExerciseEquipment();
      return setEquipmentsData(dbEquipment, equipment);
    }).collect(java.util.stream.Collectors.toSet());
  }

  private ExercisesMetrics setMetricsData(ExercisesMetrics metric,
      ExercisesMetricsRequest request) {
    metric.setName(request.name());
    metric.setMetricsValueType(request.metricsValueType());
    metric.setUnit(request.unit());
    return metric;
  }

  private void updateEquipmentsData(ExerciseEquipment equipment,
                                    ExerciseEquipmentRequest request) {

    var properties = prepareEquipmentData(equipment, request);
    equipment.getEquipmentProperties().addAll(properties);
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
    if (request == null) {
      return new ArrayList<>();
    }
    return request
        .stream().map(property -> {
          var dbProperty = new ExerciseEquipmentProperties();
          dbProperty.setMetricsValueType(property.metricsValueType());
          dbProperty.setName(property.name());
          dbProperty.setUnit(property.unit());
          dbProperty.setExerciseEquipment(equipment);
          return dbProperty;

        }).collect(Collectors.toCollection(ArrayList::new));

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
    request.exercisesEquipments().forEach((exercise) -> {
      if (isHaveDuplicatePropertiesName(exercise.propertiesRequests())) {
        log.warn("Названия свойств упражнения дублируются");
        throw new DuplicatePropertiesNameException();
      }
    });
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

  private boolean isHaveDuplicatePropertiesName(List<EquipmentPropertiesRequest> requests) {
    Set<String> duplicateName = requests.stream()
        .collect(Collectors.groupingBy(
            EquipmentPropertiesRequest::name,
            Collectors.counting()))
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() > 1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
    return !duplicateName.isEmpty();

  }
}
