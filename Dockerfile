# One image per service: docker build --build-arg MODULE=mall-product -t sample-mall/mall-product .
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:17-jre
ARG MODULE
WORKDIR /app
COPY --from=build /src/${MODULE}/target/${MODULE}-0.1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
