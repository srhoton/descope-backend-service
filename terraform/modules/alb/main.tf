# ALB Module - Use Existing Application Load Balancer

# Data source to look up the existing ALB
data "aws_lb" "existing" {
  arn = var.existing_alb_arn
}

# Data source to get the HTTP listener (if it exists)
data "aws_lb_listener" "http" {
  count = var.enable_http_listener ? 1 : 0

  load_balancer_arn = data.aws_lb.existing.arn
  port              = 80
}

# Data source to get the HTTPS listener (if it exists)
data "aws_lb_listener" "https" {
  count = var.enable_https_listener ? 1 : 0

  load_balancer_arn = data.aws_lb.existing.arn
  port              = 443
}

# Target Group for Lambda Function
resource "aws_lb_target_group" "app" {
  name        = "${var.name_prefix}-tg"
  target_type = "lambda"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
    path                = "/api/q/health"
    matcher             = "200"
  }

  tags = merge(
    var.tags,
    {
      Name = "${var.name_prefix}-tg"
    }
  )

  lifecycle {
    create_before_destroy = true
  }
}

# Note: Lambda permission and attachment are created by the Lambda module
# to avoid circular dependency issues. The Lambda module passes the target
# group ARN and handles the attachment.

# Listener Rule for HTTP - forward matching requests to our target group (if HTTP listener exists)
resource "aws_lb_listener_rule" "http" {
  count = var.enable_http_listener ? 1 : 0

  listener_arn = data.aws_lb_listener.http[0].arn
  priority     = var.listener_priority

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }

  condition {
    path_pattern {
      values = var.path_pattern
    }
  }

  tags = var.tags
}

# Listener Rule for HTTPS - forward matching requests to our target group (if HTTPS listener exists)
resource "aws_lb_listener_rule" "https" {
  count = var.enable_https_listener ? 1 : 0

  listener_arn = data.aws_lb_listener.https[0].arn
  priority     = var.listener_priority

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }

  condition {
    path_pattern {
      values = var.path_pattern
    }
  }

  tags = var.tags
}

# CloudWatch Alarms for Target Group
resource "aws_cloudwatch_metric_alarm" "target_response_time" {
  alarm_name          = "${var.name_prefix}-tg-high-response-time"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "TargetResponseTime"
  namespace           = "AWS/ApplicationELB"
  period              = 300
  statistic           = "Average"
  threshold           = 1.0
  alarm_description   = "This metric monitors target response time"
  treat_missing_data  = "notBreaching"

  dimensions = {
    LoadBalancer = data.aws_lb.existing.arn_suffix
    TargetGroup  = aws_lb_target_group.app.arn_suffix
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "unhealthy_target_count" {
  alarm_name          = "${var.name_prefix}-tg-unhealthy-targets"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "UnHealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = 60
  statistic           = "Average"
  threshold           = 0
  alarm_description   = "This metric monitors unhealthy target count"
  treat_missing_data  = "notBreaching"

  dimensions = {
    LoadBalancer = data.aws_lb.existing.arn_suffix
    TargetGroup  = aws_lb_target_group.app.arn_suffix
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "target_5xx_count" {
  alarm_name          = "${var.name_prefix}-tg-high-5xx-count"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "HTTPCode_Target_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = 300
  statistic           = "Sum"
  threshold           = 10
  alarm_description   = "This metric monitors 5XX errors from targets"
  treat_missing_data  = "notBreaching"

  dimensions = {
    LoadBalancer = data.aws_lb.existing.arn_suffix
    TargetGroup  = aws_lb_target_group.app.arn_suffix
  }

  tags = var.tags
}
