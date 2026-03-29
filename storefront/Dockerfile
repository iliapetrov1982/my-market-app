# -------- BUILD STAGE --------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle /workspace/
COPY gradle /workspace/gradle

COPY openapi /workspace/openapi
COPY storefront /workspace/storefront
COPY payments /workspace/payments

RUN chmod +x /workspace/gradlew

RUN ./gradlew :storefront:bootJar --no-daemon

# -------- RUNTIME STAGE --------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -m appuser
USER appuser

COPY --from=build /workspace/storefront/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]