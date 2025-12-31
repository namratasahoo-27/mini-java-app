@echo off
setlocal enabledelayedexpansion

echo =================================================
echo AWS ECS Fargate Deployment Script
echo Application: mini-java-app
echo =================================================
echo.

set PROJECT_NAME=mini-java-app
set TASK_FAMILY=!PROJECT_NAME!-task
set SERVICE_NAME=!PROJECT_NAME!-service
set CONTAINER_NAME=!PROJECT_NAME!
set CONTAINER_PORT=8080

set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
if "!AWS_REGION!"=="" (
    echo Error: AWS Region is required
    exit /b 1
)
set AWS_DEFAULT_REGION=!AWS_REGION!

echo Retrieving AWS Account ID...
for /f "delims=" %%i in ('aws sts get-caller-identity --query Account --output text') do set ACCOUNT_ID=%%i
if "!ACCOUNT_ID!"=="" (
    echo Error: Failed to retrieve AWS Account ID
    exit /b 1
)
echo AWS Account ID: !ACCOUNT_ID!

set /p CLUSTER_NAME="Enter ECS Cluster Name (will be created if doesn't exist): "
if "!CLUSTER_NAME!"=="" (
    echo Error: Cluster name is required
    exit /b 1
)

echo Checking if ECS cluster exists...
aws ecs describe-clusters --clusters !CLUSTER_NAME! --region !AWS_REGION! >nul 2>&1
if !ERRORLEVEL! neq 0 (
    echo Cluster does not exist. Creating ECS cluster...
    aws ecs create-cluster --cluster-name !CLUSTER_NAME! --region !AWS_REGION!
    echo ECS cluster created successfully
)

set /p VPC_ID="Enter VPC ID: "
if "!VPC_ID!"=="" (
    echo Error: VPC ID is required
    exit /b 1
)

set /p SUBNETS_INPUT="Enter Subnet IDs (comma-separated, at least 2 for HA): "
if "!SUBNETS_INPUT!"=="" (
    echo Error: At least 2 subnet IDs are required
    exit /b 1
)

REM Parse subnets
for /f "tokens=1,2 delims=," %%a in ("!SUBNETS_INPUT!") do (
    set SUBNET_1=%%a
    set SUBNET_2=%%b
)
set SUBNET_1=!SUBNET_1: =!
set SUBNET_2=!SUBNET_2: =!

if "!SUBNET_1!"=="" (
    echo Error: At least 2 subnet IDs are required
    exit /b 1
)
if "!SUBNET_2!"=="" (
    echo Error: At least 2 subnet IDs are required
    exit /b 1
)

set /p SECURITY_GROUP="Enter Security Group ID (must allow inbound on port !CONTAINER_PORT!): "
if "!SECURITY_GROUP!"=="" (
    echo Error: Security Group ID is required
    exit /b 1
)

set /p IMAGE_URI="Enter Docker Image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/mini-java-app:latest): "
if "!IMAGE_URI!"=="" (
    echo Error: Image URI is required
    exit /b 1
)

echo.
echo Load Balancer Configuration
set /p NEED_ALB="Do you need an Application Load Balancer for this service? (y/n): "

set USE_LOAD_BALANCER=false
if /i "!NEED_ALB!"=="y" (
    echo Creating Application Load Balancer...
    
    set ALB_NAME=!PROJECT_NAME!-alb
    for /f "delims=" %%i in ('aws elbv2 create-load-balancer --name !ALB_NAME! --subnets !SUBNET_1! !SUBNET_2! --security-groups !SECURITY_GROUP! --scheme internet-facing --type application --ip-address-type ipv4 --region !AWS_REGION! --query "LoadBalancers[0].LoadBalancerArn" --output text 2^>nul') do set ALB_ARN=%%i
    
    if "!ALB_ARN!"=="" (
        echo ALB may already exist, retrieving existing ALB...
        for /f "delims=" %%i in ('aws elbv2 describe-load-balancers --names !ALB_NAME! --region !AWS_REGION! --query "LoadBalancers[0].LoadBalancerArn" --output text') do set ALB_ARN=%%i
    )
    
    echo ALB ARN: !ALB_ARN!
    
    for /f "delims=" %%i in ('aws elbv2 describe-load-balancers --load-balancer-arns !ALB_ARN! --region !AWS_REGION! --query "LoadBalancers[0].DNSName" --output text') do set ALB_DNS=%%i
    
    set TG_NAME=!PROJECT_NAME!-tg
    for /f "delims=" %%i in ('aws elbv2 create-target-group --name !TG_NAME! --protocol HTTP --port !CONTAINER_PORT! --vpc-id !VPC_ID! --target-type ip --health-check-enabled --health-check-protocol HTTP --health-check-path / --health-check-interval-seconds 30 --health-check-timeout-seconds 5 --healthy-threshold-count 2 --unhealthy-threshold-count 3 --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text 2^>nul') do set TARGET_GROUP_ARN=%%i
    
    if "!TARGET_GROUP_ARN!"=="" (
        echo Target Group may already exist, retrieving existing TG...
        for /f "delims=" %%i in ('aws elbv2 describe-target-groups --names !TG_NAME! --region !AWS_REGION! --query "TargetGroups[0].TargetGroupArn" --output text') do set TARGET_GROUP_ARN=%%i
    )
    
    echo Target Group ARN: !TARGET_GROUP_ARN!
    
    aws elbv2 create-listener --load-balancer-arn !ALB_ARN! --protocol HTTP --port 80 --default-actions Type=forward,TargetGroupArn=!TARGET_GROUP_ARN! --region !AWS_REGION! >nul 2>&1
    
    set USE_LOAD_BALANCER=true
) else (
    echo Skipping Load Balancer creation
)

