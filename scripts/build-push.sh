#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}==========================================="
echo "Docker Build and Push Script"
echo -e "===========================================${NC}"
echo ""

# Project configuration
PROJECT_NAME="mini-java-app"

# Sanitize project name for Docker tag
IMAGE_NAME=$(echo "$PROJECT_NAME" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-' | sed 's/^-*//;s/-*$//')

echo -e "${YELLOW}Select Container Registry:${NC}"
echo "1. AWS ECR (Elastic Container Registry)"
echo "2. Docker Hub"
read -p "Enter choice (1 or 2): " REGISTRY_CHOICE

if [ "$REGISTRY_CHOICE" = "1" ]; then
    echo -e "\n${GREEN}AWS ECR Configuration${NC}"
    
    # Prompt for AWS region
    read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
    if [ -z "$AWS_REGION" ]; then
        echo -e "${RED}Error: AWS Region is required${NC}"
        exit 1
    fi
    
    # Get AWS Account ID
    echo -e "${YELLOW}Retrieving AWS Account ID...${NC}"
    AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    if [ -z "$AWS_ACCOUNT_ID" ]; then
        echo -e "${RED}Error: Failed to retrieve AWS Account ID. Please check AWS CLI configuration.${NC}"
        exit 1
    fi
    echo -e "${GREEN}AWS Account ID: $AWS_ACCOUNT_ID${NC}"
    
    # Prompt for ECR repository name
    read -p "Enter ECR Repository Name (default: $IMAGE_NAME): " ECR_REPO
    ECR_REPO=${ECR_REPO:-$IMAGE_NAME}
    
    # Construct registry URL
    REGISTRY_URL="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
    
    # Prompt for image tag
    read -p "Enter image tag (default: latest): " IMAGE_TAG
    IMAGE_TAG=${IMAGE_TAG:-latest}
    
    # Sanitize tag
    IMAGE_TAG=$(echo "$IMAGE_TAG" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9.-' '-' | sed 's/^-*//;s/-*$//')
    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="latest"
    fi
    
    FULL_IMAGE_NAME="$REGISTRY_URL/$ECR_REPO:$IMAGE_TAG"
    
    # Login to ECR
    echo -e "\n${YELLOW}Logging in to AWS ECR...${NC}"
    aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $REGISTRY_URL
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: ECR login failed${NC}"
        exit 1
    fi
    echo -e "${GREEN}Successfully logged in to ECR${NC}"
    
    # Check if repository exists, create if not
    echo -e "${YELLOW}Checking if ECR repository exists...${NC}"
    aws ecr describe-repositories --repository-names $ECR_REPO --region $AWS_REGION >/dev/null 2>&1 || {
        echo -e "${YELLOW}Repository does not exist. Creating ECR repository...${NC}"
        aws ecr create-repository --repository-name $ECR_REPO --region $AWS_REGION
        echo -e "${GREEN}ECR repository created successfully${NC}"
    }
    
elif [ "$REGISTRY_CHOICE" = "2" ]; then
    echo -e "\n${GREEN}Docker Hub Configuration${NC}"
    
    # Prompt for Docker Hub credentials
    read -p "Enter Docker Hub Username: " DOCKER_USERNAME
    if [ -z "$DOCKER_USERNAME" ]; then
        echo -e "${RED}Error: Docker Hub username is required${NC}"
        exit 1
    fi
    
    read -sp "Enter Docker Hub Password/Token: " DOCKER_PASSWORD
    echo ""
    if [ -z "$DOCKER_PASSWORD" ]; then
        echo -e "${RED}Error: Docker Hub password is required${NC}"
        exit 1
    fi
    
    # Prompt for repository name
    read -p "Enter Repository Name (default: $IMAGE_NAME): " REPO_NAME
    REPO_NAME=${REPO_NAME:-$IMAGE_NAME}
    
    # Prompt for image tag
    read -p "Enter image tag (default: latest): " IMAGE_TAG
    IMAGE_TAG=${IMAGE_TAG:-latest}
    
    # Sanitize tag
    IMAGE_TAG=$(echo "$IMAGE_TAG" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9.-' '-' | sed 's/^-*//;s/-*$//')
    if [ -z "$IMAGE_TAG" ]; then
        IMAGE_TAG="latest"
    fi
    
    FULL_IMAGE_NAME="$DOCKER_USERNAME/$REPO_NAME:$IMAGE_TAG"
    
    # Login to Docker Hub
    echo -e "\n${YELLOW}Logging in to Docker Hub...${NC}"
    echo $DOCKER_PASSWORD | docker login --username $DOCKER_USERNAME --password-stdin
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Docker Hub login failed${NC}"
        exit 1
    fi
    echo -e "${GREEN}Successfully logged in to Docker Hub${NC}"
    
else
    echo -e "${RED}Invalid choice. Exiting.${NC}"
    exit 1
fi

# Build Docker image
echo -e "\n${YELLOW}Building Docker image: $FULL_IMAGE_NAME${NC}"
echo -e "${YELLOW}Build context: $(pwd)${NC}"

docker build -t $FULL_IMAGE_NAME .

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Docker build failed${NC}"
    exit 1
fi

echo -e "${GREEN}Docker image built successfully${NC}"

# Push Docker image
echo -e "\n${YELLOW}Pushing Docker image to registry...${NC}"
docker push $FULL_IMAGE_NAME

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Docker push failed${NC}"
    exit 1
fi

echo -e "\n${GREEN}==========================================="
echo "Build and Push Completed Successfully!"
echo -e "===========================================${NC}"
echo -e "${GREEN}Image: $FULL_IMAGE_NAME${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Use this image URI for ECS deployment"
echo "2. Update task definition with this image"
echo "3. Run deploy-image.sh to deploy to ECS"
echo ""
