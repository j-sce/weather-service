package com.weather_service.client;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

@Log4j2
@Component
public class WeatherDataClient {

    @Value("${openweathermap.api.url}")
    private String weatherApiUrl;

    @Value("${openweathermap.api.key}")
    private String apiKey;

    private final WebClient webClient;

    @Autowired
    public WeatherDataClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public String getWeatherData(double lat, double lon) {
        URI uri = UriComponentsBuilder.fromUriString(weatherApiUrl)
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .build()
                .toUri();

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.empty())
                .block();
    }
}
