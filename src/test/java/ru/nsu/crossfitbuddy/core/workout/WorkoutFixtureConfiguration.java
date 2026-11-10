package ru.nsu.crossfitbuddy.core.workout;

import org.hibernate.generator.internal.CurrentTimestampGeneration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import java.time.*;

@TestConfiguration(proxyBeanMethods = false)
class WorkoutFixtureConfiguration {
    @Bean
    FixtureTime fixtureTime() { return new FixtureTime(); }

    @Bean
    HibernatePropertiesCustomizer workoutClock(FixtureTime time) {
        return properties -> properties.put(CurrentTimestampGeneration.CLOCK_SETTING_NAME, time.clock);
    }

    // Does not replace the application's Clock used to verify JWTs.
    static final class FixtureTime {
        private Instant now = Instant.now();
        private final Clock clock = clock(ZoneOffset.UTC);
        void set(Instant instant) { now = instant; }
        private Clock clock(ZoneId zone) {
            return new Clock() {
                public ZoneId getZone() { return zone; }
                public Clock withZone(ZoneId newZone) { return clock(newZone); }
                public Instant instant() { return now; }
            };
        }
    }
}
