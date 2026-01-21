# VPC Module - Use Existing Network Infrastructure

# Data source to look up the existing VPC
data "aws_vpc" "existing" {
  id = var.existing_vpc_id
}

# Data source to look up private subnets in the existing VPC
data "aws_subnets" "private" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.existing.id]
  }

  filter {
    name   = "tag:Name"
    values = ["*private*"]
  }
}

# Data source to look up public subnets in the existing VPC
data "aws_subnets" "public" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.existing.id]
  }

  filter {
    name   = "tag:Name"
    values = ["*public*"]
  }
}

# Get details about each private subnet
data "aws_subnet" "private" {
  for_each = toset(data.aws_subnets.private.ids)
  id       = each.value
}

# Get details about each public subnet
data "aws_subnet" "public" {
  for_each = toset(data.aws_subnets.public.ids)
  id       = each.value
}
