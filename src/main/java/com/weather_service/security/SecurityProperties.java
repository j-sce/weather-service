package com.weather_service.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("security")
public class SecurityProperties {

    @NotBlank
    private String adminPassword;

    @NotBlank
    private String privateKey;

    @NotBlank
    private String publicKey;

    @Positive
    private long tokenExpiration;
}
