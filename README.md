# 🛒 My Market App

Реактивный интернет-магазин на **Spring Boot 4** с витриной товаров, корзиной, оформлением заказов
и отдельным сервисом платежей. Спринт 8: добавлена авторизация пользователей и защита API через **Spring Security + OAuth2 (Keycloak)**.

---

## 🧩 Архитектура

```
Browser
   │  form login (user1/user1pass)
   ▼
storefront (8080)  ──── OAuth2 Client Credentials ──► payments (8081)
   │                         (Keycloak JWT)
   ▼
PostgreSQL · Redis

Keycloak (8180) — сервер авторизации OAuth2
```

**storefront** — Web UI, корзина, заказы. Авторизация пользователей по логину/паролю.
Запросы в payments отправляются с JWT-токеном, полученным по Client Credentials Flow.

**payments** — REST API баланса. Принимает только запросы с валидным JWT (Resource Server).
Баланс привязан к конкретному пользователю.

---

## 🚀 Технологический стек

| Компонент | Технология |
|-----------|-----------|
| Язык | Java 21 |
| Framework | Spring Boot 4.0.2, WebFlux |
| База данных | PostgreSQL 16, R2DBC |
| Кеш | Redis 7 |
| Миграции | Liquibase |
| Безопасность | Spring Security 6, OAuth2, Keycloak 26 |
| API | OpenAPI Generator |
| Сборка | Gradle (multi-module) |
| Контейнеры | Docker Compose |

---

## 📦 Структура проекта

```
my-market-app/
 ├── storefront/              # Web-приложение
 │   ├── config/
 │   │   ├── SecurityConfig.java      # form login, route protection
 │   │   ├── OAuth2ClientConfig.java  # ReactiveOAuth2AuthorizedClientManager
 │   │   └── PaymentsClientConfig.java # WebClient с OAuth2 фильтром
 │   ├── controller/
 │   ├── service/
 │   ├── repository/
 │   └── resources/templates/
 │
 ├── payments/                # REST сервис платежей
 │   ├── config/
 │   │   └── SecurityConfig.java      # JWT Resource Server
 │   ├── controller/
 │   └── service/
 │
 ├── openapi/
 │   └── payments-api.yaml
 │
 ├── keycloak/
 │   └── realm-export.json    # авто-импорт realm при старте
 │
 ├── docker-compose.yaml      # полный стек
 └── docker-compose.infra.yaml # только инфраструктура (для IDE)
```

---

## 🐳 Запуск через Docker Compose

### Полный стек

```bash
docker compose up -d --build
```

Сервисы поднимаются в порядке зависимостей:
1. **Keycloak** (~60-90 сек на импорт realm)
2. **PostgreSQL**, **Redis**
3. **payments** (ждёт Keycloak)
4. **storefront** (ждёт всех)

| Сервис | URL |
|--------|-----|
| Витрина | http://localhost:8080 |
| Payments API | http://localhost:8081 |
| Keycloak Admin | http://localhost:8180 (admin / admin) |

### Пересборка отдельного сервиса

```bash
docker compose up -d --build storefront
docker compose up -d --build payments
```

---

## 💻 Локальная разработка (из IDE)

Поднять только инфраструктуру:

```bash
docker compose -f docker-compose.infra.yaml up -d
```

Запустить storefront:

```bash
./gradlew :storefront:bootRun
```

Запустить payments:

```bash
./gradlew :payments:bootRun
```

---

## 🔑 Авторизация

### Тестовые пользователи

| Username | Password | Email |
|----------|----------|-------|
| `user1` | `user1pass` | alice@example.com |
| `user2` | `user2pass` | bob@example.com |

### Страницы доступные без входа

```
GET  /          витрина
GET  /items     витрина
GET  /items/:id карточка товара
GET  /login     форма входа
```

### Страницы требующие входа

```
POST /items/**     добавить в корзину
GET  /cart/items   корзина
POST /buy          оформить заказ
GET  /orders       список заказов
GET  /orders/:id   детали заказа
```

### OAuth2 Client Credentials (storefront → payments)

Storefront автоматически получает JWT-токен у Keycloak перед каждым запросом в payments.
Настройки в `application.yml`:

```yaml
spring.security.oauth2.client.registration.payments-client:
  authorization-grant-type: client_credentials
  client-id: storefront-client
  client-secret: storefront-client-secret
```

---

## 🗄️ База данных

Миграции применяются автоматически через Liquibase при старте storefront.

| Файл | Содержание |
|------|-----------|
| `001-init-schema.yaml` | Создание таблиц |
| `002-seed-items.yaml` | 25 тестовых товаров |
| `003-add-username.yaml` | Привязка корзины и заказов к пользователю |

---

## 🧪 Тестирование

### Запуск всех тестов

```bash
./gradlew test
```

### Запуск тестов отдельного модуля

```bash
./gradlew :storefront:test
./gradlew :payments:test
```

### Запуск конкретного теста

```bash
./gradlew test --tests "*BuyServiceTest"
./gradlew test --tests "*SecurityAccessControlTest"
./gradlew test --tests "*PaymentsControllerSecurityTest"
```

### Типы тестов

**Unit-тесты** — сервисы с моками (`BuyServiceTest`, `CartCommandServiceTest`, `ItemsServiceTest`, `PaymentsGatewayOAuth2Test`)

**Интеграционные тесты** — Testcontainers PostgreSQL (`BuyServiceIntegrationTest`, `ItemRepositoryTest`)

**Redis-интеграционные тесты** — Testcontainers Redis (`ItemsServiceRedisIntegrationTest`)

**Web-тесты** — `@WebFluxTest` с моками (`BuyControllerTest`, `SecurityAccessControlTest`, `PaymentsControllerSecurityTest`)

---

## 🔎 Проверка через curl

### Витрина (без авторизации)

```bash
# Список товаров
curl http://localhost:8080/items

# Поиск
curl "http://localhost:8080/items?search=cap"

# Сортировка по цене
curl "http://localhost:8080/items?sort=PRICE"

# Пагинация
curl "http://localhost:8080/items?pageNumber=2&pageSize=10"
```

### Payments API (требует JWT)

Получить токен:

```bash
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/my-market/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=storefront-client&client_secret=storefront-client-secret" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

Баланс:

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/payments/balance
```

Списание:

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 1000}' \
  http://localhost:8081/api/payments/charge
```

---

## ⚡ Redis

Redis кешируует карточки товаров витрины.

```yaml
app.cache.item-ttl: PT10M  # TTL кеша (по умолчанию 10 минут)
```

---

## 📡 OpenAPI

Контракт payments описан в `openapi/payments-api.yaml`.
Клиентский код генерируется автоматически при сборке:

```bash
./gradlew :storefront:openApiGenerate   # WebClient-клиент
./gradlew :payments:openApiGenerate     # серверный интерфейс
```

---

## 🔐 Keycloak

Подробная документация: [`keycloak/KEYCLOAK.md`](keycloak/KEYCLOAK.md)

Realm `my-market` импортируется автоматически при старте контейнера из `keycloak/realm-export.json`.

| Клиент | Назначение |
|--------|-----------|
| `storefront-client` | Machine-to-machine (Client Credentials) |
| `storefront-web` | Браузерный клиент (Authorization Code) |