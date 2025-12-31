# Deployment Guide: mini-java-app on AWS ECS Fargate

This guide provides comprehensive instructions for building, containerizing, and deploying the mini-java-app Java application to AWS ECS Fargate.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Overview](#project-overview)
3. [Local Development Setup](#local-development-setup)
4. [Building the Application](#building-the-application)
5. [Docker Containerization](#docker-containerization)
6. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
7. [ECS Task Definition](#ecs-task-definition)
8. [ECS Service Configuration](#ecs-service-configuration)
9. [Deployment to AWS ECS Fargate](#deployment-to-aws-ecs-fargate)
10. [Configuration Management](#configuration-management)
11. [Monitoring and Logging](#monitoring-and-logging)
12. [Troubleshooting](#troubleshooting)
13. [Scaling and Performance](#scaling-and-performance)
14. [Security Best Practices](#security-best-practices)

---

## Prerequisites

### Required Software

- **Java Development Kit (JDK) 11 or higher**
  - Download: https://adoptium.net/
  - Verify: `java -version`

- **Maven 3.6+**
  - Download: https://maven.apache.org/download.cgi
  - Verify: `mvn -version`

- **Docker Desktop**
  - Download: https://www.docker.com/products/docker-desktop
  - Verify: `docker --version`

- **AWS CLI v2**
  - Download: https://aws.amazon.com/cli/
  - Verify: `aws --version`
  - Configure: `aws configure`

- **Git** (optional, for version control)
  - Download: https://git-scm.com/downloads
  - Verify: `git --version`

### AWS Account Requirements

- Active AWS account with appropriate permissions
- IAM user with permissions for:
  - ECS (CreateCluster, RegisterTaskDefinition, CreateService, etc.)
  - ECR (CreateRepository, PutImage, etc.)
  - CloudWatch Logs (CreateLogGroup, PutLogEvents, etc.)
  - IAM (PassRole for task execution role)
  - EC2 (for VPC, subnets, security groups)
  - Elastic Load Balancing (optional, for ALB)

---

## Project Overview

### Application Details

- **Application Name:** mini-java-app
- **Technology Stack:** Java 11, Maven
- **Framework:** Plain Java (Socket Server)
- **Package Type:** JAR (Executable)
- **Default Port:** 8080
- **Build Tool:** Maven 3.9+

### Application Architecture

```
mini-java-app
├── src/
│   └── main/
│       ├── java/
│       │   └── com/test/
│       │       ├── MiniApp.java          (Main application)
│       │       └── DatabaseService.java  (Database service)
│       └── resources/
│           └── application.properties    (Configuration)
├── pom.xml                               (Maven configuration)
├── Dockerfile                            (Container definition)
├── docker-compose.yml                    (Local development)
└── scripts/                              (Deployment scripts)
```

### External Dependencies

The application requires connections to:
- **MySQL Database** (configured via environment variables)
- **Redis Cache** (configured via environment variables)
- **RabbitMQ** (configured via environment variables)
- **External APIs** (configured via environment variables)

**Important:** This deployment guide focuses on containerizing the application only. External services must be provisioned separately.

---

## Local Development Setup

### 1. Clone or Navigate to Project Directory

```bash
cd /path/to/mini-java-app
```

### 2. Build the Application Locally

```bash
mvn clean package -DskipTests
```

The compiled JAR will be located at: `target/mini-java-app-1.0.0.jar`

### 3. Run Locally (Optional)

```bash
java -jar target/mini-java-app-1.0.0.jar
```

**Note:** You'll need to configure environment variables for external services.

---

## Building the Application

### Maven Build Process

1. **Clean previous builds:**
   ```bash
   mvn clean
   ```

2. **Compile and package:**
   ```bash
   mvn package -DskipTests
   ```

3. **Verify build artifacts:**
   ```bash
   ls -lh target/*.jar
   ```

### Build Output

- **JAR File:** `target/mini-java-app-1.0.0.jar`
- **Size:** ~15-20 MB (including dependencies)

---

## Docker Containerization

### Dockerfile Overview

The Dockerfile uses a multi-stage build process:

1. **Builder Stage:** Uses `maven:3.9.4-eclipse-temurin-11` to compile the application
2. **Runtime Stage:** Uses `eclipse-temurin:11-jre-alpine` for a minimal runtime image

### Key Features

- **Layer Caching:** Maven dependencies are downloaded separately for faster rebuilds
- **Non-Root User:** Application runs as unprivileged user `appuser` (UID 1001)
- **JVM Optimization:** Configured with container-aware JVM settings
- **Security:** Minimal runtime image with only necessary components

### Building Docker Image Locally

```bash
# Build the image
docker build -t mini-java-app:latest .

# Verify the image
docker images | grep mini-java-app
```

### Running with Docker Compose

```bash
# Start the application
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

**Note:** Update `docker-compose.yml` with your external service endpoints before running.

---

## AWS ECS Fargate Prerequisites

### 1. AWS CLI Configuration

Configure AWS CLI with your credentials:

```bash
aws configure
```

Provide:
- AWS Access Key ID
- AWS Secret Access Key
- Default region (e.g., `us-east-1`)
- Output format (e.g., `json`)

Verify configuration:

```bash
aws sts get-caller-identity
```

### 2. VPC and Networking Setup

#### Create or Identify VPC

```bash
# List existing VPCs
aws ec2 describe-vpcs --query 'Vpcs[*].[VpcId,CidrBlock,Tags[?Key==`Name`].Value|[0]]' --output table

# Create new VPC (if needed)
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=mini-app-vpc}]'
```

#### Create Subnets

Fargate requires at least 2 subnets in different Availability Zones:

```bash
# Create subnet 1 (AZ a)
aws ec2 create-subnet \
  --vpc-id vpc-xxxxx \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=mini-app-subnet-1}]'

# Create subnet 2 (AZ b)
aws ec2 create-subnet \
  --vpc-id vpc-xxxxx \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=mini-app-subnet-2}]'

# Enable auto-assign public IP
aws ec2 modify-subnet-attribute --subnet-id subnet-xxxxx --map-public-ip-on-launch
```

#### Create Internet Gateway

```bash
# Create IGW
aws ec2 create-internet-gateway --tag-specifications 'ResourceType=internet-gateway,Tags=[{Key=Name,Value=mini-app-igw}]'

# Attach to VPC
aws ec2 attach-internet-gateway --vpc-id vpc-xxxxx --internet-gateway-id igw-xxxxx

# Update route table
aws ec2 create-route --route-table-id rtb-xxxxx --destination-cidr-block 0.0.0.0/0 --gateway-id igw-xxxxx
```

#### Create Security Group

```bash
# Create security group
aws ec2 create-security-group \
  --group-name mini-app-sg \
  --description "Security group for mini-java-app" \
  --vpc-id vpc-xxxxx

# Allow inbound traffic on port 8080
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Allow outbound traffic (all)
aws ec2 authorize-security-group-egress \
  --group-id sg-xxxxx \
  --protocol -1 \
  --cidr 0.0.0.0/0
```

### 3. IAM Roles Setup

#### Task Execution Role

Required for ECS to pull images from ECR and write logs to CloudWatch:

```bash
# Create trust policy
cat > ecs-task-execution-trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file://ecs-task-execution-trust-policy.json

# Attach managed policy
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

#### Task Role (Optional)

Required if your application needs to access AWS services:

```bash
# Create task role
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document file://ecs-task-execution-trust-policy.json

# Attach policies as needed (example: S3 access)
aws iam attach-role-policy \
  --role-name ecsTaskRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess
```

### 4. CloudWatch Logs Setup

```bash
# Create log group
aws logs create-log-group --log-group-name /ecs/mini-java-app

# Set retention policy (optional)
aws logs put-retention-policy \
  --log-group-name /ecs/mini-java-app \
  --retention-in-days 7
```

---

## ECS Task Definition

### Task Definition Overview

The task definition (`ecs/task-definition.json`) defines:

- **Launch Type:** FARGATE
- **Network Mode:** awsvpc (required for Fargate)
- **CPU:** 512 (.5 vCPU)
- **Memory:** 1024 MB (1 GB)
- **Container:** mini-java-app with JVM optimizations

### Valid Fargate CPU/Memory Combinations

| CPU (vCPU) | Memory Options (MB) |
|------------|--------------------|
| 256 (.25)  | 512, 1024, 2048 |
| 512 (.5)   | 1024, 2048, 3072, 4096 |
| 1024 (1)   | 2048 - 8192 (increments of 1024) |
| 2048 (2)   | 4096 - 16384 (increments of 1024) |
| 4096 (4)   | 8192 - 30720 (increments of 1024) |

### JVM Memory Configuration

The task definition includes optimized JVM settings:

```
JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

**Explanation:**
- `-Xmx512m`: Maximum heap size (50% of container memory)
- `-Xms256m`: Initial heap size
- `-XX:+UseContainerSupport`: Enable container awareness
- `-XX:MaxRAMPercentage=75.0`: Use up to 75% of container memory

### Environment Variables

The task definition includes placeholders for:

- `SERVER_PORT`: Application port (8080)
- `DATABASE_URL`: MySQL connection string
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password (use AWS Secrets Manager in production)
- `REDIS_HOST`: Redis server hostname
- `REDIS_PORT`: Redis server port (6379)
- `EXTERNAL_API_URL`: External API endpoint
- `PAYMENT_SERVICE_URL`: Payment service endpoint
- `RABBITMQ_HOST`: RabbitMQ server hostname
- `RABBITMQ_USERNAME`: RabbitMQ username
- `RABBITMQ_PASSWORD`: RabbitMQ password

**Security Note:** Use AWS Secrets Manager or Parameter Store for sensitive values in production.

---

## ECS Service Configuration

### Service Definition Overview

The service definition (`ecs/service-definition.json`) configures:

- **Desired Count:** 2 tasks (for high availability)
- **Launch Type:** FARGATE
- **Network Mode:** awsvpc
- **Deployment Strategy:** Rolling update (max 200%, min 50%)
- **Circuit Breaker:** Enabled with automatic rollback
- **Load Balancer:** Optional ALB integration

### Deployment Configuration

```json
"deploymentConfiguration": {
  "maximumPercent": 200,
  "minimumHealthyPercent": 50,
  "deploymentCircuitBreaker": {
    "enable": true,
    "rollback": true
  }
}
```

**Explanation:**
- **maximumPercent:** Allow up to 200% of desired count during deployment (4 tasks total)
- **minimumHealthyPercent:** Maintain at least 50% healthy tasks (1 task minimum)
- **Circuit Breaker:** Automatically rollback failed deployments

### Service Scaling

The service can be scaled manually or automatically:

```bash
# Manual scaling
aws ecs update-service \
  --cluster mini-app-cluster \
  --service mini-java-app-service \
  --desired-count 4
```

---

## Deployment to AWS ECS Fargate

### Step 1: Build and Push Docker Image

#### Using Linux/macOS

```bash
cd /path/to/mini-java-app
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

#### Using Windows

```cmd
cd C:\path\to\mini-java-app
scripts\build-push.bat
```

#### Script Workflow

1. **Select Registry:**
   - Option 1: AWS ECR (recommended for ECS)
   - Option 2: Docker Hub

2. **Provide Registry Details:**
   - For ECR: AWS Region, Repository Name
   - For Docker Hub: Username, Password, Repository Name

3. **Image Tag:** Specify version tag (default: `latest`)

4. **Build and Push:** Script builds image and pushes to registry

#### Example Output

```
===========================================
Docker Build and Push Script
===========================================

Select Container Registry:
1. AWS ECR (Elastic Container Registry)
2. Docker Hub
Enter choice (1 or 2): 1

AWS ECR Configuration
Enter AWS Region (e.g., us-east-1): us-east-1
Retrieving AWS Account ID...
AWS Account ID: 123456789012
Enter ECR Repository Name (default: mini-java-app): mini-java-app
Enter image tag (default: latest): v1.0.0

Logging in to AWS ECR...
Successfully logged in to ECR

Building Docker image: 123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:v1.0.0
Docker image built successfully

Pushing Docker image to registry...

===========================================
Build and Push Completed Successfully!
===========================================
Image: 123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:v1.0.0
```

### Step 2: Deploy to ECS Fargate

#### Using Linux/macOS

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

#### Using Windows

```cmd
scripts\deploy-image.bat
```

#### Deployment Workflow

1. **AWS Configuration:**
   - Enter AWS Region
   - Script retrieves AWS Account ID automatically

2. **ECS Cluster:**
   - Enter cluster name (created if doesn't exist)

3. **Network Configuration:**
   - VPC ID
   - Subnet IDs (comma-separated, at least 2)
   - Security Group ID

4. **Docker Image:**
   - Enter full image URI from Step 1

5. **Load Balancer (Optional):**
   - Choose whether to create ALB
   - If yes, script creates ALB and Target Group automatically

6. **Deployment:**
   - Registers task definition
   - Creates/updates ECS service
   - Waits for service stabilization

#### Example Deployment

```
=================================================
AWS ECS Fargate Deployment Script
Application: mini-java-app
=================================================

Enter AWS Region (e.g., us-east-1): us-east-1
Retrieving AWS Account ID...
AWS Account ID: 123456789012

Enter ECS Cluster Name: mini-app-cluster
Checking if ECS cluster exists...

Enter VPC ID: vpc-0abc123def456
Enter Subnet IDs (comma-separated, at least 2): subnet-0abc123,subnet-0def456
Enter Security Group ID: sg-0abc123def
Enter Docker Image URI: 123456789012.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:v1.0.0

Load Balancer Configuration
Do you need an Application Load Balancer? (y/n): y
Creating Application Load Balancer...
ALB ARN: arn:aws:elasticloadbalancing:us-east-1:123456789012:loadbalancer/app/mini-java-app-alb/abc123
Target Group ARN: arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/mini-java-app-tg/def456

Creating CloudWatch Log Group...
Log Group: /ecs/mini-java-app

Preparing ECS Task Definition...
Registering ECS Task Definition...
Task Definition registered: arn:aws:ecs:us-east-1:123456789012:task-definition/mini-java-app-task:1

Preparing ECS Service Definition...
Creating new ECS service...
ECS service created successfully

Waiting for service to stabilize (this may take several minutes)...
Service is stable

Verifying deployment...
Running Tasks: 2 / 2

=================================================
Deployment Completed Successfully!
=================================================
Cluster: mini-app-cluster
Service: mini-java-app-service
Task Definition: arn:aws:ecs:us-east-1:123456789012:task-definition/mini-java-app-task:1
Region: us-east-1
Running Tasks: 2 / 2
Load Balancer DNS: mini-java-app-alb-123456789.us-east-1.elb.amazonaws.com
Application URL: http://mini-java-app-alb-123456789.us-east-1.elb.amazonaws.com
CloudWatch Logs: /ecs/mini-java-app
```

### Step 3: Verify Deployment

#### Check Service Status

```bash
aws ecs describe-services \
  --cluster mini-app-cluster \
  --services mini-java-app-service \
  --query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount}'
```

#### Check Running Tasks

```bash
aws ecs list-tasks \
  --cluster mini-app-cluster \
  --service-name mini-java-app-service
```

#### Test Application

If using ALB:

```bash
curl http://[ALB-DNS-NAME]:80
```

---

## Configuration Management

### Environment Variables

Environment variables are defined in `ecs/task-definition.json`. Update values before deployment:

```json
"environment": [
  {"name": "DATABASE_URL", "value": "jdbc:mysql://your-db-host:3306/mini_app_db"},
  {"name": "DB_USERNAME", "value": "your-username"},
  {"name": "DB_PASSWORD", "value": "your-password"}
]
```

### Using AWS Secrets Manager (Recommended)

For production, use Secrets Manager for sensitive values:

```json
"secrets": [
  {
    "name": "DB_PASSWORD",
    "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:mini-app/db-password"
  }
]
```

Create secret:

```bash
aws secretsmanager create-secret \
  --name mini-app/db-password \
  --secret-string "your-secure-password"
```

### Using AWS Systems Manager Parameter Store

```json
"secrets": [
  {
    "name": "DB_PASSWORD",
    "valueFrom": "arn:aws:ssm:us-east-1:123456789012:parameter/mini-app/db-password"
  }
]
```

Create parameter:

```bash
aws ssm put-parameter \
  --name /mini-app/db-password \
  --value "your-secure-password" \
  --type SecureString
```

---

## Monitoring and Logging

### CloudWatch Logs

#### View Real-Time Logs

```bash
aws logs tail /ecs/mini-java-app --follow
```

#### Filter Logs by Pattern

```bash
aws logs filter-log-events \
  --log-group-name /ecs/mini-java-app \
  --filter-pattern "ERROR"
```

#### CloudWatch Insights Query

```
fields @timestamp, @message
| filter @message like /ERROR/
| sort @timestamp desc
| limit 100
```

### CloudWatch Metrics

#### Service Metrics

- CPUUtilization
- MemoryUtilization
- TaskCount
- DesiredTaskCount

#### View Metrics

```bash
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=mini-app-cluster \
  --start-time 2025-01-01T00:00:00Z \
  --end-time 2025-01-01T23:59:59Z \
  --period 3600 \
  --statistics Average
```

### CloudWatch Alarms

#### High CPU Alarm

```bash
aws cloudwatch put-metric-alarm \
  --alarm-name mini-app-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=ServiceName,Value=mini-java-app-service Name=ClusterName,Value=mini-app-cluster
```

### Application Performance Monitoring

Consider integrating APM tools:

- **AWS X-Ray:** Distributed tracing
- **Datadog:** Full-stack monitoring
- **New Relic:** Application performance
- **Prometheus + Grafana:** Custom metrics

---

## Troubleshooting

### Common Issues

#### 1. Task Fails to Start

**Symptom:** Tasks transition from PENDING to STOPPED immediately

**Diagnosis:**

```bash
aws ecs describe-tasks \
  --cluster mini-app-cluster \
  --tasks [TASK-ARN] \
  --query 'tasks[0].stoppedReason'
```

**Common Causes:**
- Invalid CPU/memory combination
- Missing execution role permissions
- Image pull failure (check ECR permissions)
- Invalid environment variables

**Solution:**

```bash
# Check execution role
aws iam get-role --role-name ecsTaskExecutionRole

# Verify ECR permissions
aws ecr get-repository-policy --repository-name mini-java-app

# Check task definition
aws ecs describe-task-definition --task-definition mini-java-app-task
```

#### 2. Application Crashes on Startup

**Symptom:** Container starts but exits immediately

**Diagnosis:**

```bash
aws logs tail /ecs/mini-java-app --since 10m
```

**Common Causes:**
- JVM out of memory (heap size too large for container)
- Missing required environment variables
- Database connection failure
- Port already in use

**Solution:**

```bash
# Adjust JVM settings in task definition
"environment": [
  {"name": "JAVA_OPTS", "value": "-Xmx384m -Xms192m"}
]

# Verify database connectivity from task
aws ecs execute-command \
  --cluster mini-app-cluster \
  --task [TASK-ID] \
  --container mini-java-app \
  --interactive \
  --command "/bin/sh"
```

#### 3. Network Connectivity Issues

**Symptom:** Cannot reach external services or ALB cannot reach tasks

**Diagnosis:**

```bash
# Check security group rules
aws ec2 describe-security-groups --group-ids sg-xxxxx

# Check route tables
aws ec2 describe-route-tables --filters "Name=vpc-id,Values=vpc-xxxxx"

# Check network ACLs
aws ec2 describe-network-acls --filters "Name=vpc-id,Values=vpc-xxxxx"
```

**Solution:**

```bash
# Add inbound rule to security group
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Verify internet gateway
aws ec2 describe-internet-gateways \
  --filters "Name=attachment.vpc-id,Values=vpc-xxxxx"
```

#### 4. Service Fails to Stabilize

**Symptom:** Deployment never completes, tasks keep restarting

**Diagnosis:**

```bash
aws ecs describe-services \
  --cluster mini-app-cluster \
  --services mini-java-app-service \
  --query 'services[0].events[0:10]'
```

**Common Causes:**
- Health check failing (if using ALB)
- Insufficient resources (CPU/memory)
- Application crashes after startup

**Solution:**

```bash
# Check target group health
aws elbv2 describe-target-health \
  --target-group-arn [TARGET-GROUP-ARN]

# Adjust health check settings
aws elbv2 modify-target-group \
  --target-group-arn [TARGET-GROUP-ARN] \
  --health-check-interval-seconds 30 \
  --health-check-timeout-seconds 10 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3
```

#### 5. High Memory Usage

**Symptom:** Tasks killed due to out-of-memory (OOM)

**Diagnosis:**

```bash
# Check CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name MemoryUtilization \
  --dimensions Name=ServiceName,Value=mini-java-app-service \
  --start-time [START] \
  --end-time [END] \
  --period 300 \
  --statistics Maximum
```

**Solution:**

1. Increase container memory in task definition:
   ```json
   "memory": "2048"
   ```

2. Adjust JVM heap size:
   ```
   JAVA_OPTS=-Xmx1536m -Xms768m
   ```

3. Enable JVM memory analysis:
   ```bash
   # Add to task definition
   -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs
   ```

### Useful Commands

```bash
# View task details
aws ecs describe-tasks --cluster mini-app-cluster --tasks [TASK-ARN]

# View service events
aws ecs describe-services --cluster mini-app-cluster --services mini-java-app-service --query 'services[0].events'

# Force new deployment
aws ecs update-service --cluster mini-app-cluster --service mini-java-app-service --force-new-deployment

# Stop task
aws ecs stop-task --cluster mini-app-cluster --task [TASK-ARN]

# Deregister task definition
aws ecs deregister-task-definition --task-definition mini-java-app-task:1

# Delete service
aws ecs delete-service --cluster mini-app-cluster --service mini-java-app-service --force

# Delete cluster
aws ecs delete-cluster --cluster mini-app-cluster
```

---

## Scaling and Performance

### Manual Scaling

```bash
aws ecs update-service \
  --cluster mini-app-cluster \
  --service mini-java-app-service \
  --desired-count 4
```

### Auto Scaling with Target Tracking

#### Create Auto Scaling Target

```bash
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/mini-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10
```

#### Create Scaling Policy (CPU-based)

```bash
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/mini-app-cluster/mini-java-app-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name cpu-scaling-policy \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration file://scaling-policy.json
```

**scaling-policy.json:**

```json
{
  "TargetValue": 70.0,
  "PredefinedMetricSpecification": {
    "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
  },
  "ScaleOutCooldown": 60,
  "ScaleInCooldown": 300
}
```

### Performance Tuning

#### JVM Tuning

```
JAVA_OPTS="
  -Xmx768m 
  -Xms384m 
  -XX:+UseG1GC 
  -XX:MaxGCPauseMillis=200 
  -XX:+UseContainerSupport 
  -XX:MaxRAMPercentage=75.0 
  -XX:InitialRAMPercentage=50.0
"
```

#### Connection Pooling

Configure connection pools for databases:

```properties
# HikariCP settings (if using Spring Boot)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

---

## Security Best Practices

### 1. Use Secrets Manager for Sensitive Data

```bash
# Store database password
aws secretsmanager create-secret \
  --name mini-app/db-credentials \
  --secret-string '{"username":"dbuser","password":"securepassword"}'

# Reference in task definition
"secrets": [
  {
    "name": "DB_PASSWORD",
    "valueFrom": "arn:aws:secretsmanager:region:account:secret:mini-app/db-credentials:password::"
  }
]
```

### 2. Restrict Security Group Rules

```bash
# Only allow ALB security group
aws ec2 authorize-security-group-ingress \
  --group-id [TASK-SG] \
  --protocol tcp \
  --port 8080 \
  --source-group [ALB-SG]
```

### 3. Enable VPC Flow Logs

```bash
aws ec2 create-flow-logs \
  --resource-type VPC \
  --resource-ids vpc-xxxxx \
  --traffic-type ALL \
  --log-destination-type cloud-watch-logs \
  --log-destination arn:aws:logs:region:account:log-group:/aws/vpc/flowlogs
```

### 4. Use Task Role with Least Privilege

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": "arn:aws:s3:::my-bucket/*"
    }
  ]
}
```

### 5. Enable Container Insights

```bash
aws ecs put-account-setting \
  --name containerInsights \
  --value enabled

aws ecs update-cluster-settings \
  --cluster mini-app-cluster \
  --settings name=containerInsights,value=enabled
```

### 6. Scan Images for Vulnerabilities

```bash
# Enable ECR image scanning
aws ecr put-image-scanning-configuration \
  --repository-name mini-java-app \
  --image-scanning-configuration scanOnPush=true

# View scan results
aws ecr describe-image-scan-findings \
  --repository-name mini-java-app \
  --image-id imageTag=latest
```

---

## Additional Resources

- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [AWS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Java Container Best Practices](https://docs.oracle.com/en/java/javase/11/docs/)
- [Maven Documentation](https://maven.apache.org/guides/)

---

## Support

For issues or questions:

1. Check CloudWatch Logs: `/ecs/mini-java-app`
2. Review ECS service events
3. Consult AWS Support or community forums
4. Review application logs for Java-specific errors

---

**Document Version:** 1.0.0  
**Last Updated:** 2025-12-31  
**Application:** mini-java-app  
**Target Platform:** AWS ECS Fargate
