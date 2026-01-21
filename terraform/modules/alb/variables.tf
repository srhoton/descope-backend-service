# ALB Module Variables

variable "name_prefix" {
  description = "Prefix for resource names"
  type        = string
}

variable "existing_alb_arn" {
  description = "ARN of the existing Application Load Balancer"
  type        = string
}

variable "vpc_id" {
  description = "ID of the VPC"
  type        = string
}

variable "certificate_arn" {
  description = "ARN of ACM certificate for HTTPS listener (optional)"
  type        = string
  default     = ""
}

variable "enable_http_listener" {
  description = "Whether the existing ALB has an HTTP listener on port 80"
  type        = bool
  default     = false
}

variable "enable_https_listener" {
  description = "Whether the existing ALB has an HTTPS listener on port 443"
  type        = bool
  default     = true
}


variable "listener_priority" {
  description = "Priority for the listener rule (1-50000)"
  type        = number
  default     = 100

  validation {
    condition     = var.listener_priority >= 1 && var.listener_priority <= 50000
    error_message = "Listener priority must be between 1 and 50000."
  }
}

variable "path_pattern" {
  description = "Path pattern for routing to the Lambda function"
  type        = list(string)
  default     = ["/api/*"]
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
