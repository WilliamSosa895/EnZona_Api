# ---------- Etapa 1: compilación ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- Etapa 2: ejecución (sin herramientas de compilación) ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S enzona && adduser -S enzona -G enzona
COPY --from=build /app/target/enzona-api-*.jar app.jar
USER enzona
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
