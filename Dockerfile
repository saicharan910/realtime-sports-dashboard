# Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S sports && adduser -S sports -G sports
COPY --from=build /app/target/*.jar app.jar
RUN chown sports:sports app.jar
USER sports
EXPOSE 8000
ENTRYPOINT ["java","-jar","app.jar"]
