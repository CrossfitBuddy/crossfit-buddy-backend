package ru.nsu.crossfitbuddy.core.specification;

import org.springframework.data.jpa.domain.Specification;
import ru.nsu.crossfitbuddy.core.entity.Exercise;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;

import java.util.UUID;

public class ExerciseSpecification {

    public static Specification<Exercise> hasNameLike(String name) {
        return (root, query, cb) ->
                (name == null || name.isBlank()) ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Exercise> hasCategory(ExerciseCategory category) {
        return (root, query, cb) ->
                (category == null) ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Exercise> hasEquipment(UUID equipmentId) {
        return (root, query, cb) -> {
            if (equipmentId == null) return null;
            var join = root.join("exerciseEquipments", jakarta.persistence.criteria.JoinType.INNER);
            return cb.equal(join.get("id"), equipmentId);
        };
    }
}