#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "${GREEN}=== Docker Build and Push Script ===${NC}"
echo ""

# Project configuration
PROJECT_NAME="mini-java-app"

# Sanitize image name: lowercase, replace invalid chars with hyphens, trim leading/trailing hyphens
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

echo "Project: $PROJECT_NAME"
echo "Sanitized Image Name: $IMAGE_NAME"
echo ""

# Prompt for image tag
read -p "Enter image tag (default: latest): " IMAGE_TAG
IMAGE_TAG=${IMAGE_TAG:-latest}

# Sanitize tag: lowercase, replace invalid chars with hyphens, trim leading/trailing hyphens
IMAGE_TAG=$(echo "$IMAGE_TAG" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9.-' '-' | sed 's/^-*//;s/-*$//')

# Default to 'latest' if tag becomes empty after sanitization
if [ -z "$IMAGE_TAG" ]; then
    IMAGE_TAG="latest"
fi

echo "Sanitized Image Tag: $IMAGE_TAG"
echo ""

# Registry selection
echo "Select Docker Registry:"
echo "1. AWS ECR (Elastic Container Registry)"
echo "2. Docker Hub"
read -p "Enter choice (1 or 2): " REGISTRY_CHOICE

if [ "$REGISTRY_CHOICE" = "1" ]; then
    echo ""
    echo "${YELLOW}=== AWS ECR Configuration ===${NC}"
    
    # AWS ECR configuration
    read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
    read -p "Enter AWS Account ID: " AWS_ACCOUNT_ID
    read -p "Enter ECR Repository Name (default: $IMAGE_NAME): " ECR_REPO
    ECR_REPO=${ECR_REPO:-$IMAGE_NAME}
    
    REGISTRY_URL="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
    FULL_IMAGE_NAME="$REGISTRY_URL/$ECR_REPO:$IMAGE_TAG"
    
    echo ""
    echo "Full Image Name: $FULL_IMAGE_NAME"
    echo ""
    
    # Authenticate with AWS ECR
    echo "${YELLOW}Authenticating with AWS ECR...${NC}"
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URL"
    
    if [ $? -ne 0 ]; then
        echo "${RED}Failed to authenticate with AWS ECR${NC}"
        exit 1
    fi
    
    echo "${GREEN}Successfully authenticated with AWS ECR${NC}"
    echo ""
    
    # Check if ECR repository exists, create if it doesn't
    echo "${YELLOW}Checking ECR repository...${NC}"
    aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$AWS_REGION" >/dev/null 2>&1 || {
        echo "${YELLOW}Repository does not exist. Creating ECR repository: $ECR_REPO${NC}"
        aws ecr create-repository --repository-name "$ECR_REPO" --region "$AWS_REGION"
        echo "${GREEN}ECR repository created successfully${NC}"
    }
    echo ""
    
elif [ "$REGISTRY_CHOICE" = "2" ]; then
    echo ""
    echo "${YELLOW}=== Docker Hub Configuration ===${NC}"
    
    # Docker Hub configuration
    read -p "Enter Docker Hub Username: " DOCKER_USERNAME
    read -sp "Enter Docker Hub Password or Access Token: " DOCKER_PASSWORD
    echo ""
    
    FULL_IMAGE_NAME="$DOCKER_USERNAME/$IMAGE_NAME:$IMAGE_TAG"
    
    echo ""
    echo "Full Image Name: $FULL_IMAGE_NAME"
    echo ""
    
    # Authenticate with Docker Hub
    echo "${YELLOW}Authenticating with Docker Hub...${NC}"
    echo "$DOCKER_PASSWORD" | docker login --username "$DOCKER_USERNAME" --password-stdin
    
    if [ $? -ne 0 ]; then
        echo "${RED}Failed to authenticate with Docker Hub${NC}"
        exit 1
    fi
    
    echo "${GREEN}Successfully authenticated with Docker Hub${NC}"
    echo ""
    
else
    echo "${RED}Invalid choice. Exiting.${NC}"
    exit 1
fi

# Build Docker image
echo "${YELLOW}Building Docker image...${NC}"
echo "Running: docker build -t $FULL_IMAGE_NAME ."
echo ""

docker build -t "$FULL_IMAGE_NAME" .

if [ $? -ne 0 ]; then
    echo "${RED}Docker build failed${NC}"
    exit 1
fi

echo ""
echo "${GREEN}Docker image built successfully: $FULL_IMAGE_NAME${NC}"
echo ""

# Push Docker image
echo "${YELLOW}Pushing Docker image to registry...${NC}"
docker push "$FULL_IMAGE_NAME"

if [ $? -ne 0 ]; then
    echo "${RED}Docker push failed${NC}"
    exit 1
fi

echo ""
echo "${GREEN}=== Build and Push Completed Successfully ===${NC}"
echo "Image: $FULL_IMAGE_NAME"
echo ""
echo "Next steps:"
echo "1. Use this image URI in your Kubernetes deployment"
echo "2. Run deploy-image.sh to deploy to AWS EKS"
echo ""
