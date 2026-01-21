# VPC Module Variables - For Existing VPC

variable "existing_vpc_id" {
  description = "ID of the existing VPC to use"
  type        = string
  default     = "vpc-03163f35ccd0fc6a9"

  validation {
    condition     = can(regex("^vpc-[a-z0-9]+$", var.existing_vpc_id))
    error_message = "VPC ID must be in the format vpc-xxxxxxxxx."
  }
}

variable "name_prefix" {
  description = "Prefix for resource names (used for reference only with existing VPC)"
  type        = string
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
