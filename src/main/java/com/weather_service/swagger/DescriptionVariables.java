package com.weather_service.swagger;

public class DescriptionVariables {

    public static final String LATITUDE_RANGE = "Latitude values range between -90 and +90 degrees";
    public static final String LONGITUDE_RANGE = "Longitude values range between -180 and +180 degrees";
    public static final String WEATHER = "Weather Controller";

    private DescriptionVariables() {
        throw new IllegalStateException("Utility class");
    }
}
