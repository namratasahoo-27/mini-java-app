# Deployment Guide - Mini Java Application

Comprehensive guide for deploying the Mini Java Application to AWS EKS (Elastic Kubernetes Service).

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Building and Testing with Docker](#building-and-testing-with-docker)
4. [AWS EKS Prerequisites](#aws-eks-prerequisites)
5. [Building and Pushing Docker Image](#building-and-pushing-docker-image)
6. [Deploying to AWS EKS](#deploying-to-aws-eks)
7. [Verifying Deployment](#verifying-deployment)
8. [Configuration Management](#configuration-management)
9. [Troubleshooting](#troubleshooting)
10. [Scaling and Management](#scaling-and-management)
11. [Security Considerations](#security-considerations)
12. [Technology-Specific Notes](#technology-specific-notes)

---

## Prerequisites

### Required Software

- **Java Development Kit (JDK) 11** or higher
- **Maven 3.6+** for building the application
- **Docker 20.10+** for containerization
- **Docker Compose 1.29+** for local development
- **AWS CLI 2.x** for AWS operations
- **kubectl 1.24+** for Kubernetes operations
- **eksctl** (optional) for EKS cluster management

### AWS Account Requirements

- Active AWS account with appropriate permissions
- IAM user with permissions for:
  - Amazon ECR (Elastic Container Registry)
  - Amazon EKS (Elastic Kubernetes Service)
  - EC2, VPC, IAM (for EKS cluster)
  - Application Load Balancer (ALB)

### System Requirements

- **Operating System**: Linux, macOS, or Windows 10/11
- **RAM**: Minimum 8GB (16GB recommended)
- **Disk Space**: At least 20GB free space
- **Network**: Stable internet connection for pulling dependencies and Docker images

---

## Local Development Setup

### 1. Clone or Navigate to Project Directory

```bash
cd /modernize-data/studio-data/TNT1001/APP1207/transformed-code/374/studio-workspace/comp1
```

### 2. Build the Application Locally

```bash
# Using Maven
mvn clean package

# The built JAR will be in target/mini-java-app-1.0.0.jar
```

### 3. Run Application Locally (without Docker)

```bash
java -jar target/mini-java-app-1.0.0.jar
```

The application should start on port 8080.

### 4. Test Application

```bash
curl http://localhost:8080/mini-app
```

---

## Building and Testing with Docker

### 1. Build Docker Image Locally

```bash
docker build -t mini-java-app:local .
```

### 2. Run with Docker Compose

Update the `docker-compose.yml` file with your environment-specific values, then:

```bash
docker-compose up -d
```

### 3. View Logs

```bash
docker-compose logs -f mini-java-app
```

### 4. Stop and Clean Up

```bash
docker-compose down
```

---

## AWS EKS Prerequisites

### 1. Install AWS CLI

**Linux/macOS:**
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

**Windows:**
Download and install from: https://aws.amazon.com/cli/

### 2. Configure AWS CLI

```bash
aws configure
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Enter default region (e.g., us-east-1)
# Enter default output format (json)
```

### 3. Install kubectl

**Linux:**
```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

**macOS:**
```bash
brew install kubectl
```

**Windows:**
```powershell
choco install kubernetes-cli
```

### 4. Create EKS Cluster (if not exists)

Using eksctl:
```bash
eksctl create cluster \
  --name mini-java-app-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 2 \
  --nodes-max 4 \
  --managed
```

Or use the AWS Console to create an EKS cluster.

### 5. Install AWS Load Balancer Controller

The AWS Load Balancer Controller is required for ALB Ingress:

```bash
# Create IAM policy
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.6.0/docs/install/iam_policy.json

aws iam create-policy \
    --policy-name AWSLoadBalancerControllerIAMPolicy \
    --policy-document file://iam_policy.json

# Install using Helm
helm repo add eks https://aws.github.io/eks-charts
helm repo update

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=mini-java-app-cluster \
  --set serviceAccount.create=true \
  --set serviceAccount.name=aws-load-balancer-controller
```

---

## Building and Pushing Docker Image

### Using Automated Scripts

#### Linux/macOS

```bash
cd /modernize-data/studio-data/TNT1001/APP1207/transformed-code/374/studio-workspace/comp1
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

#### Windows

```cmd
cd C:\path\to\project
scripts\build-push.bat
```

### Manual Build and Push (AWS ECR)

```bash
# Set variables
REGION="us-east-1"
ACCOUNT_ID="123456789012"
REPO_NAME="mini-java-app"
IMAGE_TAG="latest"

# Authenticate with ECR
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com

# Create ECR repository (if not exists)
aws ecr create-repository --repository-name $REPO_NAME --region $REGION || true

# Build image
docker build -t $REPO_NAME:$IMAGE_TAG .

# Tag image
docker tag $REPO_NAME:$IMAGE_TAG $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$REPO_NAME:$IMAGE_TAG

# Push image
docker push $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$REPO_NAME:$IMAGE_TAG
```

---

## Deploying to AWS EKS

### Using Automated Deployment Script

#### Linux/macOS

```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

The script will prompt you for:
- AWS Region
- EKS Cluster Name
- Docker Image URI
- Environment variables for external services

#### Windows

```cmd
scripts\deploy-image.bat
```

### Manual Deployment

1. **Configure kubectl**

```bash
aws eks update-kubeconfig --region us-east-1 --name mini-java-app-cluster
```

2. **Update Kubernetes Manifests**

Edit `kubernetes/deployment.yaml` and replace placeholders:
- `{{IMAGE_URI}}`: Your full Docker image URI
- `{{DB_URL}}`, `{{DB_USERNAME}}`, `{{DB_PASSWORD}}`: Database credentials
- `{{REDIS_HOST}}`, `{{REDIS_PASSWORD}}`: Redis configuration
- Other environment variables as needed

3. **Apply Manifests**

```bash
# Create namespace
kubectl apply -f kubernetes/namespace.yaml

# Create deployment
kubectl apply -f kubernetes/deployment.yaml

# Create service
kubectl apply -f kubernetes/service.yaml

# Create ingress
kubectl apply -f kubernetes/ingress.yaml
```

4. **Wait for Deployment**

```bash
kubectl rollout status deployment/mini-java-app -n mini-java-app
```

---

## Verifying Deployment

### Check Pod Status

```bash
kubectl get pods -n mini-java-app
```

Expected output:
```
NAME                             READY   STATUS    RESTARTS   AGE
mini-java-app-xxxxxxxxx-xxxxx    1/1     Running   0          2m
mini-java-app-xxxxxxxxx-xxxxx    1/1     Running   0          2m
```

### Check Service

```bash
kubectl get svc -n mini-java-app
```

### Check Ingress and Get URL

```bash
kubectl get ingress -n mini-java-app
```

Get the ALB hostname:
```bash
kubectl get ingress mini-java-app-ingress -n mini-java-app -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

### View Application Logs

```bash
kubectl logs -n mini-java-app -l app=mini-java-app --tail=100 -f
```

### Test Application

Once the ALB is provisioned (may take 5-10 minutes):

```bash
curl http://<ALB-HOSTNAME>/mini-app
```

---

## Configuration Management

### Environment Variables

All application configuration is managed through environment variables defined in `kubernetes/deployment.yaml`.

**Critical Variables:**

- **Database**: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- **Redis Cache**: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- **External APIs**: `EXTERNAL_API_URL`, `EXTERNAL_API_KEY`
- **Payment Service**: `PAYMENT_SERVICE_URL`, `PAYMENT_SERVICE_USERNAME`, `PAYMENT_SERVICE_PASSWORD`
- **RabbitMQ**: `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`
- **Monitoring**: `MONITORING_URL`, `MONITORING_USERNAME`, `MONITORING_PASSWORD`
- **Security**: `JWT_SECRET`, `ENCRYPTION_KEY`

### Using Kubernetes Secrets (Recommended)

For sensitive data, use Kubernetes Secrets instead of plain environment variables:

```bash
# Create secret for database credentials
kubectl create secret generic db-credentials \
  --from-literal=username=root \
  --from-literal=password=your-password \
  -n mini-java-app

# Update deployment.yaml to reference the secret
env:
- name: DB_USERNAME
  valueFrom:
    secretKeyRef:
      name: db-credentials
      key: username
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: db-credentials
      key: password
```

### ConfigMaps for Non-Sensitive Configuration

```bash
kubectl create configmap app-config \
  --from-literal=log-level=INFO \
  --from-literal=environment=production \
  -n mini-java-app
```

---

## Troubleshooting

### Pod Not Starting

**Check pod events:**
```bash
kubectl describe pod <pod-name> -n mini-java-app
```

**Common issues:**
- Image pull errors: Verify ECR permissions and image URI
- Resource limits: Check if pod has enough CPU/memory
- Configuration errors: Verify environment variables

### Application Crashes

**View logs:**
```bash
kubectl logs <pod-name> -n mini-java-app --previous
```

**Common causes:**
- Database connection failures
- Missing required environment variables
- JVM memory issues (adjust `JAVA_OPTS`)

### Service Not Accessible

**Check service endpoints:**
```bash
kubectl get endpoints -n mini-java-app
```

**Verify ingress:**
```bash
kubectl describe ingress mini-java-app-ingress -n mini-java-app
```

**Check ALB Controller logs:**
```bash
kubectl logs -n kube-system -l app.kubernetes.io/name=aws-load-balancer-controller
```

### Performance Issues

**Check resource usage:**
```bash
kubectl top pods -n mini-java-app
```

**Scale up if needed:**
```bash
kubectl scale deployment mini-java-app --replicas=4 -n mini-java-app
```

---

## Scaling and Management

### Manual Scaling

```bash
kubectl scale deployment mini-java-app --replicas=5 -n mini-java-app
```

### Horizontal Pod Autoscaler (HPA)

Create HPA based on CPU utilization:

```bash
kubectl autoscale deployment mini-java-app \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n mini-java-app
```

### Rolling Updates

```bash
# Update image
kubectl set image deployment/mini-java-app \
  mini-java-app=<new-image-uri> \
  -n mini-java-app

# Monitor rollout
kubectl rollout status deployment/mini-java-app -n mini-java-app
```

### Rollback

```bash
# View rollout history
kubectl rollout history deployment/mini-java-app -n mini-java-app

# Rollback to previous version
kubectl rollout undo deployment/mini-java-app -n mini-java-app

# Rollback to specific revision
kubectl rollout undo deployment/mini-java-app --to-revision=2 -n mini-java-app
```

---

## Security Considerations

### 1. Container Security

- Application runs as non-root user (`appuser`)
- Use minimal base image (eclipse-temurin:11-jre)
- Regularly update base images for security patches

### 2. Network Security

- Use Kubernetes Network Policies to restrict pod-to-pod communication
- Configure Security Groups for EKS nodes
- Use AWS WAF with Application Load Balancer

### 3. Secret Management

- Store sensitive data in Kubernetes Secrets or AWS Secrets Manager
- Enable encryption at rest for Kubernetes Secrets
- Rotate credentials regularly

### 4. IAM and RBAC

- Use IAM roles for service accounts (IRSA)
- Implement least-privilege access with Kubernetes RBAC
- Regularly audit IAM policies

### 5. Image Security

- Scan images for vulnerabilities (AWS ECR image scanning)
- Use image signing and verification
- Implement image pull policies

---

## Technology-Specific Notes

### Java 11 Application

**JVM Configuration:**

The application uses the following JVM settings optimized for containers:

```bash
JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

- `UseContainerSupport`: Enables container-aware JVM behavior
- `MaxRAMPercentage`: Limits heap to 75% of container memory
- Adjust based on your application's memory requirements

**Heap Size Tuning:**

If your application requires more memory:

```yaml
env:
- name: JAVA_OPTS
  value: "-Xmx1024m -Xms512m -XX:+UseContainerSupport"
resources:
  requests:
    memory: "1Gi"
  limits:
    memory: "2Gi"
```

**Garbage Collection:**

For production, consider G1GC (default in Java 11) or ZGC:

```bash
JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Maven Build Optimization

**Dependency Caching:**

The Dockerfile is optimized for dependency caching:

```dockerfile
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests
```

This ensures dependencies are only re-downloaded when `pom.xml` changes.

### Spring Boot Specific

Although this is a basic Java application, if you're using Spring Boot:

**Actuator Endpoints:**

Add Spring Boot Actuator for health checks:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
```

**Profile Management:**

```yaml
env:
- name: SPRING_PROFILES_ACTIVE
  value: "production"
```

---

## Additional Resources

- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [Java Containerization Best Practices](https://docs.oracle.com/en/java/javase/11/docs/)

---

## Support and Maintenance

### Monitoring

- Set up CloudWatch Container Insights for EKS
- Configure Prometheus and Grafana for metrics
- Implement centralized logging with CloudWatch Logs or ELK stack

### Backup and Disaster Recovery

- Regular backups of persistent data
- Document disaster recovery procedures
- Test recovery processes regularly

### Cost Optimization

- Use AWS Spot Instances for non-critical workloads
- Implement cluster autoscaling
- Regularly review and optimize resource requests/limits
- Use AWS Cost Explorer to monitor spending

---

## Cleanup

### Delete Kubernetes Resources

```bash
kubectl delete namespace mini-java-app
```

### Delete ECR Repository

```bash
aws ecr delete-repository --repository-name mini-java-app --region us-east-1 --force
```

### Delete EKS Cluster

```bash
eksctl delete cluster --name mini-java-app-cluster --region us-east-1
```

---

**Document Version:** 1.0  
**Last Updated:** 2025-01-25  
**Platform:** AWS EKS (Kubernetes)  
**Application:** Mini Java Application (Java 11 + Maven)
