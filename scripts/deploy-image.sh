#!/bin/bash
set -e
set -o pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${GREEN}================================================="
echo "AWS ECS Fargate Deployment Script"
echo "Application: mini-java-app"
echo -e "=================================================${NC}"
echo ""

# Project configuration
PROJECT_NAME="mini-java-app"
TASK_FAMILY="${PROJECT_NAME}-task"
SERVICE_NAME="${PROJECT_NAME}-service"
CONTAINER_NAME="${PROJECT_NAME}"
CONTAINER_PORT=8080

# Prompt for AWS Region
read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
    echo -e "${RED}Error: AWS Region is required${NC}"
    exit 1
fi
export AWS_DEFAULT_REGION=$AWS_REGION

# Get AWS Account ID
echo -e "${YELLOW}Retrieving AWS Account ID...${NC}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
if [ -z "$ACCOUNT_ID" ]; then
    echo -e "${RED}Error: Failed to retrieve AWS Account ID${NC}"
    exit 1
fi
echo -e "${GREEN}AWS Account ID: $ACCOUNT_ID${NC}"

# Prompt for ECS Cluster
read -p "Enter ECS Cluster Name (will be created if doesn't exist): " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
    echo -e "${RED}Error: Cluster name is required${NC}"
    exit 1
fi

# Check if cluster exists, create if not
echo -e "${YELLOW}Checking if ECS cluster exists...${NC}"
aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION >/dev/null 2>&1 || {
    echo -e "${YELLOW}Cluster does not exist. Creating ECS cluster...${NC}"
    aws ecs create-cluster --cluster-name $CLUSTER_NAME --region $AWS_REGION
    echo -e "${GREEN}ECS cluster created successfully${NC}"
}

# Prompt for VPC ID
read -p "Enter VPC ID: " VPC_ID
if [ -z "$VPC_ID" ]; then
    echo -e "${RED}Error: VPC ID is required${NC}"
    exit 1
fi

# Prompt for Subnets
read -p "Enter Subnet IDs (comma-separated, at least 2 for HA): " SUBNETS_INPUT
if [ -z "$SUBNETS_INPUT" ]; then
    echo -e "${RED}Error: At least 2 subnet IDs are required${NC}"
    exit 1
fi

# Parse subnets
IFS=',' read -ra SUBNET_ARRAY <<< "$SUBNETS_INPUT"
SUBNET_1=$(echo "${SUBNET_ARRAY[0]}" | xargs)
SUBNET_2=$(echo "${SUBNET_ARRAY[1]}" | xargs)

if [ -z "$SUBNET_1" ] || [ -z "$SUBNET_2" ]; then
    echo -e "${RED}Error: At least 2 subnet IDs are required${NC}"
    exit 1
fi

# Prompt for Security Group
read -p "Enter Security Group ID (must allow inbound on port $CONTAINER_PORT): " SECURITY_GROUP
if [ -z "$SECURITY_GROUP" ]; then
    echo -e "${RED}Error: Security Group ID is required${NC}"
    exit 1
fi

# Prompt for Docker Image URI
read -p "Enter Docker Image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
    echo -e "${RED}Error: Image URI is required${NC}"
    exit 1
fi

# Load Balancer Configuration
echo -e "\n${YELLOW}Load Balancer Configuration${NC}"
read -p "Do you need an Application Load Balancer for this service? (y/n): " NEED_ALB

