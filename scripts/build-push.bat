@echo off
setlocal enabledelayedexpansion

echo === Docker Build and Push Script ===
echo.

REM Project configuration
set PROJECT_NAME=mini-java-app

REM Sanitize image name to lowercase and replace invalid characters
set IMAGE_NAME=%PROJECT_NAME%
for %%i in (A B C D E F G H I J K L M N O P Q R S T U V W X Y Z) do (
    set IMAGE_NAME=!IMAGE_NAME:%%i=%%i!
)
set IMAGE_NAME=%IMAGE_NAME: =-% 
set IMAGE_NAME=%IMAGE_NAME:_=-%
powershell -Command "$name = '%IMAGE_NAME%'; $name = $name.ToLower(); $name = $name -replace '[^a-z0-9-]', '-'; $name = $name.Trim('-'); Write-Output $name" > temp_image_name.txt
set /p IMAGE_NAME=<temp_image_name.txt
del temp_image_name.txt

echo Project: %PROJECT_NAME%
echo Sanitized Image Name: %IMAGE_NAME%
echo.

REM Prompt for image tag
set /p IMAGE_TAG="Enter image tag (default: latest): "
if "!IMAGE_TAG!"==" " set IMAGE_TAG=latest

REM Sanitize tag
powershell -Command "$tag = '%IMAGE_TAG%'; $tag = $tag.ToLower(); $tag = $tag -replace '[^a-z0-9.-]', '-'; $tag = $tag.Trim('-'); if ([string]::IsNullOrWhiteSpace($tag)) { $tag = 'latest' }; Write-Output $tag" > temp_tag.txt
set /p IMAGE_TAG=<temp_tag.txt
del temp_tag.txt

echo Sanitized Image Tag: !IMAGE_TAG!
echo.

REM Registry selection
echo Select Docker Registry:
echo 1. AWS ECR (Elastic Container Registry)
echo 2. Docker Hub
set /p REGISTRY_CHOICE="Enter choice (1 or 2): "

if "!REGISTRY_CHOICE!"=="1" (
    echo.
    echo === AWS ECR Configuration ===
    
    REM AWS ECR configuration
    set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
    set /p AWS_ACCOUNT_ID="Enter AWS Account ID: "
    set /p ECR_REPO="Enter ECR Repository Name (default: %IMAGE_NAME%): "
    if "!ECR_REPO!"=="" set ECR_REPO=%IMAGE_NAME%
    
    set REGISTRY_URL=!AWS_ACCOUNT_ID!.dkr.ecr.!AWS_REGION!.amazonaws.com
    set FULL_IMAGE_NAME=!REGISTRY_URL!/!ECR_REPO!:!IMAGE_TAG!
    
    echo.
    echo Full Image Name: !FULL_IMAGE_NAME!
    echo.
    
    REM Authenticate with AWS ECR
    echo Authenticating with AWS ECR...
    for /f "delims=" %%i in ('aws ecr get-login-password --region !AWS_REGION!') do set ECR_PASSWORD=%%i
    echo !ECR_PASSWORD! | docker login --username AWS --password-stdin !REGISTRY_URL!
    
    if !ERRORLEVEL! neq 0 (
        echo Failed to authenticate with AWS ECR
        exit /b 1
    )
    
    echo Successfully authenticated with AWS ECR
    echo.
    
    REM Check if ECR repository exists
    echo Checking ECR repository...
    aws ecr describe-repositories --repository-names !ECR_REPO! --region !AWS_REGION! >nul 2>&1
    if !ERRORLEVEL! neq 0 (
        echo Repository does not exist. Creating ECR repository: !ECR_REPO!
        aws ecr create-repository --repository-name !ECR_REPO! --region !AWS_REGION!
        if !ERRORLEVEL! neq 0 (
            echo Failed to create ECR repository
            exit /b 1
        )
        echo ECR repository created successfully
    )
    echo.
    
) else if "!REGISTRY_CHOICE!"=="2" (
    echo.
    echo === Docker Hub Configuration ===
    
    REM Docker Hub configuration
    set /p DOCKER_USERNAME="Enter Docker Hub Username: "
    set /p DOCKER_PASSWORD="Enter Docker Hub Password or Access Token: "
    
    set FULL_IMAGE_NAME=!DOCKER_USERNAME!/%IMAGE_NAME%:!IMAGE_TAG!
    
    echo.
    echo Full Image Name: !FULL_IMAGE_NAME!
    echo.
    
    REM Authenticate with Docker Hub
    echo Authenticating with Docker Hub...
    echo !DOCKER_PASSWORD! | docker login --username !DOCKER_USERNAME! --password-stdin
    
    if !ERRORLEVEL! neq 0 (
        echo Failed to authenticate with Docker Hub
        exit /b 1
    )
    
    echo Successfully authenticated with Docker Hub
    echo.
    
) else (
    echo Invalid choice. Exiting.
    exit /b 1
)

REM Build Docker image
echo Building Docker image...
echo Running: docker build -t !FULL_IMAGE_NAME! .
echo.

docker build -t !FULL_IMAGE_NAME! .

if !ERRORLEVEL! neq 0 (
    echo Docker build failed
    exit /b 1
)

echo.
echo Docker image built successfully: !FULL_IMAGE_NAME!
echo.

REM Push Docker image
echo Pushing Docker image to registry...
docker push !FULL_IMAGE_NAME!

if !ERRORLEVEL! neq 0 (
    echo Docker push failed
    exit /b 1
)

echo.
echo === Build and Push Completed Successfully ===
echo Image: !FULL_IMAGE_NAME!
echo.
echo Next steps:
echo 1. Use this image URI in your Kubernetes deployment
echo 2. Run deploy-image.bat to deploy to AWS EKS
echo.

pause
