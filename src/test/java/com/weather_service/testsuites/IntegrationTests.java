package com.weather_service.testsuites;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({"com.weather_service.integration"})
public class IntegrationTests {
}
