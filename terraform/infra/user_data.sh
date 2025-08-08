#!/bin/bash

set -e

echo "[INFO] Installing Docker and Docker Compose..."
dnf update -y
dnf install -y docker jq

echo "[INFO] Starting Docker..."
systemctl enable docker
systemctl start docker

echo "[INFO] Installing Docker Compose Plugin (v2)..."
mkdir -p ~/.docker/cli-plugins/
curl -SL https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64 \
  -o ~/.docker/cli-plugins/docker-compose
chmod +x ~/.docker/cli-plugins/docker-compose

# Optional: verify
docker compose version

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

region="${region}"
account_id="${account_id}"
image_tag="${image_tag}"
repo="${repo}"

#echo "[INFO] Installing AWS CLI..."
#dnf install -y awscli

echo "[INFO] Logging in to ECR..."
aws ecr get-login-password --region $region | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.$region.amazonaws.com

echo "[INFO] Writing docker-compose.yml..."
cat > /home/ec2-user/docker-compose.yml <<EOF
services:
  redis:
    image: redis:latest
    ports:
      - "6379:6379"
    networks:
      - weather-net

  app:
    image: ${account_id}.dkr.ecr.${region}.amazonaws.com/${repo}:${image_tag}
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
cd /home/ec2-user
docker compose up -d

echo "[INFO] Deployment completed."
