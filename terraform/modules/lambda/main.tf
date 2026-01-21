# Lambda Module - AWS Lambda Function

# CloudWatch Log Group for Lambda
resource "aws_cloudwatch_log_group" "lambda" {
  name              = "/aws/lambda/${var.name_prefix}-function"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name = "${var.name_prefix}-lambda-log-group"
    }
  )
}

# Lambda Function
resource "aws_lambda_function" "app" {
  function_name = "${var.name_prefix}-function"
  role          = var.lambda_execution_role_arn

  # Package configuration - ZIP deployment
  package_type = "Zip"
  filename     = var.lambda_zip_file
  source_code_hash = filebase64sha256(var.lambda_zip_file)

  # Runtime configuration
  runtime = "java17"
  handler = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"

  # Resource configuration
  memory_size = var.memory_size
  timeout     = var.timeout

  # Network configuration - place Lambda in private subnets
  vpc_config {
    subnet_ids         = var.private_subnet_ids
    security_group_ids = [var.lambda_security_group_id]
  }

  # Environment variables
  environment {
    variables = {
      DYNAMODB_TABLE_UNITS = var.dynamodb_table_name
      QUARKUS_LOG_LEVEL    = "INFO"
    }
  }

  # Tracing configuration
  tracing_config {
    mode = "Active"
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.name_prefix}-lambda-function"
    }
  )

  depends_on = [aws_cloudwatch_log_group.lambda]
}

# Lambda Function URL (Alternative to API Gateway for simpler setup)
resource "aws_lambda_function_url" "app" {
  count = var.enable_function_url ? 1 : 0

  function_name      = aws_lambda_function.app.function_name
  authorization_type = "NONE"

  cors {
    allow_origins  = ["*"]
    allow_methods  = ["*"]
    allow_headers  = ["*"]
    expose_headers = ["*"]
    max_age        = 3600
  }
}

# Lambda Alias for versioning
resource "aws_lambda_alias" "live" {
  name             = "live"
  function_name    = aws_lambda_function.app.function_name
  function_version = "$LATEST"

  lifecycle {
    ignore_changes = [function_version]
  }
}

# Lambda Permission for ALB to invoke the function
resource "aws_lambda_permission" "alb" {
  count = var.enable_alb_integration ? 1 : 0

  statement_id  = "AllowExecutionFromALB"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.app.function_name
  principal     = "elasticloadbalancing.amazonaws.com"
  source_arn    = var.alb_target_group_arn
}

# Attach Lambda to Target Group
resource "aws_lb_target_group_attachment" "lambda" {
  count = var.enable_alb_integration ? 1 : 0

  target_group_arn = var.alb_target_group_arn
  target_id        = aws_lambda_function.app.arn
  depends_on       = [aws_lambda_permission.alb]
}

# CloudWatch Alarms
resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = "${var.name_prefix}-lambda-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = 60
  statistic           = "Sum"
  threshold           = 5
  alarm_description   = "This metric monitors Lambda function errors"
  treat_missing_data  = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.app.function_name
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "lambda_throttles" {
  alarm_name          = "${var.name_prefix}-lambda-throttles"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Throttles"
  namespace           = "AWS/Lambda"
  period              = 60
  statistic           = "Sum"
  threshold           = 5
  alarm_description   = "This metric monitors Lambda function throttles"
  treat_missing_data  = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.app.function_name
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "lambda_duration" {
  alarm_name          = "${var.name_prefix}-lambda-duration"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "Duration"
  namespace           = "AWS/Lambda"
  period              = 60
  statistic           = "Average"
  threshold           = var.timeout * 1000 * 0.8 # 80% of timeout in milliseconds
  alarm_description   = "This metric monitors Lambda function duration"
  treat_missing_data  = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.app.function_name
  }

  tags = var.tags
}

# Lambda Auto Scaling (Provisioned Concurrency - Optional)
resource "aws_lambda_provisioned_concurrency_config" "app" {
  count = var.provisioned_concurrent_executions > 0 ? 1 : 0

  function_name                     = aws_lambda_function.app.function_name
  provisioned_concurrent_executions = var.provisioned_concurrent_executions
  qualifier                         = aws_lambda_alias.live.name
}

# Application Auto Scaling Target for Lambda
resource "aws_appautoscaling_target" "lambda" {
  count = var.provisioned_concurrent_executions > 0 ? 1 : 0

  max_capacity       = var.max_provisioned_concurrent_executions
  min_capacity       = var.provisioned_concurrent_executions
  resource_id        = "function:${aws_lambda_function.app.function_name}:provisioned-concurrency:${aws_lambda_alias.live.name}"
  scalable_dimension = "lambda:function:ProvisionedConcurrentExecutions"
  service_namespace  = "lambda"
}

# Application Auto Scaling Policy for Lambda
resource "aws_appautoscaling_policy" "lambda" {
  count = var.provisioned_concurrent_executions > 0 ? 1 : 0

  name               = "${var.name_prefix}-lambda-scaling-policy"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.lambda[0].resource_id
  scalable_dimension = aws_appautoscaling_target.lambda[0].scalable_dimension
  service_namespace  = aws_appautoscaling_target.lambda[0].service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "LambdaProvisionedConcurrencyUtilization"
    }
    target_value = 0.7
  }
}
