package com.weather_service.controller;

import com.weather_service.handler.GlobalExceptionHandler;
import com.weather_service.model.WeatherData;
import com.weather_service.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WeatherControllerTest {

    private static final String URL = "/api/weather";

    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private WeatherController weatherController;

    private MockMvc mockMvc;

    private WeatherData weatherData;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(weatherController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @BeforeEach
    void initializeWeatherData(){
        weatherData = new WeatherData();
        weatherData.setLatitude(64.049075);
        weatherData.setLongitude(-16.181418);
        weatherData.setTemperature(15.0);
        weatherData.setHumidity(85);
        weatherData.setWindSpeed(5.5);
        weatherData.setCloudiness(50);
        weatherData.setSunrise(LocalDateTime.now().minusHours(5));
        weatherData.setSunset(LocalDateTime.now().plusHours(5));
    }

    @Test
    void testGetWeather_Success() throws Exception {
        when(weatherService.getWeatherData(64.049075, -16.181418)).thenReturn(weatherData);

        mockMvc.perform(get(URL)
                        .param("lat", "64.049075")
                        .param("lon", "-16.181418"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.latitude").value(64.049075))
                .andExpect(jsonPath("$.longitude").value(-16.181418))
                .andExpect(jsonPath("$.temperature").value(15.0))
                .andExpect(jsonPath("$.humidity").value(85))
                .andExpect(jsonPath("$.windSpeed").value(5.5))
                .andExpect(jsonPath("$.cloudiness").value(50));

        verify(weatherService, times(1)).getWeatherData(64.049075, -16.181418);
    }

    @Test
    void testGetWeather_BadRequest() throws Exception {
        mockMvc.perform(get(URL)
                        .param("lat", "invalid_latitude")
                        .param("lon", "-16.181418"))
                .andExpect(status().isBadRequest());

        verify(weatherService, never()).getWeatherData(anyDouble(), anyDouble());
    }

    @Test
    void testGetWeather_ServiceThrowsException() throws Exception {
        when(weatherService.getWeatherData(64.049075, -16.181418)).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get(URL)
                        .param("lat", "64.049075")
                        .param("lon", "-16.181418"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred. Please try again later."));

        verify(weatherService, times(1)).getWeatherData(64.049075, -16.181418);
    }
}
