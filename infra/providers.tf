provider "aws" {
  region = var.aws_region

  assume_role {
    role_arn     = "arn:aws:iam::026338613200:role/otel-lab-terraform-role"
    session_name = "otel-lab-terraform"
  }

  default_tags {
    tags = {
      Project     = "otel-lab"
      Environment = "lab"
      ManagedBy   = "terraform"
    }
  }
}
