# =========================
#        .dockerignore
# =========================
#.git
#.gitignore
#.gradle
#build
#.idea
#*.iml
#node_modules
#docker-compose.yml
#README.md


# =========================
#        Dockerfile
# =========================

# -------- BUILD STAGE --------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 1️⃣ Копируем только gradle-файлы (чтобы кешировались зависимости)
COPY ../gradlew settings.gradle build.gradle /workspace/
COPY ../gradle /workspace/gradle

# Делаем gradlew исполняемым
RUN chmod +x gradlew

# 2️⃣ Скачиваем зависимости (этот слой будет кешироваться)
RUN ./gradlew dependencies --no-daemon

# 3️⃣ Теперь копируем исходники
COPY src /workspace/src

# 4️⃣ Собираем приложение
RUN ./gradlew clean bootJar --no-daemon


# -------- RUNTIME STAGE --------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Создаём non-root пользователя
RUN useradd -m appuser
USER appuser

# Копируем только jar
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
