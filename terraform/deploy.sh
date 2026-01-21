#!/bin/bash
set -e

# Unit Management Service Deployment Script (Lambda ZIP)
# This script builds the application as a ZIP package and deploys it to Lambda

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if required commands exist
check_prerequisites() {
    local missing=0

    if ! command -v aws &> /dev/null; then
        print_error "AWS CLI is not installed"
        missing=1
    fi

    if ! command -v terraform &> /dev/null; then
        print_error "Terraform is not installed"
        missing=1
    fi

    if ! command -v java &> /dev/null; then
        print_error "Java is not installed"
        missing=1
    fi

    if [ $missing -eq 1 ]; then
        exit 1
    fi
}

# Get AWS region from Terraform output
get_aws_region() {
    cd terraform
    local region=$(terraform output -raw aws_region 2>/dev/null || echo "us-east-1")
    cd ..
    echo "$region"
}

# Main deployment function
main() {
    print_info "Starting Lambda ZIP deployment process..."

    # Check prerequisites
    check_prerequisites

    # Get configuration
    AWS_REGION=$(get_aws_region)

    print_info "AWS Region: $AWS_REGION"

    # Build the application
    print_info "Building Quarkus application..."
    cd ..
    ./gradlew clean build -x test

    if [ $? -ne 0 ]; then
        print_error "Gradle build failed"
        exit 1
    fi

    print_info "Application built successfully"

    # Create deployment package
    print_info "Creating Lambda deployment package..."
    cd build

    # Remove old function.zip if it exists
    rm -f function.zip

    # Copy the uber-jar to function.zip
    if [ -f "quarkus-app/quarkus-run.jar" ]; then
        # Quarkus 3.x structure - package the entire quarkus-app directory
        cd quarkus-app
        zip -r ../function.zip . -x "*.original"
        cd ..
    elif [ -f "*-runner.jar" ]; then
        # Legacy structure - just zip the runner jar
        zip function.zip *-runner.jar
    else
        print_error "Could not find Quarkus output jar"
        exit 1
    fi

    if [ $? -ne 0 ]; then
        print_error "Failed to create deployment package"
        exit 1
    fi

    print_info "Deployment package created: build/function.zip"
    print_info "Package size: $(du -h function.zip | cut -f1)"

    # Update Lambda function via Terraform
    print_info "Deploying Lambda function via Terraform..."
    cd ../terraform

    terraform apply -auto-approve

    if [ $? -ne 0 ]; then
        print_error "Terraform apply failed"
        exit 1
    fi

    print_info "Deployment completed successfully!"
    print_info ""
    print_info "Next steps:"
    print_info "1. Check Lambda function status in AWS Console"
    print_info "2. Monitor CloudWatch Logs for function execution"
    print_info "3. Verify health check endpoint: http://\$(terraform output -raw alb_dns_name)/api/q/health"
    print_info "4. Test API endpoint: http://\$(terraform output -raw alb_dns_name)/api/units"
}

# Run main function
main "$@"
