package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.crossfitbuddy.core.entity.TrainerSubscription;
import java.util.UUID;

public interface TrainerSubscriptionRepository extends JpaRepository<TrainerSubscription, UUID> {}
