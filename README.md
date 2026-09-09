# CrossfitBuddy Backend

Стартовый REST API на Spring Boot, Java 21 и Maven. В заготовку входят Spring Security, JPA, Validation, Flyway, PostgreSQL, OpenAPI, Actuator, JWT, Lombok и MapStruct.

## Запуск

1. Установить JDK 21 и Maven 3.9+.
2. Запустить БД: `docker compose up -d`.
3. Запустить приложение: `mvn spring-boot:run`.
4. Проверить health: `http://localhost:8080/actuator/health`.
5. Открыть Swagger UI: `http://localhost:8080/swagger-ui/index.html`.
