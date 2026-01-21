# DynamoDB Module - Data Store

# DynamoDB Table for Units
resource "aws_dynamodb_table" "units" {
  name             = var.table_name
  billing_mode     = var.billing_mode
  hash_key         = "id"
  stream_enabled   = true
  stream_view_type = "NEW_AND_OLD_IMAGES"

  # Only set these if using PROVISIONED billing mode
  read_capacity  = var.billing_mode == "PROVISIONED" ? var.read_capacity : null
  write_capacity = var.billing_mode == "PROVISIONED" ? var.write_capacity : null

  attribute {
    name = "id"
    type = "S"
  }

  # Point-in-time recovery for data protection
  point_in_time_recovery {
    enabled = true
  }

  # Server-side encryption
  server_side_encryption {
    enabled     = true
    kms_key_arn = aws_kms_key.dynamodb.arn
  }

  # Time to live configuration
  ttl {
    enabled        = false
    attribute_name = ""
  }

  tags = merge(
    var.tags,
    {
      Name = var.table_name
    }
  )

  # Lifecycle policy - temporarily disabled for infrastructure rebuild
  # lifecycle {
  #   prevent_destroy = true
  # }
}

# KMS Key for DynamoDB encryption
resource "aws_kms_key" "dynamodb" {
  description             = "KMS key for DynamoDB table encryption"
  deletion_window_in_days = 10
  enable_key_rotation     = true

  tags = merge(
    var.tags,
    {
      Name = "${var.table_name}-kms-key"
    }
  )
}

# KMS Key Alias
resource "aws_kms_alias" "dynamodb" {
  name          = "alias/${var.table_name}"
  target_key_id = aws_kms_key.dynamodb.key_id
}

# Auto Scaling for Read Capacity (only if PROVISIONED mode)
resource "aws_appautoscaling_target" "read" {
  count = var.billing_mode == "PROVISIONED" ? 1 : 0

  max_capacity       = var.read_capacity * 10
  min_capacity       = var.read_capacity
  resource_id        = "table/${aws_dynamodb_table.units.name}"
  scalable_dimension = "dynamodb:table:ReadCapacityUnits"
  service_namespace  = "dynamodb"
}

resource "aws_appautoscaling_policy" "read" {
  count = var.billing_mode == "PROVISIONED" ? 1 : 0

  name               = "${var.table_name}-read-scaling-policy"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.read[0].resource_id
  scalable_dimension = aws_appautoscaling_target.read[0].scalable_dimension
  service_namespace  = aws_appautoscaling_target.read[0].service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "DynamoDBReadCapacityUtilization"
    }
    target_value = 70.0
  }
}

# Auto Scaling for Write Capacity (only if PROVISIONED mode)
resource "aws_appautoscaling_target" "write" {
  count = var.billing_mode == "PROVISIONED" ? 1 : 0

  max_capacity       = var.write_capacity * 10
  min_capacity       = var.write_capacity
  resource_id        = "table/${aws_dynamodb_table.units.name}"
  scalable_dimension = "dynamodb:table:WriteCapacityUnits"
  service_namespace  = "dynamodb"
}

resource "aws_appautoscaling_policy" "write" {
  count = var.billing_mode == "PROVISIONED" ? 1 : 0

  name               = "${var.table_name}-write-scaling-policy"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.write[0].resource_id
  scalable_dimension = aws_appautoscaling_target.write[0].scalable_dimension
  service_namespace  = aws_appautoscaling_target.write[0].service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "DynamoDBWriteCapacityUtilization"
    }
    target_value = 70.0
  }
}

# CloudWatch Alarms for DynamoDB
resource "aws_cloudwatch_metric_alarm" "read_throttle" {
  alarm_name          = "${var.table_name}-read-throttle"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "ReadThrottleEvents"
  namespace           = "AWS/DynamoDB"
  period              = 300
  statistic           = "Sum"
  threshold           = 10
  alarm_description   = "This metric monitors DynamoDB read throttle events"
  treat_missing_data  = "notBreaching"

  dimensions = {
    TableName = aws_dynamodb_table.units.name
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "write_throttle" {
  alarm_name          = "${var.table_name}-write-throttle"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "WriteThrottleEvents"
  namespace           = "AWS/DynamoDB"
  period              = 300
  statistic           = "Sum"
  threshold           = 10
  alarm_description   = "This metric monitors DynamoDB write throttle events"
  treat_missing_data  = "notBreaching"

  dimensions = {
    TableName = aws_dynamodb_table.units.name
  }

  tags = var.tags
}
