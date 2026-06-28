# crm-service Docker image (Java 17)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8083 8084

ENV SPRING_PROFILES_ACTIVE=prod \
    SPRING_DATASOURCE_URL=jdbc:postgresql://192.168.55.223:5432/user_db \
    SPRING_DATASOURCE_USERNAME="" \
    SPRING_DATASOURCE_PASSWORD="" \
    JAVA_TOOL_OPTIONS="-Xmx512m"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
