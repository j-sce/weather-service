package com.weather_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather_service.client.WeatherDataClient;
import com.weather_service.handler.BadRequestException;
import com.weather_service.model.WeatherData;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherDataClient weatherDataClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String GEO_KEY = "weather";

    @Value("${weather.cache.ttl:5}")
    private Integer ttlMinutes;
    @Value("${weather.cache.distance:1.0}")
    private Double cacheDistance;

    public WeatherData getWeatherData(double lat, double lon) {
        // Check if data exists in cache within cache distance radius
        WeatherData cachedWeatherData = getCachedWeatherData(lat, lon);
        if (cachedWeatherData != null) {
            return cachedWeatherData;
        }

        // Get data from external API
        String jsonString = getWeather(lat, lon);
        WeatherData weatherData = parseWeatherData(jsonString);
        // Cache the fetched weather data
        cacheWeatherData(lat, lon, weatherData);

        return weatherData;
    }

    String getWeather(double lat, double lon) {
        log.debug("Getting weather data from OpenWeatherMap API");
        String weatherData = weatherDataClient.getWeatherData(lat, lon);

        if (weatherData == null) {
            log.error("Failed to retrieve weather data for lat: {} and lon: {}", lat, lon);
            throw new BadRequestException("No weather data found.");
        }

        return weatherData;
    }

    WeatherData parseWeatherData(String jsonString) {
        log.debug("Parsing weather data to WeatherData Object");
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonString);

            WeatherData weatherData = new WeatherData();

            // Extract latitude and longitude
            JsonNode coordNode = rootNode.path("coord");
            weatherData.setLatitude(coordNode.path("lat").asDouble());
            weatherData.setLongitude(coordNode.path("lon").asDouble());

            // Extract weather details
            List<Integer> weatherIds = new ArrayList<>();
            List<String> weatherDescriptions = new ArrayList<>();
            JsonNode weatherNode = rootNode.path(GEO_KEY);
            for (JsonNode weather : weatherNode) {
                weatherIds.add(weather.path("id").asInt());
                weatherDescriptions.add(weather.path("description").asText());
            }
            weatherData.setWeatherIds(weatherIds);
            weatherData.setWeatherDescriptions(weatherDescriptions);

            // Extract temperature, humidity, wind speed, and cloudiness
            JsonNode mainNode = rootNode.path("main");
            weatherData.setTemperature(mainNode.path("temp").asDouble());
            weatherData.setHumidity(mainNode.path("humidity").asInt());

            JsonNode windNode = rootNode.path("wind");
            weatherData.setWindSpeed(windNode.path("speed").asDouble());

            JsonNode cloudsNode = rootNode.path("clouds");
            weatherData.setCloudiness(cloudsNode.path("all").asInt());

            // Extract sunrise and sunset
            JsonNode sysNode = rootNode.path("sys");
            weatherData.setSunrise(convertTimestampToLocalDateTime(sysNode.path("sunrise").asLong()));
            weatherData.setSunset(convertTimestampToLocalDateTime(sysNode.path("sunset").asLong()));

            return weatherData;
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException while parsing weather data: {}", e.getMessage());
            throw new BadRequestException("Error occurred during deserialization");
        } catch (Exception e) {
            log.error("Unexpected error while parsing weather data: {}", e.getMessage());
            throw new BadRequestException("Unexpected error while parsing weather data");
        }
    }

    private static LocalDateTime convertTimestampToLocalDateTime(long timestamp) {
        return Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public void cacheWeatherData(double lat, double lon, WeatherData weatherData) {
        log.info("Caching weather data: {}", weatherData);
        GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();

        // Unique key for storing weather data and geospatial info
        String weatherKey = "weather:" + lat + ":" + lon;

        // Cache weather data
        redisTemplate.opsForValue().set(weatherKey, weatherData, Duration.ofMinutes(ttlMinutes));

        // Add geolocation
        geoOps.add(GEO_KEY, new Point(lon, lat), weatherKey);
        log.info("Cached weather data for lat={}, lon={}", lat, lon);
    }

    public WeatherData getCachedWeatherData(double lat, double lon) {
        log.info("Getting cached weather data.");
        try {
            GeoOperations<String, Object> geoOps = redisTemplate.opsForGeo();
            // Search within cacheDistance radius for cached weather data
            Circle circle = new Circle(new Point(lon, lat), new Distance(cacheDistance, RedisGeoCommands.DistanceUnit.KILOMETERS));
            GeoResults<RedisGeoCommands.GeoLocation<Object>> results = geoOps.radius(GEO_KEY, circle);

            if (results != null && !results.getContent().isEmpty()) {
                GeoResult<RedisGeoCommands.GeoLocation<Object>> closestResult = results.getContent().stream()
                        .min(Comparator.comparing(geoResult -> geoResult.getDistance().getValue())) // Find the closest result
                        .orElse(null);

                if (closestResult != null) {
                    String closestWeatherKey = (String) closestResult.getContent().getName();
                    log.info("Closest cached weather data found for key: {}", closestWeatherKey);
                    return (WeatherData) redisTemplate.opsForValue().get(closestWeatherKey);
                }
            }
            log.debug("No cached weather data found.");
            return null;
        } catch (Exception e) {
            log.error("Error retrieving cached weather data: {}", e.getMessage());
            return null;
        }
    }
}
