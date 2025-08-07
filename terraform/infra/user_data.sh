#!/bin/bash

set -e

# Update package list and install prerequisites
apt-get update -y
apt-get install -y docker.io unzip curl jq

curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Enable Docker service
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

region="${region}"
account_id="${account_id}"
image_tag="${image_tag}"
repo="${repo}"

echo "[INFO] Logging in to ECR..."
aws ecr get-login-password --region $region | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.$region.amazonaws.com

echo "[INFO] Writing docker-compose.yml..."
cat > /home/ubuntu/docker-compose.yml <<'EOF'
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
cd /home/ubuntu
docker compose up -d

echo "[INFO] Deployment completed."
