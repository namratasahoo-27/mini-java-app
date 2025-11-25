@echo off
setlocal enabledelayedexpansion

echo === AWS EKS Deployment Script ===
echo.

REM Prompt for AWS configuration
set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
if "!AWS_REGION!"=="" (
    echo AWS Region is required
    exit /b 1
)

set /p CLUSTER_NAME="Enter EKS Cluster Name: "
if "!CLUSTER_NAME!"=="" (
    echo EKS Cluster Name is required
    exit /b 1
)

echo.
set /p IMAGE_URI="Enter Docker Image URI (with tag): "
if "!IMAGE_URI!"=="" (
    echo Docker Image URI is required
    exit /b 1
)

echo.
echo === Environment Variables Configuration ===
echo Please provide values for the following environment variables.
echo Press Enter to use default values.
echo.

REM Database configuration
set /p DB_URL="Enter DB_URL (default: jdbc:mysql://localhost:3306/mini_app_db): "
if "!DB_URL!"=="" set DB_URL=jdbc:mysql://localhost:3306/mini_app_db

set /p DB_USERNAME="Enter DB_USERNAME (default: root): "
if "!DB_USERNAME!"=="" set DB_USERNAME=root

set /p DB_PASSWORD="Enter DB_PASSWORD: "
if "!DB_PASSWORD!"=="" set DB_PASSWORD=password123

REM Redis configuration
set /p REDIS_HOST="Enter REDIS_HOST (default: localhost): "
if "!REDIS_HOST!"=="" set REDIS_HOST=localhost

set /p REDIS_PORT="Enter REDIS_PORT (default: 6379): "
if "!REDIS_PORT!"=="" set REDIS_PORT=6379

set /p REDIS_PASSWORD="Enter REDIS_PASSWORD: "
if "!REDIS_PASSWORD!"=="" set REDIS_PASSWORD=redis_secret_123

REM External API configuration
set /p EXTERNAL_API_URL="Enter EXTERNAL_API_URL (default: http://api.example.com:8080/v1): "
if "!EXTERNAL_API_URL!"=="" set EXTERNAL_API_URL=http://api.example.com:8080/v1

set /p EXTERNAL_API_KEY="Enter EXTERNAL_API_KEY: "
if "!EXTERNAL_API_KEY!"=="" set EXTERNAL_API_KEY=hardcoded_api_key_12345

REM Payment service configuration
set /p PAYMENT_SERVICE_URL="Enter PAYMENT_SERVICE_URL: "
if "!PAYMENT_SERVICE_URL!"=="" set PAYMENT_SERVICE_URL=https://payment.internal.company.com/process

set /p PAYMENT_SERVICE_USERNAME="Enter PAYMENT_SERVICE_USERNAME: "
if "!PAYMENT_SERVICE_USERNAME!"=="" set PAYMENT_SERVICE_USERNAME=payment_user

set /p PAYMENT_SERVICE_PASSWORD="Enter PAYMENT_SERVICE_PASSWORD: "
if "!PAYMENT_SERVICE_PASSWORD!"=="" set PAYMENT_SERVICE_PASSWORD=payment_secret_456

REM RabbitMQ configuration
set /p RABBITMQ_HOST="Enter RABBITMQ_HOST (default: localhost): "
if "!RABBITMQ_HOST!"=="" set RABBITMQ_HOST=localhost

set /p RABBITMQ_PORT="Enter RABBITMQ_PORT (default: 5672): "
if "!RABBITMQ_PORT!"=="" set RABBITMQ_PORT=5672

set /p RABBITMQ_USERNAME="Enter RABBITMQ_USERNAME: "
if "!RABBITMQ_USERNAME!"=="" set RABBITMQ_USERNAME=rabbitmq_user

set /p RABBITMQ_PASSWORD="Enter RABBITMQ_PASSWORD: "
if "!RABBITMQ_PASSWORD!"=="" set RABBITMQ_PASSWORD=rabbitmq_secret

REM Monitoring configuration
set /p MONITORING_URL="Enter MONITORING_URL: "
if "!MONITORING_URL!"=="" set MONITORING_URL=http://monitoring.internal.company.com:9090/metrics

set /p MONITORING_USERNAME="Enter MONITORING_USERNAME: "
if "!MONITORING_USERNAME!"=="" set MONITORING_USERNAME=monitor_user

set /p MONITORING_PASSWORD="Enter MONITORING_PASSWORD: "
if "!MONITORING_PASSWORD!"=="" set MONITORING_PASSWORD=monitor_pass

