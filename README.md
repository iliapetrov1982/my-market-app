# 🛒 My Market App

Spring Boot приложение — витрина интернет-магазина с корзиной и заказами.

---

🚀 Стек технологий

Java 21  
Spring Boot 4.0.2  
Spring MVC  
Spring Data JPA  
Thymeleaf  
PostgreSQL 16  
Liquibase (schema + seed data)  
Gradle  
Docker / Docker Compose

Тестирование:
JUnit 5  
Mockito  
Hamcrest  
Testcontainers

---

🗄️ База данных и миграции

Для управления схемой БД используется Liquibase. При старте приложения автоматически накатываются миграции:

- 001-init-schema.yaml — создаёт таблицы:
  items, cart_items, orders, order_items
- 002-seed-items.yaml — наполняет таблицу items начальными товарами (seed data)

Seed-данные:
- используются приложением «из коробки»
- применяются в интеграционных тестах репозиториев
- не мокируются

---

🧪 Тестирование

Репозитории:
- интеграционные тесты
- PostgreSQL Testcontainers
- Liquibase автоматически применяется
- проверяются реальные JPQL-запросы (ItemRepository, OrderRepository и др.)

Сервисы:
- юнит-тесты с Mockito
- проверяется бизнес-логика:
    - нормализация pageSize
    - сортировка
    - обработка пустого поиска
    - fallback на последнюю страницу
    - формирование строк по 3 элемента с placeholder-ами

Контроллеры:
- @WebMvcTest
- MockMvc
- @MockBean для сервисов
- проверяется:
    - имя view (items, item)
    - model-атрибуты
    - redirect-логика
    - обработка POST-запросов

---

📦 Основные тестовые зависимости (Gradle)

testImplementation 'org.springframework.boot:spring-boot-starter-test'  
testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'  
testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'  
testImplementation 'org.springframework.boot:spring-boot-testcontainers'  
testImplementation 'org.testcontainers:junit-jupiter'  
testImplementation 'org.testcontainers:postgresql'  
testImplementation 'org.hamcrest:hamcrest'

Аннотация @MockBean предоставляется зависимостью spring-boot-starter-test.

---

🐳 Docker

Приложение и база данных запускаются через docker-compose.

PostgreSQL:
- image: postgres:16
- database: my-market-app
- user: my_market_user
- password: my_market_pass

Spring Boot приложение:
- Java 21
- порт: 8080
- Liquibase применяется автоматически при старте

Запуск всего окружения:

```bash
docker compose up --build
```


После запуска приложение доступно по адресу:

http://localhost:8080

---

🌐 Основные страницы приложения

/ или /items — витрина товаров  
/items/{id} — карточка товара  
/cart/items — корзина  
/orders — список заказов

---