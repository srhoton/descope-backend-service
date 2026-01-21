# ALB Module Outputs

output "alb_arn" {
  description = "ARN of the Application Load Balancer"
  value       = data.aws_lb.existing.arn
}

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = data.aws_lb.existing.dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the Application Load Balancer"
  value       = data.aws_lb.existing.zone_id
}

output "target_group_arn" {
  description = "ARN of the target group"
  value       = aws_lb_target_group.app.arn
}

output "target_group_name" {
  description = "Name of the target group"
  value       = aws_lb_target_group.app.name
}

output "http_listener_arn" {
  description = "ARN of the HTTP listener"
  value       = var.enable_http_listener ? data.aws_lb_listener.http[0].arn : null
}

output "https_listener_arn" {
  description = "ARN of the HTTPS listener"
  value       = var.enable_https_listener ? data.aws_lb_listener.https[0].arn : null
}

output "http_listener_rule_arn" {
  description = "ARN of the HTTP listener rule"
  value       = var.enable_http_listener ? aws_lb_listener_rule.http[0].arn : null
}

output "https_listener_rule_arn" {
  description = "ARN of the HTTPS listener rule"
  value       = var.enable_https_listener ? aws_lb_listener_rule.https[0].arn : null
}
