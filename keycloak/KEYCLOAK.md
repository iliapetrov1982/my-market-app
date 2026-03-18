# Keycloak — настройка OAuth2-сервера авторизации

## Что настроено

Keycloak запускается автоматически при старте `docker-compose.yaml`.  
Realm `my-market` импортируется из файла `keycloak/realm-export.json`.

### Realm: `my-market`

| Параметр        | Значение                                      |
|-----------------|-----------------------------------------------|
| Realm           | `my-market`                                   |
| Admin UI        | http://localhost:8180                         |
| Admin логин     | `admin` / `admin`                             |
| Issuer URI      | `http://localhost:8180/realms/my-market`      |
| Token endpoint  | `http://localhost:8180/realms/my-market/protocol/openid-connect/token` |
| JWKS endpoint   | `http://localhost:8180/realms/my-market/protocol/openid-connect/certs` |

---

### Клиенты

#### `storefront-client` — Machine-to-machine (Client Credentials Flow)
Используется для авторизованных HTTP-запросов из `storefront` в `payments`.

| Параметр        | Значение                    |
|-----------------|-----------------------------|
| client_id       | `storefront-client`         |
| client_secret   | `storefront-client-secret`  |
| Grant type      | `client_credentials`        |

Получить токен вручную:
```bash
curl -s -X POST \
  http://localhost:8180/realms/my-market/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=storefront-client" \
  -d "client_secret=storefront-client-secret" | jq .
```

#### `storefront-web` — Браузерный клиент (Authorization Code + PKCE)
Используется для входа конечных пользователей через UI витрины.

| Параметр        | Значение                    |
|-----------------|-----------------------------|
| client_id       | `storefront-web`            |
| Grant type      | `authorization_code`        |
| Redirect URIs   | `http://localhost:8080/*`   |

---

### Тестовые пользователи

| Username | Password   | Email                |
|----------|------------|----------------------|
| `user1`  | `user1pass`| alice@example.com    |
| `user2`  | `user2pass`| bob@example.com      |

---

## Запуск только инфраструктуры (для разработки из IDE)

```bash
docker compose -f docker-compose.infra.yaml up -d
```

После старта Keycloak будет доступен на `http://localhost:8180`.  
Проверить готовность:
```bash
curl -s http://localhost:8180/health/ready
```

## Запуск всего стека

```bash
docker compose up -d
```

> Keycloak стартует первым. Сервисы `payments` и `storefront` ждут его healthcheck.

---

## Переменные окружения для локального запуска из IDE

Для запуска `storefront` локально (без Docker) необходимо задать:

```properties
KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/my-market
KEYCLOAK_TOKEN_URI=http://localhost:8180/realms/my-market/protocol/openid-connect/token
KEYCLOAK_CLIENT_ID=storefront-client
KEYCLOAK_CLIENT_SECRET=storefront-client-secret
```

Для `payments`:
```properties
KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/my-market
```

---

## Сброс данных Keycloak

Если нужно пересоздать realm с нуля:

```bash
docker compose stop keycloak
docker compose rm -f keycloak
docker compose up -d keycloak
```