#!/bin/bash
set -e
set -o pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "${GREEN}=== AWS EKS Deployment Script ===${NC}"
echo ""

# Prompt for AWS configuration
read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
    echo "${RED}AWS Region is required${NC}"
    exit 1
fi

read -p "Enter EKS Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
    echo "${RED}EKS Cluster Name is required${NC}"
    exit 1
fi

echo ""
read -p "Enter Docker Image URI (with tag, e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
    echo "${RED}Docker Image URI is required${NC}"
    exit 1
fi

echo ""
echo "${YELLOW}=== Environment Variables Configuration ===${NC}"
echo "Please provide values for the following environment variables."
echo "Press Enter to skip optional variables."
echo ""

# Database configuration
read -p "Enter DB_URL (e.g., jdbc:mysql://db-host:3306/mini_app_db): " DB_URL
DB_URL=${DB_URL:-jdbc:mysql://localhost:3306/mini_app_db}

read -p "Enter DB_USERNAME (default: root): " DB_USERNAME
DB_USERNAME=${DB_USERNAME:-root}

read -sp "Enter DB_PASSWORD: " DB_PASSWORD
echo ""
DB_PASSWORD=${DB_PASSWORD:-password123}

# Redis configuration
read -p "Enter REDIS_HOST (default: localhost): " REDIS_HOST
REDIS_HOST=${REDIS_HOST:-localhost}

read -p "Enter REDIS_PORT (default: 6379): " REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}

read -sp "Enter REDIS_PASSWORD: " REDIS_PASSWORD
echo ""
REDIS_PASSWORD=${REDIS_PASSWORD:-redis_secret_123}

# External API configuration
read -p "Enter EXTERNAL_API_URL (default: http://api.example.com:8080/v1): " EXTERNAL_API_URL
EXTERNAL_API_URL=${EXTERNAL_API_URL:-http://api.example.com:8080/v1}

read -p "Enter EXTERNAL_API_KEY: " EXTERNAL_API_KEY
EXTERNAL_API_KEY=${EXTERNAL_API_KEY:-hardcoded_api_key_12345}

# Payment service configuration
read -p "Enter PAYMENT_SERVICE_URL (default: https://payment.internal.company.com/process): " PAYMENT_SERVICE_URL
PAYMENT_SERVICE_URL=${PAYMENT_SERVICE_URL:-https://payment.internal.company.com/process}

read -p "Enter PAYMENT_SERVICE_USERNAME: " PAYMENT_SERVICE_USERNAME
PAYMENT_SERVICE_USERNAME=${PAYMENT_SERVICE_USERNAME:-payment_user}

read -sp "Enter PAYMENT_SERVICE_PASSWORD: " PAYMENT_SERVICE_PASSWORD
echo ""
PAYMENT_SERVICE_PASSWORD=${PAYMENT_SERVICE_PASSWORD:-payment_secret_456}

# RabbitMQ configuration
read -p "Enter RABBITMQ_HOST (default: localhost): " RABBITMQ_HOST
RABBITMQ_HOST=${RABBITMQ_HOST:-localhost}

read -p "Enter RABBITMQ_PORT (default: 5672): " RABBITMQ_PORT
RABBITMQ_PORT=${RABBITMQ_PORT:-5672}

read -p "Enter RABBITMQ_USERNAME: " RABBITMQ_USERNAME
RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-rabbitmq_user}

read -sp "Enter RABBITMQ_PASSWORD: " RABBITMQ_PASSWORD
echo ""
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-rabbitmq_secret}

