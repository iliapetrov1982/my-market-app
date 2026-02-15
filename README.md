# 🛒 My Market App

Реактивное Spring Boot приложение — витрина интернет-магазина с корзиной и заказами.

---

## 🚀 Стек технологий

Java 21  
Spring Boot 4.x  
Spring WebFlux (Reactive)  
Spring Data R2DBC  
Thymeleaf  
PostgreSQL 16  
R2DBC PostgreSQL Driver  
Liquibase (schema + seed data)  
Gradle  
Docker / Docker Compose

Тестирование:  
JUnit 5  
Mockito  
Hamcrest  
Reactor Test (StepVerifier)  
Testcontainers

---

## ⚡ Реактивная архитектура

Приложение полностью переведено на **реактивную модель (WebFlux + R2DBC)**.

### Контроллеры
- Возвращают `Mono<String>`
- Используют `ServerWebExchange` для чтения form-data
- POST-обработчики работают с `application/x-www-form-urlencoded`
- Нет блокирующих вызовов (`.block()` запрещён)

### Сервисы
- Возвращают `Mono<T>` / `Flux<T>`
- Используют реактивные репозитории
- Транзакции реализованы через `TransactionalOperator`
- Бизнес-логика полностью реактивная

### Репозитории
- `ReactiveCrudRepository`
- Кастомные SQL-запросы через `DatabaseClient`
- Нет JPA / Hibernate
- Нет EntityManager

---

## 🗄️ База данных и миграции

Для управления схемой БД используется Liquibase.

При старте приложения автоматически применяются миграции:

- `001-init-schema.yaml` — создаёт таблицы:
  - items
  - cart_items
  - orders
  - order_items

- `002-seed-items.yaml` — наполняет таблицу `items` начальными товарами (seed data)

Seed-данные:
- используются приложением «из коробки»
- применяются в интеграционных тестах
- не мокируются

---

## 🧪 Тестирование

### Репозитории
- Интеграционные тесты
- PostgreSQL Testcontainers
- Liquibase автоматически применяется
- Проверяются реальные SQL-запросы
- Используется реальная реактивная БД (R2DBC)

### Сервисы
- Юнит-тесты с Mockito
- Используется `StepVerifier`
- Транзакции подменяются passthrough-реализацией `TransactionalOperator`
- Проверяется:
  - реактивная цепочка
  - корректный расчёт total_sum
  - сохранение order_items
  - очистка корзины
  - обработка пустой корзины
  - формирование витрины (rows по 3 элемента с placeholder)

### Контроллеры
- WebFlux тесты (`@WebFluxTest`)
- WebTestClient
- @MockBean для сервисов
- Проверяется:
  - имя view
  - model-атрибуты
  - redirect-логика
  - корректная обработка form-data
  - POST /buy
  - POST /cart/items
  - POST /items

---

## 📦 Основные тестовые зависимости (Gradle)

```gradle
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.springframework.boot:spring-boot-starter-webflux-test'
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:postgresql'
testImplementation 'io.projectreactor:reactor-test'
testImplementation 'org.hamcrest:hamcrest'
```

---

## 🐳 Docker

Приложение и база данных запускаются через docker-compose.

### PostgreSQL
- image: postgres:16
- database: my-market-app
- user: my_market_user
- password: my_market_pass

### Spring Boot приложение
- Java 21
- порт: 8080
- Liquibase применяется автоматически при старте
- Используется R2DBC (не JDBC)

Запуск всего окружения:

```bash
docker compose up --build
```

После запуска приложение доступно по адресу:

http://localhost:8080

---

## 🌐 Основные страницы приложения

`/` или `/items` — витрина товаров  
`/items/{id}` — карточка товара  
`/cart/items` — корзина  
`/orders` — список заказов  
`/orders/{id}` — детали заказа

---

## 🧠 Особенности реализации

- Полностью реактивный стек
- Нет блокирующих операций
- Нет JPA / Hibernate
- Нет @Transactional (используется `TransactionalOperator`)
- Order total рассчитывается реактивно
- order_items сохраняются через `saveAll(Publisher)`
- Корзина очищается реактивно после успешной покупки
- UI построен на Thymeleaf (server-side rendering + WebFlux)

---

## 📌 Итог

Проект демонстрирует:
- построение полноценного реактивного CRUD-приложения
- работу с R2DBC + PostgreSQL
- реактивные транзакции
- интеграцию Liquibase
- unit + integration тестирование реактивного кода
- корректную работу формы POST в WebFlux

---
