package ru.nsu.crossfitbuddy.core.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExerciseEquipmentRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExercisesMetricsRequest;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.GeneralExerciseRequest;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.GeneralExerciseResponse;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipment;
import ru.nsu.crossfitbuddy.core.entity.ExercisesMetrics;
import ru.nsu.crossfitbuddy.core.entity.GeneralExercises;
import ru.nsu.crossfitbuddy.core.entity.MetricsValueType;
import ru.nsu.crossfitbuddy.core.exception.exercises.DuplicateEquipmentsNameExeption;
import ru.nsu.crossfitbuddy.core.exception.exercises.DuplicateMetricsNameExeption;
import ru.nsu.crossfitbuddy.core.mapper.ExerciseEquipmentMapper;
import ru.nsu.crossfitbuddy.core.mapper.ExercisesMetricsMapper;
import ru.nsu.crossfitbuddy.core.mapper.GeneralExerciseMapper;
import ru.nsu.crossfitbuddy.core.repository.ExerciseEquipmentRepository;
import ru.nsu.crossfitbuddy.core.repository.ExerciseMetricRepository;
import ru.nsu.crossfitbuddy.core.repository.GeneralExercisesRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeneralExercisesServiceTest {

    @Mock GeneralExercisesRepository exerciseRepository;
    @Mock ExerciseEquipmentRepository equipmentRepository;
    @Mock ExerciseMetricRepository metricRepository;
    @Mock GeneralExerciseMapper exerciseMapper;
    @Mock ExerciseEquipmentMapper equipmentMapper;
    @Mock ExercisesMetricsMapper metricsMapper;
    @Mock AvatarStorageService storageService;

    @InjectMocks GeneralExercisesService service;

    private GeneralExerciseRequest request(UUID id, String name,
        List<ExercisesMetricsRequest> metrics,
        List<ExerciseEquipmentRequest> equipments,
        MockMultipartFile image, MockMultipartFile video) {
        return new GeneralExerciseRequest(id, name, "desc", metrics, equipments,
            ExerciseCategory.STRENGTH, image, video);
    }

    private ExercisesMetricsRequest metric(UUID id, String name) {
        return new ExercisesMetricsRequest(id, name, MetricsValueType.FLOAT, "кг");
    }

    private ExerciseEquipmentRequest equipment(UUID id, String name) {
        return new ExerciseEquipmentRequest(id, name, null, List.of());
    }

    @Test
    @DisplayName("Создание упражнения сохраняет сущность и грузит медиа в MinIO")
    void createExercise_validUploadsMediaAndSaves() throws Exception {
        var image = new MockMultipartFile("image", "i.jpg", "image/jpeg", "x".getBytes());
        var video = new MockMultipartFile("video", "v.mp4", "video/mp4", "x".getBytes());
        var req = request(null, "Жим", List.of(metric(null, "Вес")),
            List.of(equipment(null, "Штанга")), image, video);

        when(storageService.uploadToFolder(anyString(), any())).thenReturn("media-url");

        service.createExercise(req);

        ArgumentCaptor<GeneralExercises> captor = ArgumentCaptor.forClass(GeneralExercises.class);
        verify(exerciseRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Жим");
        assertThat(captor.getValue().getImageUrl()).isEqualTo("media-url");
        verify(storageService, times(2)).uploadToFolder(anyString(), any());
    }

    @Test
    @DisplayName("Дубликаты имён оборудования отклоняются до сохранения")
    void createExercise_duplicateEquipmentName_throws() {
        var req = request(null, "Жим", List.of(metric(null, "Вес")),
            List.of(equipment(null, "Штанга"), equipment(null, "Штанга")), null, null);

        assertThrows(DuplicateEquipmentsNameExeption.class, () -> service.createExercise(req));
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Дубликаты имён метрик отклоняются до сохранения")
    void createExercise_duplicateMetricName_throws() {
        var req = request(null, "Жим", List.of(metric(null, "Вес"), metric(null, "Вес")),
            List.of(equipment(null, "Штанга")), null, null);

        assertThrows(DuplicateMetricsNameExeption.class, () -> service.createExercise(req));
        verify(exerciseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Обновление без id падает с IllegalArgumentException")
    void updateExercise_nullId_throws() {
        var req = request(null, "Жим", List.of(), List.of(), null, null);
        assertThrows(IllegalArgumentException.class, () -> service.updateExercises(req));
    }

    @Test
    @DisplayName("Обновление несуществующего упражнения падает с EntityNotFoundException")
    void updateExercise_notFound_throws() {
        var id = UUID.randomUUID();
        var req = request(id, "Жим", List.of(), List.of(), null, null);
        when(exerciseRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
            () -> service.updateExercises(req));
    }

    @Test
    @DisplayName("Обновление существующего упражнения подтягивает связанные id")
    void updateExercise_valid_resolvesReferences() {
        var exId = UUID.randomUUID();
        var eqId = UUID.randomUUID();
        var mtId = UUID.randomUUID();
        var req = request(exId, "Жим", List.of(metric(mtId, "Вес")),
            List.of(equipment(eqId, "Штанга")), null, null);

        when(exerciseRepository.findById(exId)).thenReturn(Optional.of(new GeneralExercises()));
        when(equipmentRepository.findById(eqId)).thenReturn(Optional.of(new ExerciseEquipment()));
        when(metricRepository.findById(mtId)).thenReturn(Optional.of(new ExercisesMetrics()));

        service.updateExercises(req);

        verify(exerciseRepository).findById(exId);
        verify(equipmentRepository).findById(eqId);
        verify(metricRepository).findById(mtId);
    }

    @Test
    @DisplayName("Детали упражнения: найденное возвращается через маппер")
    void getDetails_found_maps() {
        var id = UUID.randomUUID();
        var ex = new GeneralExercises();
        var resp = new GeneralExerciseResponse(id, "name", "desc", List.of(), List.of(), ExerciseCategory.STRENGTH, null, null);

        when(exerciseRepository.findByIdWithDetails(id)).thenReturn(Optional.of(ex));
        when(exerciseMapper.toResponse(ex)).thenReturn(resp);

        assertThat(service.getExerciseDetails(id)).isSameAs(resp);
    }

    @Test
    @DisplayName("Детали упражнения: отсутствующее падает с EntityNotFoundException")
    void getDetails_notFound_throws() {
        var id = UUID.randomUUID();
        when(exerciseRepository.findByIdWithDetails(id)).thenReturn(Optional.empty());
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
            () -> service.getExerciseDetails(id));
    }

    @Test
    @DisplayName("Каталог делегирует в репозиторий со Specification и сортировкой по name")
    void getCatalog_delegatesWithSort() {
        when(exerciseRepository.findAll(any(Specification.class), any(Sort.class)))
            .thenReturn(List.of());
        when(exerciseMapper.toResponseList(any())).thenReturn(List.of());

        service.getExercisesCatalog(null, null, null);

        verify(exerciseRepository).findAll(any(Specification.class), any(Sort.class));
        verify(exerciseMapper).toResponseList(any());
    }

    @Test
    @DisplayName("Списки справочников делегируются в репозитории")
    void lists_delegate() {
        when(exerciseRepository.findAll()).thenReturn(List.of());
        when(equipmentRepository.findAll()).thenReturn(List.of());
        when(metricRepository.findAll()).thenReturn(List.of());
        when(exerciseMapper.toResponseList(any())).thenReturn(List.of());
        when(equipmentMapper.toResponseList(any())).thenReturn(List.of());
        when(metricsMapper.toResponseList(any())).thenReturn(List.of());

        service.getAllExercise();
        service.getAllEquipments();
        service.getAllMetrics();

        verify(exerciseRepository).findAll();
        verify(equipmentRepository).findAll();
        verify(metricRepository).findAll();
    }
}