# Monitoring configuration
read -p "Enter MONITORING_URL (default: http://monitoring.internal.company.com:9090/metrics): " MONITORING_URL
MONITORING_URL=${MONITORING_URL:-http://monitoring.internal.company.com:9090/metrics}

read -p "Enter MONITORING_USERNAME: " MONITORING_USERNAME
MONITORING_USERNAME=${MONITORING_USERNAME:-monitor_user}

read -sp "Enter MONITORING_PASSWORD: " MONITORING_PASSWORD
echo ""
MONITORING_PASSWORD=${MONITORING_PASSWORD:-monitor_pass}

# Security configuration
read -sp "Enter JWT_SECRET: " JWT_SECRET
echo ""
JWT_SECRET=${JWT_SECRET:-my_super_secret_jwt_key_123456789}

read -sp "Enter ENCRYPTION_KEY: " ENCRYPTION_KEY
echo ""
ENCRYPTION_KEY=${ENCRYPTION_KEY:-encryption_key_hardcoded}

echo ""
echo "${YELLOW}Configuring kubectl for EKS cluster...${NC}"
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

if [ $? -ne 0 ]; then
    echo "${RED}Failed to configure kubectl for EKS cluster${NC}"
    exit 1
fi

echo "${GREEN}kubectl configured successfully${NC}"
echo ""

# Verify cluster connectivity
echo "${YELLOW}Verifying cluster connectivity...${NC}"
kubectl cluster-info || {
    echo "${RED}Failed to connect to Kubernetes cluster${NC}"
    exit 1
}

echo "${GREEN}Successfully connected to cluster${NC}"
echo ""

# Update Kubernetes manifests with actual values
echo "${YELLOW}Updating Kubernetes manifests...${NC}"

# Create temporary directory for processed manifests
TMP_DIR=$(mktemp -d)
trap "rm -rf $TMP_DIR" EXIT

# Copy manifests to temp directory
cp -r kubernetes/* "$TMP_DIR/"

# Replace placeholders in deployment.yaml
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{DB_URL}}|$DB_URL|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{DB_USERNAME}}|$DB_USERNAME|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{DB_PASSWORD}}|$DB_PASSWORD|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_HOST}}|$REDIS_HOST|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_PORT}}|$REDIS_PORT|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_PASSWORD}}|$REDIS_PASSWORD|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{EXTERNAL_API_URL}}|$EXTERNAL_API_URL|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{EXTERNAL_API_KEY}}|$EXTERNAL_API_KEY|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{PAYMENT_SERVICE_URL}}|$PAYMENT_SERVICE_URL|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{PAYMENT_SERVICE_USERNAME}}|$PAYMENT_SERVICE_USERNAME|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{PAYMENT_SERVICE_PASSWORD}}|$PAYMENT_SERVICE_PASSWORD|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{RABBITMQ_HOST}}|$RABBITMQ_HOST|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{RABBITMQ_PORT}}|$RABBITMQ_PORT|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{RABBITMQ_USERNAME}}|$RABBITMQ_USERNAME|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{RABBITMQ_PASSWORD}}|$RABBITMQ_PASSWORD|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{MONITORING_URL}}|$MONITORING_URL|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{MONITORING_USERNAME}}|$MONITORING_USERNAME|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{MONITORING_PASSWORD}}|$MONITORING_PASSWORD|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{JWT_SECRET}}|$JWT_SECRET|g" "$TMP_DIR/deployment.yaml"
sed -i "s|{{ENCRYPTION_KEY}}|$ENCRYPTION_KEY|g" "$TMP_DIR/deployment.yaml"

echo "${GREEN}Manifests updated successfully${NC}"
echo ""

# Apply Kubernetes manifests
echo "${YELLOW}Applying Kubernetes manifests...${NC}"
echo ""

# Apply namespace
echo "Creating namespace..."
kubectl apply -f "$TMP_DIR/namespace.yaml"
echo ""

# Apply deployment
echo "Creating deployment..."
kubectl apply -f "$TMP_DIR/deployment.yaml"
echo ""

# Apply service
echo "Creating service..."
kubectl apply -f "$TMP_DIR/service.yaml"
echo ""

# Apply ingress
echo "Creating ingress..."
kubectl apply -f "$TMP_DIR/ingress.yaml"
echo ""

echo "${GREEN}All manifests applied successfully${NC}"
echo ""

# Wait for deployment rollout
echo "${YELLOW}Waiting for deployment to complete...${NC}"
kubectl rollout status deployment/mini-java-app -n mini-java-app --timeout=5m

if [ $? -ne 0 ]; then
    echo "${RED}Deployment rollout failed or timed out${NC}"
    echo "Checking pod status..."
    kubectl get pods -n mini-java-app
    echo ""
    echo "Pod logs:"
    kubectl logs -n mini-java-app -l app=mini-java-app --tail=50
    exit 1
fi

echo "${GREEN}Deployment completed successfully${NC}"
echo ""

# Verify deployment
echo "${YELLOW}Verifying deployment...${NC}"
echo ""
echo "Pods:"
kubectl get pods -n mini-java-app
echo ""
echo "Services:"
kubectl get svc -n mini-java-app
echo ""
echo "Ingress:"
kubectl get ingress -n mini-java-app
echo ""

# Get ingress URL
INGRESS_URL=$(kubectl get ingress mini-java-app-ingress -n mini-java-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")

echo "${GREEN}=== Deployment Successful ===${NC}"
echo ""
echo "Application Details:"
echo "  Namespace: mini-java-app"
echo "  Deployment: mini-java-app"
echo "  Service: mini-java-app-service"
echo "  Ingress URL: $INGRESS_URL"
echo ""
echo "Useful commands:"
echo "  View pods: kubectl get pods -n mini-java-app"
echo "  View logs: kubectl logs -n mini-java-app -l app=mini-java-app"
echo "  View services: kubectl get svc -n mini-java-app"
echo "  Delete deployment: kubectl delete namespace mini-java-app"
echo ""

if [ "$INGRESS_URL" != "pending" ]; then
    echo "${GREEN}Application should be accessible at: http://$INGRESS_URL/mini-app${NC}"
    echo "${YELLOW}Note: It may take a few minutes for the ALB to become fully operational${NC}"
else
    echo "${YELLOW}Ingress URL is still pending. Check back in a few minutes.${NC}"
fi

echo ""
