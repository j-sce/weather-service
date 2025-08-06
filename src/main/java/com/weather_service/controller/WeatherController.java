package com.weather_service.controller;

import com.weather_service.model.WeatherData;
import com.weather_service.service.WeatherService;
import com.weather_service.swagger.DescriptionVariables;
import com.weather_service.swagger.HTTPResponseMessages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/api/weather")
@Tag(name = DescriptionVariables.WEATHER)
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @Operation(summary = "Gets weather data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = HTTPResponseMessages.HTTP_200,
                    content = @Content(schema = @Schema(implementation = WeatherData.class))),
            @ApiResponse(responseCode = "400", description = HTTPResponseMessages.HTTP_400, content = @Content),
            @ApiResponse(responseCode = "403", description = HTTPResponseMessages.HTTP_403, content = @Content),
            @ApiResponse(responseCode = "404", description = HTTPResponseMessages.HTTP_404, content = @Content),
            @ApiResponse(responseCode = "500", description = HTTPResponseMessages.HTTP_500, content = @Content)
    })
    @GetMapping
    public ResponseEntity<WeatherData> getWeather(
            @Parameter(name = "lat", description = "latitude", example = "64.049075", required = true) @RequestParam Double lat,
            @Parameter(name = "lon", description = "longitude", example = "-16.181418", required = true) @RequestParam Double lon) {
        log.info("Getting weather data");
        return ResponseEntity.ok(weatherService.getWeatherData(lat, lon));
    }
}
