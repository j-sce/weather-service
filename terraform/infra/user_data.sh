#!/bin/bash

set -e

echo "[INFO] Installing Docker and Docker Compose..."
apt-get update -y
apt-get install -y docker.io curl jq

echo "[INFO] Starting Docker..."
systemctl enable docker
systemctl start docker

echo "[INFO] Authenticating with ECR..."
TOKEN=$(curl -X PUT http://169.254.169.254/latest/api/token \
    -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
ROLE_NAME=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/iam/security-credentials/)
CREDS=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/iam/security-credentials/$ROLE_NAME)

AWS_ACCESS_KEY_ID=$(echo "$CREDS" | jq -r .AccessKeyId)
AWS_SECRET_ACCESS_KEY=$(echo "$CREDS" | jq -r .SecretAccessKey)
AWS_SESSION_TOKEN=$(echo "$CREDS" | jq -r .Token)

export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_SESSION_TOKEN

REGION="${region}"
ACCOUNT_ID="${account_id}"
TAG="${image_tag}"
REPO="weather-service"

echo "[INFO] Logging in to ECR..."
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.$REGION.amazonaws.com

echo "[INFO] Writing docker-compose.yml..."
cat > /home/ubuntu/docker-compose.yml <<EOF
services:
  redis:
    image: redis:latest
    ports:
      - "6379:6379"
    networks:
      - weather-net

  app:
    image: ${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${REPO}:${TAG}
    container_name: app
    ports:
      - "9090:9090"
    environment:
      - REDIS_HOST=redis
    networks:
      - weather-net
    depends_on:
      - redis

networks:
  weather-net:
EOF

echo "[INFO] Running docker-compose up -d..."
cd /home/ubuntu
docker compose up -d

echo "[INFO] Deployment completed."