if [[ "$NEED_ALB" =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Creating Application Load Balancer...${NC}"
    
    # Create ALB
    ALB_NAME="${PROJECT_NAME}-alb"
    ALB_ARN=$(aws elbv2 create-load-balancer \
        --name $ALB_NAME \
        --subnets $SUBNET_1 $SUBNET_2 \
        --security-groups $SECURITY_GROUP \
        --scheme internet-facing \
        --type application \
        --ip-address-type ipv4 \
        --region $AWS_REGION \
        --query 'LoadBalancers[0].LoadBalancerArn' \
        --output text 2>/dev/null || echo "")
    
    if [ -z "$ALB_ARN" ]; then
        echo -e "${YELLOW}ALB may already exist, retrieving existing ALB...${NC}"
        ALB_ARN=$(aws elbv2 describe-load-balancers \
            --names $ALB_NAME \
            --region $AWS_REGION \
            --query 'LoadBalancers[0].LoadBalancerArn' \
            --output text)
    fi
    
    echo -e "${GREEN}ALB ARN: $ALB_ARN${NC}"
    
    # Get ALB DNS Name
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --load-balancer-arns $ALB_ARN \
        --region $AWS_REGION \
        --query 'LoadBalancers[0].DNSName' \
        --output text)
    
    # Create Target Group with ip target type (required for Fargate)
    TG_NAME="${PROJECT_NAME}-tg"
    TARGET_GROUP_ARN=$(aws elbv2 create-target-group \
        --name $TG_NAME \
        --protocol HTTP \
        --port $CONTAINER_PORT \
        --vpc-id $VPC_ID \
        --target-type ip \
        --health-check-enabled \
        --health-check-protocol HTTP \
        --health-check-path / \
        --health-check-interval-seconds 30 \
        --health-check-timeout-seconds 5 \
        --healthy-threshold-count 2 \
        --unhealthy-threshold-count 3 \
        --region $AWS_REGION \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text 2>/dev/null || echo "")
    
    if [ -z "$TARGET_GROUP_ARN" ]; then
        echo -e "${YELLOW}Target Group may already exist, retrieving existing TG...${NC}"
        TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups \
            --names $TG_NAME \
            --region $AWS_REGION \
            --query 'TargetGroups[0].TargetGroupArn' \
            --output text)
    fi
    
    echo -e "${GREEN}Target Group ARN: $TARGET_GROUP_ARN${NC}"
    
    # Create Listener
    LISTENER_ARN=$(aws elbv2 create-listener \
        --load-balancer-arn $ALB_ARN \
        --protocol HTTP \
        --port 80 \
        --default-actions Type=forward,TargetGroupArn=$TARGET_GROUP_ARN \
        --region $AWS_REGION \
        --query 'Listeners[0].ListenerArn' \
        --output text 2>/dev/null || echo "")
    
    if [ -z "$LISTENER_ARN" ]; then
        echo -e "${YELLOW}Listener may already exist${NC}"
    else
        echo -e "${GREEN}Listener created successfully${NC}"
    fi
    
    USE_LOAD_BALANCER=true
else
    echo -e "${YELLOW}Skipping Load Balancer creation${NC}"
    USE_LOAD_BALANCER=false
fi

# Create CloudWatch Log Group
echo -e "\n${YELLOW}Creating CloudWatch Log Group...${NC}"
LOG_GROUP="/ecs/${PROJECT_NAME}"
aws logs create-log-group --log-group-name $LOG_GROUP --region $AWS_REGION 2>/dev/null || echo -e "${YELLOW}Log group already exists${NC}"
echo -e "${GREEN}Log Group: $LOG_GROUP${NC}"

# Update task definition JSON
echo -e "\n${YELLOW}Preparing ECS Task Definition...${NC}"
TASK_DEF_FILE="ecs/task-definition.json"

if [ ! -f "$TASK_DEF_FILE" ]; then
    echo -e "${RED}Error: Task definition file not found: $TASK_DEF_FILE${NC}"
    exit 1
fi

# Create temporary task definition with replacements
TASK_DEF_TEMP="/tmp/task-definition-${PROJECT_NAME}.json"
cat $TASK_DEF_FILE | \
    sed "s|{{IMAGE_URI}}|$IMAGE_URI|g" | \
    sed "s|{{AWS_REGION}}|$AWS_REGION|g" | \
    sed "s|{{ACCOUNT_ID}}|$ACCOUNT_ID|g" > $TASK_DEF_TEMP

# Register task definition
echo -e "${YELLOW}Registering ECS Task Definition...${NC}"
TASK_DEF_ARN=$(aws ecs register-task-definition \
    --cli-input-json file://$TASK_DEF_TEMP \
    --region $AWS_REGION \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text)

if [ -z "$TASK_DEF_ARN" ]; then
    echo -e "${RED}Error: Failed to register task definition${NC}"
    exit 1
fi

echo -e "${GREEN}Task Definition registered: $TASK_DEF_ARN${NC}"

# Update service definition JSON
echo -e "\n${YELLOW}Preparing ECS Service Definition...${NC}"
SERVICE_DEF_FILE="ecs/service-definition.json"

if [ ! -f "$SERVICE_DEF_FILE" ]; then
    echo -e "${RED}Error: Service definition file not found: $SERVICE_DEF_FILE${NC}"
    exit 1
fi

# Create temporary service definition with replacements
SERVICE_DEF_TEMP="/tmp/service-definition-${PROJECT_NAME}.json"
cat $SERVICE_DEF_FILE | \
    sed "s|{{CLUSTER_NAME}}|$CLUSTER_NAME|g" | \
    sed "s|{{SUBNET_1}}|$SUBNET_1|g" | \
    sed "s|{{SUBNET_2}}|$SUBNET_2|g" | \
    sed "s|{{SECURITY_GROUP}}|$SECURITY_GROUP|g" > $SERVICE_DEF_TEMP

