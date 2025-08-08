package com.weather_service.integration;

import com.weather_service.TestcontainersConfiguration;
import com.weather_service.caching.CacheConfig;
import com.weather_service.client.WeatherDataClient;
import com.weather_service.handler.BadRequestException;
import com.weather_service.model.WeatherData;
import com.weather_service.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = TestcontainersConfiguration.Initializer.class)
@Import({CacheConfig.class, WeatherService.class})
@EnableCaching
@ImportAutoConfiguration(classes = {
        CacheAutoConfiguration.class,
        RedisAutoConfiguration.class
})
class WeatherDataCachingIntegrationTest {

    @MockitoBean
    private WeatherDataClient weatherDataClient;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames()
                .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
        ReflectionTestUtils.setField(weatherService, "ttlMinutes", 1);
    }

    @Test
    void whenGetWeatherData_thenItemReturnedFromCache_Success() {
        double lat = 40.7128;
        double lon = -74.0060;
        WeatherData expectedWeatherData = new WeatherData();
        expectedWeatherData.setLatitude(lat);
        expectedWeatherData.setLongitude(lon);
        expectedWeatherData.setTemperature(25.0);
        expectedWeatherData.setHumidity(50);
        expectedWeatherData.setWindSpeed(5.0);
        expectedWeatherData.setCloudiness(0);
        expectedWeatherData.setWeatherIds(List.of(800));
        expectedWeatherData.setWeatherDescriptions(List.of("clear sky"));
        expectedWeatherData.setSunrise(LocalDateTime.of(2021, 10, 1, 10, 20));
        expectedWeatherData.setSunset(LocalDateTime.of(2021, 10, 1, 22, 20));

        when(weatherDataClient.getWeatherData(lat, lon)).thenReturn(
                "{\"coord\": {\"lat\": 40.7128, \"lon\": -74.0060}, \"weather\": [{\"id\": 800, \"description\": \"clear sky\"}], " +
                        "\"main\": {\"temp\": 25, \"humidity\": 50}, " +
                        "\"wind\": {\"speed\": 5}, " +
                        "\"clouds\": {\"all\": 0}, " +
                        "\"sys\": {\"sunrise\": 1633072800, \"sunset\": 1633116000}}"
        );

        WeatherData cacheMiss = weatherService.getWeatherData(lat, lon);
        assertEquals(expectedWeatherData, cacheMiss);

        WeatherData cacheHit = weatherService.getWeatherData(lat, lon);
        assertEquals(expectedWeatherData, cacheHit);

        verify(weatherDataClient, times(1)).getWeatherData(lat, lon);
    }

    @Test
    void whenGetWeatherData_nonExistent_thenCacheNotPopulated_Failure() {
        double lat = 999.0; // Invalid latitude
        double lon = 999.0; // Invalid longitude

        when(weatherDataClient.getWeatherData(anyDouble(), anyDouble())).thenThrow(new BadRequestException("No weather data found."));

        weatherService.getWeatherData(lat, lon);

        assertNull(Objects.requireNonNull(cacheManager.getCache("weather")).get("weather:" + lat + ":" + lon));
        verify(weatherDataClient, times(1)).getWeatherData(lat, lon);
    }

    @Test
    void whenGetWeatherDataWithin1KmRadius_thenCorrectDataReturned() {
        // Point A
        double lat1 = 40.7128;
        double lon1 = -74.0060;
        // Point B (within 1km)
        double lat2 = 40.7138;
        double lon2 = -74.0050;

        WeatherData weatherData = new WeatherData();
        weatherData.setLatitude(lat1);
        weatherData.setLongitude(lon1);
        weatherData.setTemperature(25.0);
        weatherData.setHumidity(50);
        weatherData.setWindSpeed(5.0);
        weatherData.setCloudiness(0);
        weatherData.setWeatherIds(List.of(800));
        weatherData.setWeatherDescriptions(List.of("clear sky"));

        // Cache the weather data
        weatherService.cacheWeatherData(lat1, lon1, weatherData);

        // Retrieve weather data from a point that is within 1km radius of lat2 and lon2
        WeatherData retrievedWeatherData = weatherService.getCachedWeatherData(lat2, lon2);

        assertNotNull(retrievedWeatherData);
        assertEquals(weatherData.getTemperature(), retrievedWeatherData.getTemperature());
        assertEquals(weatherData.getHumidity(), retrievedWeatherData.getHumidity());
        assertEquals(weatherData.getWindSpeed(), retrievedWeatherData.getWindSpeed());
        assertEquals(weatherData.getCloudiness(), retrievedWeatherData.getCloudiness());
        assertEquals(weatherData.getWeatherIds(), retrievedWeatherData.getWeatherIds());
        assertEquals(weatherData.getWeatherDescriptions(), retrievedWeatherData.getWeatherDescriptions());

        // Verify that the cached data for the first point is also retrievable
        WeatherData retrievedWeatherData1 = weatherService.getCachedWeatherData(lat1, lon1);
        assertNotNull(retrievedWeatherData1);
        assertEquals(weatherData.getTemperature(), retrievedWeatherData1.getTemperature());
    }

    @Test
    void whenMultipleWeatherDataInCache_thenCorrectClosestWeatherKeyReturned() {
        double latReference = 54.89524; // Reference point
        double lonReference = 23.956237; // Reference point

        WeatherData weatherData1 = new WeatherData();
        weatherData1.setLatitude(54.89178); // Close to the reference point
        weatherData1.setLongitude(23.95768);
        weatherData1.setTemperature(20.0);
        weatherData1.setHumidity(50);
        weatherData1.setWindSpeed(5.0);
        weatherData1.setCloudiness(0);

        WeatherData weatherData2 = new WeatherData();
        weatherData2.setLatitude(54.9009); // Slightly further but still within 1km
        weatherData2.setLongitude(23.9479);
        weatherData2.setTemperature(25.0);
        weatherData2.setHumidity(60);
        weatherData2.setWindSpeed(3.0);
        weatherData2.setCloudiness(10);

        // Cache the weather data
        weatherService.cacheWeatherData(weatherData1.getLatitude(), weatherData1.getLongitude(), weatherData1);
        weatherService.cacheWeatherData(weatherData2.getLatitude(), weatherData2.getLongitude(), weatherData2);

        // Retrieve weather data from the reference point
        WeatherData retrievedWeatherData = weatherService.getCachedWeatherData(latReference, lonReference);

        assertNotNull(retrievedWeatherData);
        assertEquals(weatherData1.getTemperature(), retrievedWeatherData.getTemperature());
        assertEquals(weatherData1.getHumidity(), retrievedWeatherData.getHumidity());
        assertEquals(weatherData1.getWindSpeed(), retrievedWeatherData.getWindSpeed());
        assertEquals(weatherData1.getCloudiness(), retrievedWeatherData.getCloudiness());

        // Assert that the correct closest key was used
        String closestWeatherKey = "weather:" + weatherData1.getLatitude() + ":" + weatherData1.getLongitude();
        assertEquals("weather:54.89178:23.95768", closestWeatherKey);
    }
}