echo.
echo Creating CloudWatch Log Group...
set LOG_GROUP=/ecs/!PROJECT_NAME!
aws logs create-log-group --log-group-name !LOG_GROUP! --region !AWS_REGION! >nul 2>&1
echo Log Group: !LOG_GROUP!

echo.
echo Preparing ECS Task Definition...
set TASK_DEF_FILE=ecs\task-definition.json

if not exist "!TASK_DEF_FILE!" (
    echo Error: Task definition file not found: !TASK_DEF_FILE!
    exit /b 1
)

set TASK_DEF_TEMP=%TEMP%\task-definition-!PROJECT_NAME!.json
powershell -Command "(Get-Content '!TASK_DEF_FILE!') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' -replace '{{AWS_REGION}}', '!AWS_REGION!' -replace '{{ACCOUNT_ID}}', '!ACCOUNT_ID!' | Set-Content '!TASK_DEF_TEMP!'"

echo Registering ECS Task Definition...
for /f "delims=" %%i in ('aws ecs register-task-definition --cli-input-json file://!TASK_DEF_TEMP! --region !AWS_REGION! --query "taskDefinition.taskDefinitionArn" --output text') do set TASK_DEF_ARN=%%i

if "!TASK_DEF_ARN!"=="" (
    echo Error: Failed to register task definition
    exit /b 1
)

echo Task Definition registered: !TASK_DEF_ARN!

echo.
echo Preparing ECS Service Definition...
set SERVICE_DEF_FILE=ecs\service-definition.json

if not exist "!SERVICE_DEF_FILE!" (
    echo Error: Service definition file not found: !SERVICE_DEF_FILE!
    exit /b 1
)

set SERVICE_DEF_TEMP=%TEMP%\service-definition-!PROJECT_NAME!.json
powershell -Command "(Get-Content '!SERVICE_DEF_FILE!') -replace '{{CLUSTER_NAME}}', '!CLUSTER_NAME!' -replace '{{SUBNET_1}}', '!SUBNET_1!' -replace '{{SUBNET_2}}', '!SUBNET_2!' -replace '{{SECURITY_GROUP}}', '!SECURITY_GROUP!' | Set-Content '!SERVICE_DEF_TEMP!'"

if "!USE_LOAD_BALANCER!"=="false" (
    powershell -Command "$json = Get-Content '!SERVICE_DEF_TEMP!' | ConvertFrom-Json; $json.PSObject.Properties.Remove('loadBalancers'); $json.PSObject.Properties.Remove('healthCheckGracePeriodSeconds'); $json | ConvertTo-Json -Depth 10 | Set-Content '!SERVICE_DEF_TEMP!'"
) else (
    powershell -Command "(Get-Content '!SERVICE_DEF_TEMP!') -replace '{{TARGET_GROUP_ARN}}', '!TARGET_GROUP_ARN!' | Set-Content '!SERVICE_DEF_TEMP!'"
)

echo.
echo Checking if ECS service exists...
for /f "delims=" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[?status=='ACTIVE'].serviceName" --output text') do set EXISTING_SERVICE=%%i

if "!EXISTING_SERVICE!"=="" (
    echo Creating new ECS service...
    aws ecs create-service --cli-input-json file://!SERVICE_DEF_TEMP! --region !AWS_REGION! >nul
    echo ECS service created successfully
) else (
    echo Updating existing ECS service...
    aws ecs update-service --cluster !CLUSTER_NAME! --service !SERVICE_NAME! --task-definition !TASK_DEF_ARN! --force-new-deployment --region !AWS_REGION! >nul
    echo ECS service updated successfully
)

echo.
echo Waiting for service to stabilize (this may take several minutes)...
aws ecs wait services-stable --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!
echo Service is stable

echo.
echo Verifying deployment...
for /f "delims=" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[0].runningCount" --output text') do set RUNNING_COUNT=%%i
for /f "delims=" %%i in ('aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION! --query "services[0].desiredCount" --output text') do set DESIRED_COUNT=%%i

echo Running Tasks: !RUNNING_COUNT! / !DESIRED_COUNT!

echo.
echo =================================================
echo Deployment Completed Successfully!
echo =================================================
echo Cluster: !CLUSTER_NAME!
echo Service: !SERVICE_NAME!
echo Task Definition: !TASK_DEF_ARN!
echo Region: !AWS_REGION!
echo Running Tasks: !RUNNING_COUNT! / !DESIRED_COUNT!

if "!USE_LOAD_BALANCER!"=="true" (
    echo Load Balancer DNS: !ALB_DNS!
    echo Application URL: http://!ALB_DNS!
)

echo CloudWatch Logs: !LOG_GROUP!
echo.
echo To view logs:
echo aws logs tail !LOG_GROUP! --follow --region !AWS_REGION!
echo.
echo To view service details:
echo aws ecs describe-services --cluster !CLUSTER_NAME! --services !SERVICE_NAME! --region !AWS_REGION!
echo.

del /f /q !TASK_DEF_TEMP! !SERVICE_DEF_TEMP! >nul 2>&1

echo Deployment script completed successfully!

endlocal
