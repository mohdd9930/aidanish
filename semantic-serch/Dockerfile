# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

# Build the app
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Spring Boot listens on this port (matches application.properties)
EXPOSE 5000

# Memory-tune for small free-tier hosts (512 MB).
# Override at runtime with: docker run -e JAVA_OPTS="-Xmx1g" ...
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
