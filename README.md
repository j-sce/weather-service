# 🌦️ Weather Service

A Spring Boot-based microservice for fetching, caching, and serving weather data. The service integrates with an external weather API and caches data in Redis for efficient retrieval. Infrastructure is provisioned on AWS using Terraform, and deployments are automated using Jenkins CI/CD.

---

## 🚀 Features

- Fetch weather data by latitude and longitude.
- Cache responses in Redis with a configurable TTL.
- Support for retrieving weather data within a 1km radius.
- JWT-based authentication for API access.
- Integration and end-to-end tests with Redis support.
- Infrastructure as Code using Terraform (EC2 + SSM).
- Dockerized deployment and ECR integration.
- Jenkins-based CI/CD pipeline.