package com.weather_service.model;

import com.weather_service.swagger.DescriptionVariables;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WeatherData implements Serializable {

    @Schema(description = "Latitude of location", example = "56.9710")
    @Range(min = -90, max = 90, message = DescriptionVariables.LATITUDE_RANGE)
    private Double latitude;

    @Schema(description = "Longitude of location", example = "24.1604")
    @Range(min = -180, max = 180, message = DescriptionVariables.LONGITUDE_RANGE)
    private Double longitude;

    @Schema(description = "List of weather condition id", example = "[741, 701]")
    private List<Integer> weatherIds;

    @Schema(description = "List of weather condition descriptions", example = "[fog, mist]")
    private List<String> weatherDescriptions;

    @Schema(description = "Temperature, °C", example = "21.04")
    private Double temperature;

    @Schema(description = "Humidity, %", example = "73")
    private Integer humidity;

    @Schema(description = "Wind speed, m/s", example = "2.31")
    private Double windSpeed;

    @Schema(description = "Cloudiness, %", example = "66")
    private Integer cloudiness;

    @Schema(description = "Sunrise time, (system default time-zone)", example = "2024-08-28T06:18:07")
    private LocalDateTime sunrise;

    @Schema(description = "Sunset time, (system default time-zone)", example = "2024-08-28T20:31:22")
    private LocalDateTime sunset;
}
