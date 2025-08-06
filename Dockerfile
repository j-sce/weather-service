FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean build -x test

FROM amazoncorretto:21-alpine
ENV LC_ALL=C.UTF-8
ENV TZ=Europe/Minsk
RUN ln -snf "/usr/share/zoneinfo/$TZ" /etc/localtime && echo "$TZ" > /etc/timezone
WORKDIR /app
COPY --from=build /app/build/libs/weather-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]