# Handle load balancer in service definition
if [ "$USE_LOAD_BALANCER" = false ]; then
    # Remove loadBalancers section from service definition
    cat $SERVICE_DEF_TEMP | jq 'del(.loadBalancers) | del(.healthCheckGracePeriodSeconds)' > "${SERVICE_DEF_TEMP}.tmp"
    mv "${SERVICE_DEF_TEMP}.tmp" $SERVICE_DEF_TEMP
else
    # Replace target group ARN
    cat $SERVICE_DEF_TEMP | sed "s|{{TARGET_GROUP_ARN}}|$TARGET_GROUP_ARN|g" > "${SERVICE_DEF_TEMP}.tmp"
    mv "${SERVICE_DEF_TEMP}.tmp" $SERVICE_DEF_TEMP
fi

# Check if service exists
echo -e "\n${YELLOW}Checking if ECS service exists...${NC}"
EXISTING_SERVICE=$(aws ecs describe-services \
    --cluster $CLUSTER_NAME \
    --services $SERVICE_NAME \
    --region $AWS_REGION \
    --query 'services[?status==`ACTIVE`].serviceName' \
    --output text)

if [ -z "$EXISTING_SERVICE" ] || [ "$EXISTING_SERVICE" = "None" ]; then
    # Create new service
    echo -e "${YELLOW}Creating new ECS service...${NC}"
    aws ecs create-service \
        --cli-input-json file://$SERVICE_DEF_TEMP \
        --region $AWS_REGION > /dev/null
    
    echo -e "${GREEN}ECS service created successfully${NC}"
else
    # Update existing service
    echo -e "${YELLOW}Updating existing ECS service...${NC}"
    aws ecs update-service \
        --cluster $CLUSTER_NAME \
        --service $SERVICE_NAME \
        --task-definition $TASK_DEF_ARN \
        --force-new-deployment \
        --region $AWS_REGION > /dev/null
    
    echo -e "${GREEN}ECS service updated successfully${NC}"
fi

# Wait for service to stabilize
echo -e "\n${YELLOW}Waiting for service to stabilize (this may take several minutes)...${NC}"
aws ecs wait services-stable \
    --cluster $CLUSTER_NAME \
    --services $SERVICE_NAME \
    --region $AWS_REGION

echo -e "${GREEN}Service is stable${NC}"

# Verify deployment
echo -e "\n${YELLOW}Verifying deployment...${NC}"
RUNNING_COUNT=$(aws ecs describe-services \
    --cluster $CLUSTER_NAME \
    --services $SERVICE_NAME \
    --region $AWS_REGION \
    --query 'services[0].runningCount' \
    --output text)

DESIRED_COUNT=$(aws ecs describe-services \
    --cluster $CLUSTER_NAME \
    --services $SERVICE_NAME \
    --region $AWS_REGION \
    --query 'services[0].desiredCount' \
    --output text)

echo -e "${GREEN}Running Tasks: $RUNNING_COUNT / $DESIRED_COUNT${NC}"

# Display deployment summary
echo -e "\n${GREEN}================================================="
echo "Deployment Completed Successfully!"
echo "=================================================${NC}"
echo -e "${BLUE}Cluster:${NC} $CLUSTER_NAME"
echo -e "${BLUE}Service:${NC} $SERVICE_NAME"
echo -e "${BLUE}Task Definition:${NC} $TASK_DEF_ARN"
echo -e "${BLUE}Region:${NC} $AWS_REGION"
echo -e "${BLUE}Running Tasks:${NC} $RUNNING_COUNT / $DESIRED_COUNT"

if [ "$USE_LOAD_BALANCER" = true ]; then
    echo -e "${BLUE}Load Balancer DNS:${NC} $ALB_DNS"
    echo -e "${BLUE}Application URL:${NC} http://$ALB_DNS"
fi

echo -e "${BLUE}CloudWatch Logs:${NC} $LOG_GROUP"
echo ""
echo -e "${YELLOW}To view logs:${NC}"
echo "aws logs tail $LOG_GROUP --follow --region $AWS_REGION"
echo ""
echo -e "${YELLOW}To view service details:${NC}"
echo "aws ecs describe-services --cluster $CLUSTER_NAME --services $SERVICE_NAME --region $AWS_REGION"
echo ""

# Clean up temporary files
rm -f $TASK_DEF_TEMP $SERVICE_DEF_TEMP

echo -e "${GREEN}Deployment script completed successfully!${NC}"
