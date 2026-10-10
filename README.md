# CrossfitBuddy Backend

Стартовый REST API на Spring Boot, Java 21 и Maven. В заготовку входят Spring Security, JPA, Validation, Flyway, PostgreSQL, OpenAPI, Actuator, JWT, Lombok и MapStruct.

## Локальное развертывание

### Переменные окружения
Создать файл .env в корне проекта со следующим содержимым:

```properties
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_HOST=localhost
DB_PORT=5432
DB_NAME=postgres

APP_PORT=8080
APP_ENV=dev
JWT_SECRET=local-development-secret-change-before-deploy
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### Запуск

1. Установить JDK 21 и Maven 3.9+.
2. Определить переменные окружения (раздел выше)
3. Запустить БД: `docker compose up -d`.
4. Запустить приложение: `mvn spring-boot:run`.
5. Проверить health: `http://localhost:8080/actuator/health`.
6. Открыть Swagger UI: `http://localhost:8080/swagger-ui/index.html`.
