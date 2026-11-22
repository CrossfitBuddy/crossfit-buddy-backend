package ru.nsu.crossfitbuddy.core.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipment;
import ru.nsu.crossfitbuddy.core.entity.GeneralExercises;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GeneralExercisesRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private GeneralExercisesRepository exerciseRepository;

    @Autowired
    private ExerciseEquipmentRepository equipmentRepository;

    private ExerciseEquipment barbell;
    private ExerciseEquipment dumbbell;

    @BeforeEach
    void setUp() {
        barbell = new ExerciseEquipment();
        barbell.setName("Штанга");
        equipmentRepository.save(barbell);

        dumbbell = new ExerciseEquipment();
        dumbbell.setName("Гантели");
        equipmentRepository.save(dumbbell);

        GeneralExercises ex1 = new GeneralExercises();
        ex1.setName("Приседания со штангой");
        ex1.setDescription("Базовое упражнение");
        ex1.setCategory(ExerciseCategory.STRENGTH);
        ex1.setExerciseEquipments(Set.of(barbell));
        exerciseRepository.save(ex1);

        GeneralExercises ex2 = new GeneralExercises();
        ex2.setName("Армейский жим");
        ex2.setDescription("Жим над головой");
        ex2.setCategory(ExerciseCategory.STRENGTH);
        ex2.setExerciseEquipments(Set.of(barbell, dumbbell));
        exerciseRepository.save(ex2);

        GeneralExercises ex3 = new GeneralExercises();
        ex3.setName("Берпи");
        ex3.setDescription("Кардио нагрузка");
        ex3.setCategory(ExerciseCategory.CARDIO);
        exerciseRepository.save(ex3);

        exerciseRepository.flush();
    }

    @Test
    @DisplayName("Должен возвращать упражнения, отсортированные по алфавиту")
    void shouldReturnExercisesSortedAlphabetically() {
        List<GeneralExercises> result = exerciseRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("Армейский жим");
        assertThat(result.get(1).getName()).isEqualTo("Берпи");
        assertThat(result.get(2).getName()).isEqualTo("Приседания со штангой");
    }

    @Test
    @DisplayName("Должен искать по имени регистронезависимо")
    void shouldSearchByNameCaseInsensitive() {
        Specification<GeneralExercises> spec = (root, query, cb) ->
            cb.like(cb.lower(root.get("name")), "%присед%");

        List<GeneralExercises> result = exerciseRepository.findAll(spec);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Приседания со штангой");
    }

    @Test
    @DisplayName("Должен фильтровать по категории")
    void shouldFilterByCategory() {
        Specification<GeneralExercises> spec = (root, query, cb) ->
            cb.equal(root.get("category"), ExerciseCategory.CARDIO);

        List<GeneralExercises> result = exerciseRepository.findAll(spec);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Берпи");
    }

    @Test
    @DisplayName("Должен фильтровать по оборудованию (проверка JOIN)")
    void shouldFilterByEquipment() {
        Specification<GeneralExercises> spec = (root, query, cb) -> {
            var join = root.join("exerciseEquipments", jakarta.persistence.criteria.JoinType.INNER);
            return cb.equal(join.get("id"), dumbbell.getId());
        };

        List<GeneralExercises> result = exerciseRepository.findAll(spec);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Армейский жим");
    }

    @Test
    @DisplayName("Должен корректно применять комбинированные фильтры и сортировку")
    void shouldApplyCombinedFiltersAndSort() {
        Specification<GeneralExercises> spec = (root, query, cb) -> {
            query.distinct(true);
            return cb.and(
                cb.equal(root.get("category"), ExerciseCategory.STRENGTH),
                cb.like(cb.lower(root.get("name")), "%жим%")
            );
        };

        List<GeneralExercises> result = exerciseRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "name"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Армейский жим");
    }
}
