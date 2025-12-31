@echo off
setlocal enabledelayedexpansion

echo ===========================================
echo Docker Build and Push Script
echo ===========================================
echo.

set PROJECT_NAME=mini-java-app

REM Sanitize project name for Docker tag using PowerShell
for /f "delims=" %%i in ('powershell -Command "'%PROJECT_NAME%'.ToLower() -replace '[^a-z0-9]+', '-' -replace '^-+', '' -replace '-+$', ''"') do set IMAGE_NAME=%%i

echo Select Container Registry:
echo 1. AWS ECR (Elastic Container Registry)
echo 2. Docker Hub
set /p REGISTRY_CHOICE="Enter choice (1 or 2): "

if "!REGISTRY_CHOICE!"=="1" (
    echo.
    echo AWS ECR Configuration
    echo.
    
    set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
    if "!AWS_REGION!"=="" (
        echo Error: AWS Region is required
        exit /b 1
    )
    
    echo Retrieving AWS Account ID...
    for /f "delims=" %%i in ('aws sts get-caller-identity --query Account --output text') do set AWS_ACCOUNT_ID=%%i
    if "!AWS_ACCOUNT_ID!"=="" (
        echo Error: Failed to retrieve AWS Account ID
        exit /b 1
    )
    echo AWS Account ID: !AWS_ACCOUNT_ID!
    
    set /p ECR_REPO="Enter ECR Repository Name (default: !IMAGE_NAME!): "
    if "!ECR_REPO!"=="" set ECR_REPO=!IMAGE_NAME!
    
    set REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com
    
    set /p IMAGE_TAG="Enter image tag (default: latest): "
    if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest
    
    REM Sanitize tag
    for /f "delims=" %%i in ('powershell -Command "'!IMAGE_TAG!'.ToLower() -replace '[^a-z0-9.-]+', '-' -replace '^-+', '' -replace '-+$', ''"') do set IMAGE_TAG=%%i
    if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest
    
    set FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!
    
    echo.
    echo Logging in to AWS ECR...
    aws ecr get-login-password --region !AWS_REGION! | docker login --username AWS --password-stdin !REGISTRY_URL!
    if !ERRORLEVEL! neq 0 (
        echo Error: ECR login failed
        exit /b 1
    )
    echo Successfully logged in to ECR
    
    echo Checking if ECR repository exists...
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Repository does not exist. Creating ECR repository...
        aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
        echo ECR repository created successfully
    )
    
) else if "!REGISTRY_CHOICE!"=="2" (
    echo.
    echo Docker Hub Configuration
    echo.
    
    set /p DOCKER_USERNAME="Enter Docker Hub Username: "
    if "!DOCKER_USERNAME!"=="" (
        echo Error: Docker Hub username is required
        exit /b 1
    )
    
    set /p DOCKER_PASSWORD="Enter Docker Hub Password/Token: "
    if "!DOCKER_PASSWORD!"=="" (
        echo Error: Docker Hub password is required
        exit /b 1
    )
    
    set /p REPO_NAME="Enter Repository Name (default: !IMAGE_NAME!): "
    if "!REPO_NAME!"=="" set REPO_NAME=!IMAGE_NAME!
    
    set /p IMAGE_TAG="Enter image tag (default: latest): "
    if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest
    
    REM Sanitize tag
    for /f "delims=" %%i in ('powershell -Command "'!IMAGE_TAG!'.ToLower() -replace '[^a-z0-9.-]+', '-' -replace '^-+', '' -replace '-+$', ''"') do set IMAGE_TAG=%%i
    if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest
    
    set FULL_IMAGE_NAME=!DOCKER_USERNAME!/!REPO_NAME!:!IMAGE_TAG!
    
    echo.
    echo Logging in to Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    if !ERRORLEVEL! neq 0 (
        echo Error: Docker Hub login failed
        exit /b 1
    )
    echo Successfully logged in to Docker Hub
    
) else (
    echo Invalid choice. Exiting.
    exit /b 1
)

echo.
echo Building Docker image: !FULL_IMAGE_NAME!
echo Build context: %CD%

docker build -t !FULL_IMAGE_NAME! .
if !ERRORLEVEL! neq 0 (
    echo Error: Docker build failed
    exit /b 1
)
echo Docker image built successfully

echo.
echo Pushing Docker image to registry...
docker push !FULL_IMAGE_NAME!
if !ERRORLEVEL! neq 0 (
    echo Error: Docker push failed
    exit /b 1
)

echo.
echo ===========================================
echo Build and Push Completed Successfully!
echo ===========================================
echo Image: !FULL_IMAGE_NAME!
echo.
echo Next Steps:
echo 1. Use this image URI for ECS deployment
echo 2. Update task definition with this image
echo 3. Run deploy-image.bat to deploy to ECS
echo.

endlocal
