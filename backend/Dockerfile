FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/
RUN ./mvnw -B -DskipTests clean package \
    && cp target/backend-0.0.1-SNAPSHOT.jar app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S basilico && adduser -S basilico -G basilico

COPY --from=build /workspace/app.jar app.jar

USER basilico
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
