package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractRepositoryTest {
  protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
      "postgres:17")
      .withDatabaseName("crossfitbuddy_test")
      .withUsername("crossfitbuddy_test")
      .withPassword("crossfitbuddy_test");
  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.hikari.maximum-pool-size", () -> 2);
    registry.add("spring.datasource.hikari.minimum-idle", () -> 1);
  }
}