REM Security configuration
set /p JWT_SECRET="Enter JWT_SECRET: "
if "!JWT_SECRET!"=="" set JWT_SECRET=my_super_secret_jwt_key_123456789

set /p ENCRYPTION_KEY="Enter ENCRYPTION_KEY: "
if "!ENCRYPTION_KEY!"=="" set ENCRYPTION_KEY=encryption_key_hardcoded

echo.
echo Configuring kubectl for EKS cluster...
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!

if !ERRORLEVEL! neq 0 (
    echo Failed to configure kubectl for EKS cluster
    exit /b 1
)

echo kubectl configured successfully
echo.

REM Verify cluster connectivity
echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo Failed to connect to Kubernetes cluster
    exit /b 1
)

echo Successfully connected to cluster
echo.

REM Update Kubernetes manifests
echo Updating Kubernetes manifests...

REM Create temporary directory
set TMP_DIR=%TEMP%\k8s-deploy-%RANDOM%
mkdir !TMP_DIR!

REM Copy manifests
xcopy /E /I kubernetes\* !TMP_DIR!\

REM Replace placeholders using PowerShell
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{DB_URL}}', '!DB_URL!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{DB_USERNAME}}', '!DB_USERNAME!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{DB_PASSWORD}}', '!DB_PASSWORD!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{REDIS_PASSWORD}}', '!REDIS_PASSWORD!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{EXTERNAL_API_URL}}', '!EXTERNAL_API_URL!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{EXTERNAL_API_KEY}}', '!EXTERNAL_API_KEY!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{PAYMENT_SERVICE_URL}}', '!PAYMENT_SERVICE_URL!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{PAYMENT_SERVICE_USERNAME}}', '!PAYMENT_SERVICE_USERNAME!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{PAYMENT_SERVICE_PASSWORD}}', '!PAYMENT_SERVICE_PASSWORD!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{RABBITMQ_HOST}}', '!RABBITMQ_HOST!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{RABBITMQ_PORT}}', '!RABBITMQ_PORT!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{RABBITMQ_USERNAME}}', '!RABBITMQ_USERNAME!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{RABBITMQ_PASSWORD}}', '!RABBITMQ_PASSWORD!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{MONITORING_URL}}', '!MONITORING_URL!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{MONITORING_USERNAME}}', '!MONITORING_USERNAME!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{MONITORING_PASSWORD}}', '!MONITORING_PASSWORD!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{JWT_SECRET}}', '!JWT_SECRET!' | Set-Content '!TMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TMP_DIR!\deployment.yaml') -replace '{{ENCRYPTION_KEY}}', '!ENCRYPTION_KEY!' | Set-Content '!TMP_DIR!\deployment.yaml'"

echo Manifests updated successfully
echo.

REM Apply Kubernetes manifests
echo Applying Kubernetes manifests...
echo.

echo Creating namespace...
kubectl apply -f !TMP_DIR!\namespace.yaml
echo.

echo Creating deployment...
kubectl apply -f !TMP_DIR!\deployment.yaml
echo.

echo Creating service...
kubectl apply -f !TMP_DIR!\service.yaml
echo.

echo Creating ingress...
kubectl apply -f !TMP_DIR!\ingress.yaml
echo.

echo All manifests applied successfully
echo.

REM Wait for deployment rollout
echo Waiting for deployment to complete...
kubectl rollout status deployment/mini-java-app -n mini-java-app --timeout=5m

if !ERRORLEVEL! neq 0 (
    echo Deployment rollout failed or timed out
    echo Checking pod status...
    kubectl get pods -n mini-java-app
    echo.
    echo Pod logs:
    kubectl logs -n mini-java-app -l app=mini-java-app --tail=50
    rmdir /S /Q !TMP_DIR!
    exit /b 1
)

echo Deployment completed successfully
echo.

REM Verify deployment
echo Verifying deployment...
echo.
echo Pods:
kubectl get pods -n mini-java-app
echo.
echo Services:
kubectl get svc -n mini-java-app
echo.
echo Ingress:
kubectl get ingress -n mini-java-app
echo.

echo === Deployment Successful ===
echo.
echo Application Details:
echo   Namespace: mini-java-app
echo   Deployment: mini-java-app
echo   Service: mini-java-app-service
echo.
echo Useful commands:
echo   View pods: kubectl get pods -n mini-java-app
echo   View logs: kubectl logs -n mini-java-app -l app=mini-java-app
echo   View services: kubectl get svc -n mini-java-app
echo   Delete deployment: kubectl delete namespace mini-java-app
echo.

REM Cleanup
rmdir /S /Q !TMP_DIR!

pause
