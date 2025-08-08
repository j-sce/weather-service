package com.weather_service.integration;

import com.weather_service.TestcontainersConfiguration;
import com.weather_service.client.WeatherDataClient;
import com.weather_service.model.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(initializers = TestcontainersConfiguration.Initializer.class)
class WeatherServiceAppEndToEndTest {

    private static final String URL = "/api/weather";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherDataClient weatherDataClient;

    @Autowired
    private CacheManager cacheManager;

    private WeatherData weatherData;
    String weatherDataString = "{\"coord\":{\"lon\":-16.181418,\"lat\":64.049075},\"weather\":[{\"id\":800,\"main\":\"Clear\",\"description\":\"clear sky\",\"icon\":\"01d\"}],\"base\":\"stations\",\"main\":{\"temp\":15.00,\"feels_like\":21.15,\"temp_min\":21.63,\"temp_max\":22.67,\"pressure\":1006,\"humidity\":85,\"sea_level\":1006,\"grnd_level\":1005},\"visibility\":10000,\"wind\":{\"speed\":5.5,\"deg\":170},\"clouds\":{\"all\":50},\"dt\":1725964611,\"sys\":{\"type\":2,\"id\":2075320,\"country\":\"LV\",\"sunrise\":1725939874,\"sunset\":1725987378},\"timezone\":10800,\"id\":6615326,\"name\":\"Vecr?ga\",\"cod\":200}";
    private String token;

    @BeforeEach
    void clearCache(){
        cacheManager.getCacheNames()
                .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
    }

    @BeforeEach
    void setUp() {
        weatherData = new WeatherData();
        weatherData.setLatitude(64.049075);
        weatherData.setLongitude(-16.181418);
        weatherData.setTemperature(15.0);
        weatherData.setHumidity(85);
        weatherData.setWindSpeed(5.5);
        weatherData.setCloudiness(50);
        weatherData.setSunrise(LocalDateTime.now().minusHours(5));
        weatherData.setSunset(LocalDateTime.now().plusHours(5));

        token = "Bearer test-token";
    }

    @Test
    void testGetWeather_NoWeatherDataFound() throws Exception {
        when(weatherDataClient.getWeatherData(anyDouble(),anyDouble())).thenReturn(null);

        mockMvc.perform(get(URL)
                        .header("Authorization", token)
                        .param("lat", "44.2")
                        .param("lon", "-37.1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No weather data found."));

        verify(weatherDataClient, times(1)).getWeatherData(anyDouble(),anyDouble());
    }

    @Test
    void testGetWeather_Success() throws Exception {
        when(weatherDataClient.getWeatherData(anyDouble(),anyDouble())).thenReturn(weatherDataString);

        mockMvc.perform(get(URL)
                        .header("Authorization", token)
                        .param("lat", "64.049075")
                        .param("lon", "-16.181418"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.latitude").value(weatherData.getLatitude()))
                .andExpect(jsonPath("$.longitude").value(weatherData.getLongitude()))
                .andExpect(jsonPath("$.temperature").value(weatherData.getTemperature()))
                .andExpect(jsonPath("$.humidity").value(weatherData.getHumidity()))
                .andExpect(jsonPath("$.windSpeed").value(weatherData.getWindSpeed()))
                .andExpect(jsonPath("$.cloudiness").value(weatherData.getCloudiness()));

        verify(weatherDataClient, times(1)).getWeatherData(anyDouble(),anyDouble());
    }

    @Test
    void testGetWeather_NoToken() throws Exception {
        mockMvc.perform(get(URL)
                        .param("lat", "64.049075")
                        .param("lon", "-16.181418"))
                .andExpect(status().isBadRequest());

        verify(weatherDataClient, times(0)).getWeatherData(anyDouble(),anyDouble());
    }

    @Test
    void testGetWeather_BadRequest() throws Exception {

        mockMvc.perform(get(URL)
                        .header("Authorization", token)
                        .param("lat", "invalid_latitude")
                        .param("lon", "-16.181418"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetWeatherData_CacheHit() throws Exception {

        cacheManager.getCache("weatherData").put("weather:64.049075:-16.181418", weatherData);

        mockMvc.perform(get(URL)
                        .header("Authorization", token)
                        .param("lat", "64.049075")
                        .param("lon", "-16.181418"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.latitude").value(weatherData.getLatitude()))
                .andExpect(jsonPath("$.longitude").value(weatherData.getLongitude()));

        verify(weatherDataClient, times(0)).getWeatherData(anyDouble(), anyDouble());
    }

    @Test
    void testGetWeatherData_CacheDistanceBoundary() throws Exception {
        when(weatherDataClient.getWeatherData(anyDouble(), anyDouble())).thenReturn(weatherDataString);

        cacheManager.getCache("weatherData").put("weather:64.049075:-16.181418", weatherData);

        mockMvc.perform(get(URL)
                        .header("Authorization", token)
                        .param("lat", "64.049075")
                        .param("lon", "-16.181418"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.latitude").value(weatherData.getLatitude()))
                .andExpect(jsonPath("$.longitude").value(weatherData.getLongitude()));

        verify(weatherDataClient, times(0)).getWeatherData(anyDouble(), anyDouble());
    }

    @Test
    void testGetWeatherData_InvalidJson() throws Exception {
        when(weatherDataClient.getWeatherData(anyDouble(), anyDouble())).thenReturn("invalid json");

        mockMvc.perform(get(URL)
                        .header("Authorization", token)
                        .param("lat", "80.0")
                        .param("lon", "-20.0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error occurred during deserialization"));

        verify(weatherDataClient, times(1)).getWeatherData(anyDouble(), anyDouble());
    }
}
