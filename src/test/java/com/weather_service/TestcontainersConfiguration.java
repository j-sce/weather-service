package com.weather_service;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@Configuration
@ContextConfiguration(initializers = TestcontainersConfiguration.Initializer.class)
public class TestcontainersConfiguration {

    static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379);

    static {
        redisContainer.start();
    }


    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            String redisHost = redisContainer.getHost();
            Integer redisPort = redisContainer.getFirstMappedPort();

            TestPropertyValues.of(
                    "spring.data.redis.host=" + redisHost,
                    "spring.data.redis.port=" + redisPort
            ).applyTo(context.getEnvironment());
        }
    }
}
