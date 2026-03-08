# 🛒 My Market App (Reactive, WebFlux + R2DBC + Payments Service)

Reactive интернет-магазин на **Spring Boot 4**, реализующий витрину товаров, корзину и оформление заказов
с проверкой баланса через отдельный **payments-service**.

Проект полностью использует **реактивный стек**:

* Spring WebFlux
* Spring Data R2DBC
* PostgreSQL
* Redis
* Liquibase
* OpenAPI (client + server)
* Docker Compose

---

# 🚀 Технологический стек

## Runtime

* **Java 21**
* **Spring Boot 4.0.2**
* Spring WebFlux
* Spring Data R2DBC
* Spring Data Redis Reactive
* Thymeleaf
* PostgreSQL 16
* Redis 7
* Liquibase
* OpenAPI Generator
* Gradle

---

# 🧩 Архитектура

Проект состоит из **двух сервисов**:

```
my-market-app
 ├─ storefront
 │   ├─ Web UI
 │   ├─ корзина
 │   ├─ оформление заказов
 │   └─ клиент payments API
 │
 └─ payments
     └─ сервис списания средств
```

---

# 💳 Payments Service

Отдельный сервис, отвечающий за баланс.

API:

```
GET  /api/payments/balance
POST /api/payments/charge
```

Пример ответа:

```
{
  "amount": 100000
}
```

Списание средств:

```
{
  "success": true,
  "remainingAmount": 95000,
  "message": "OK"
}
```

---

# 📡 OpenAPI

Контракт платежного сервиса описан в:

```
openapi/payments-api.yaml
```

Код клиента генерируется автоматически:

```
./gradlew openApiGenerate
```

Используется библиотека:

```
webclient
```

Сгенерированный код:

```
build/generated/openapi/payments-client
```

---

# 🗄️ База данных

Liquibase автоматически применяет миграции при старте приложения.

Основные таблицы:

```
items
cart_items
orders
order_items
```

Миграции:

```
001-init-schema.yaml
002-seed-items.yaml
```

Seed-данные используются:

* в приложении
* в интеграционных тестах

---

# ⚡ Redis

Redis используется как **кеш витрины товаров**.

Настройки:

```
spring.data.redis.host
spring.data.redis.port
```

TTL кеша:

```
app.cache.item-ttl
```

---

# 🐳 Docker

Система запускается через **docker compose**.

Сервисы:

```
storefront   -> http://localhost:8080
payments     -> http://localhost:8081
postgres
redis
```

Запуск всей системы:

```
docker compose up --build
```

---

# ▶️ Локальный запуск storefront

Если нужно запустить storefront отдельно.

Поднять зависимости:

```
docker compose up -d postgres redis payments
```

Запустить приложение:

```
./gradlew :storefront:bootRun
```

После запуска:

```
http://localhost:8080
```

---

# 🌐 Основные страницы

```
/items          витрина товаров
/items/{id}     карточка товара

/cart/items     корзина

/orders         список заказов
/orders/{id}    детали заказа
```

---

# 🛍️ Оформление заказа

Алгоритм `BuyService`:

```
1. читается корзина
2. вычисляется сумма заказа
3. запрашивается баланс payments
4. выполняется charge
5. создаётся Order
6. создаются OrderItem
7. корзина очищается
```

Транзакция реализована через:

```
TransactionalOperator
```

---

# 🧪 Тестирование

## Используемые инструменты

* JUnit 5
* Mockito
* Hamcrest
* Reactor Test
* Testcontainers (PostgreSQL)

---

## Запуск тестов

Все тесты:

```
./gradlew test
```

Один тест:

```
./gradlew test --tests "*BuyServiceTest"
```

---

# 🧪 Типы тестов

### Unit tests

Тестируются:

* сервисы
* бизнес-логика
* реактивные цепочки

Используется:

```
Mockito
```

---

### Integration tests

Используется:

```
Testcontainers
```

Поднимается контейнер:

```
PostgreSQL
```

Liquibase применяется автоматически.

---

### Web tests

Используется:

```
@WebFluxTest
```

---

# 🔎 Проверка через curl

## Витрина

```
curl http://localhost:8080/items
```

Поиск:

```
curl "http://localhost:8080/items?search=cap"
```

Сортировка:

```
curl "http://localhost:8080/items?sort=PRICE"
```

Пагинация:

```
curl "http://localhost:8080/items?pageNumber=2&pageSize=10"
```

---

## Добавить товар в корзину

```
curl -X POST "http://localhost:8080/items" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "id=2&action=PLUS"
```

---

## Корзина

```
curl http://localhost:8080/cart/items
```

Изменить количество:

```
curl -X POST "http://localhost:8080/cart/items" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "id=2&action=PLUS"
```

Удалить товар:

```
curl -X POST "http://localhost:8080/cart/items" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "id=2&action=DELETE"
```

---

## Оформить заказ

```
curl -X POST http://localhost:8080/buy
```

---

## Список заказов

```
curl http://localhost:8080/orders
```

Детали заказа:

```
curl http://localhost:8080/orders/1
```

---

# 📦 Структура проекта

```
my-market-app
 ├─ storefront
 │   ├─ controller
 │   ├─ service
 │   ├─ repository
 │   ├─ config
 │   └─ templates
 │
 ├─ payments
 │   └─ REST API для баланса
 │
 ├─ openapi
 │   └─ payments-api.yaml
 │
 └─ docker-compose.yaml
```

---

# 🎯 Особенности проекта

* полностью **reactive стек**
* **R2DBC вместо JPA**
* **OpenAPI-generated client**
* **отдельный payments-service**
* **Redis cache**
* **Testcontainers**
* **Docker Compose окружение**

---
