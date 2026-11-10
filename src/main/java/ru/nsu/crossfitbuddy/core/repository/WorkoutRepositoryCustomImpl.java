package ru.nsu.crossfitbuddy.core.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import ru.nsu.crossfitbuddy.core.entity.*;
import ru.nsu.crossfitbuddy.core.workout.WorkoutFilter;
import ru.nsu.crossfitbuddy.core.workout.WorkoutSortingKey;

import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
public class WorkoutRepositoryCustomImpl implements WorkoutRepositoryCustom {
    private final EntityManager entityManager;

    @Override
    public Page<Workout> findAccessible(
        UUID userId,
        UserRole activeRole,
        WorkoutFilter filter
    ) {
        var pageable = PageRequest.of(filter.page(), filter.size());

        var items = findWorkouts(userId, activeRole, filter);
        var total = countWorkouts(userId, activeRole, filter);

        return new PageImpl<>(items, pageable, total);
    }

    private List<Workout> findWorkouts(
        UUID userId,
        UserRole role,
        WorkoutFilter filter
    ) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Workout.class);
        var root = query.from(Workout.class);

        query.select(root)
            .where(buildPredicate(cb, query, root, userId, role, filter))
            .orderBy(buildOrder(cb, root, filter.sort()));

        return entityManager.createQuery(query)
            .setFirstResult(Math.toIntExact(
                (long) filter.page() * filter.size()
            ))
            .setMaxResults(filter.size())
            .getResultList();
    }

    private long countWorkouts(
        UUID userId,
        UserRole role,
        WorkoutFilter filter
    ) {
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Long.class);
        var root = query.from(Workout.class);

        query.select(cb.count(root))
            .where(buildPredicate(cb, query, root, userId, role, filter));

        return entityManager.createQuery(query).getSingleResult();
    }

    private Predicate buildPredicate(
        CriteriaBuilder cb,
        CriteriaQuery<?> query,
        Root<Workout> root,
        UUID userId,
        UserRole role,
        WorkoutFilter filter
    ) {
        return cb.and(
            buildAccessPredicate(cb, query, root, userId, role, filter),
            buildFilterPredicate(cb, root, filter)
        );
    }

    private Predicate buildAccessPredicate(
        CriteriaBuilder cb,
        CriteriaQuery<?> query,
        Root<Workout> root,
        UUID userId,
        UserRole role,
        WorkoutFilter filter
    ) {
        var sources = filter.sources();

        Predicate own = cb.equal(root.get("createdBy"), userId);

        List<Predicate> predicates = new ArrayList<>();

        if (sources.isEmpty() || sources.contains(WorkoutType.OWN)) {
            predicates.add(own);
        }

        if (role == UserRole.ATHLETE) {
            Predicate published = cb.notEqual(
                root.get("status"),
                WorkoutStatus.DRAFT
            );

            if (sources.isEmpty() || sources.contains(WorkoutType.ASSIGNED)) {
                predicates.add(cb.and(
                    cb.not(own),
                    published,
                    cb.equal(root.get("type"), WorkoutType.ASSIGNED),
                    assignedToUser(cb, query, root, userId)
                ));
            }

            if (sources.isEmpty() || sources.contains(WorkoutType.SHARED)) {
                predicates.add(cb.and(
                    cb.not(own),
                    published,
                    cb.equal(root.get("type"), WorkoutType.SHARED),
                    sharedWithUser(cb, query, root, userId)
                ));
            }
        }

        return cb.or(predicates.toArray(Predicate[]::new));
    }

    private Predicate assignedToUser(
        CriteriaBuilder cb,
        CriteriaQuery<?> query,
        Root<Workout> workout,
        UUID userId
    ) {
        var assigned = query.subquery(UUID.class);
        var participant = assigned.from(WorkoutParticipant.class);

        assigned.select(participant.get("id"))
            .where(
                cb.equal(participant.get("workout"), workout),
                cb.equal(participant.get("athlete").get("id"), userId),
                hasActiveSubscription(cb, assigned, participant, userId)
            );

        return cb.exists(assigned);
    }

    private Predicate hasActiveSubscription(
        CriteriaBuilder cb,
        Subquery<?> parent,
        Root<WorkoutParticipant> participant,
        UUID userId
    ) {
        var subscription = parent.subquery(UUID.class);
        var root = subscription.from(TrainerSubscription.class);

        subscription.select(root.get("id"))
            .where(
                cb.equal(root.get("athlete").get("id"), userId),
                cb.equal(root.get("trainer"), participant.get("trainer")),
                cb.equal(root.get("status"), SubscriptionStatus.ACTIVE)
            );

        return cb.exists(subscription);
    }

    private Predicate sharedWithUser(
        CriteriaBuilder cb,
        CriteriaQuery<?> query,
        Root<Workout> workout,
        UUID userId
    ) {
        var shared = query.subquery(UUID.class);
        var participant = shared.from(WorkoutParticipant.class);

        shared.select(participant.get("id"))
            .where(
                cb.equal(participant.get("workout"), workout),
                cb.equal(participant.get("athlete").get("id"), userId)
            );

        return cb.exists(shared);
    }

    private Predicate buildFilterPredicate(
        CriteriaBuilder cb,
        Root<Workout> root,
        WorkoutFilter filter
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (!filter.statuses().isEmpty()) {
            predicates.add(root.get("status").in(filter.statuses()));
        }

        if (filter.dateFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                root.get("createdAt"),
                filter.dateFrom()
            ));
        }

        if (filter.dateToExclusive() != null) {
            predicates.add(cb.lessThan(
                root.get("createdAt"),
                filter.dateToExclusive()
            ));
        }

        for (String pattern : filter.titlePatterns()) {
            predicates.add(cb.like(
                cb.lower(root.get("title")),
                pattern.toLowerCase(Locale.ROOT),
                '!'
            ));
        }

        return cb.and(predicates.toArray(Predicate[]::new));
    }

    private List<Order> buildOrder(
        CriteriaBuilder cb,
        Root<Workout> root,
        WorkoutSortingKey sort
    ) {
        var title = cb.lower(root.get("title"));
        var createdAt = root.get("createdAt");

        var primary = switch (sort) {
            case TITLE_ASC -> cb.asc(title);
            case TITLE_DESC -> cb.desc(title);
            case DATE_ASC -> cb.asc(createdAt);
            case DATE_DESC -> cb.desc(createdAt);
        };

        List<Order> orders = new ArrayList<>();

        if (sort == WorkoutSortingKey.TITLE_ASC || sort == WorkoutSortingKey.TITLE_DESC) {
            orders.add(cb.asc(
                cb.<Integer>selectCase()
                    .when(cb.isNull(root.get("title")), 1)
                    .otherwise(0)
            ));
        }

        orders.add(primary);
        orders.add(cb.desc(root.get("id")));

        return orders;
    }
}
