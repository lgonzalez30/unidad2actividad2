variable "aws_region" {
  description = "AWS region used for the ephemeral lab."
  type        = string
  default     = "us-east-1"
}

variable "budget_notification_email" {
  description = "Email used for AWS Budget alerts"
  type        = string
  sensitive   = true
}

variable "allowed_ingress_cidr" {
  description = "CIDR allowed to call service-a. Restrict this to your public IP when possible."
  type        = string
  default     = "0.0.0.0/0"
}

variable "image_tag" {
  description = "Container image tag deployed to ECS."
  type        = string
  default     = "latest"
}

variable "otel_enabled" {
  description = "Enable OpenTelemetry in the application containers."
  type        = bool
  default     = true
}

variable "service_a_cpu" {
  description = "Fargate task CPU units for Task A."
  type        = number
  default     = 512
}

variable "service_a_memory" {
  description = "Fargate task memory MiB for Task A."
  type        = number
  default     = 1024
}

variable "service_b_cpu" {
  description = "Fargate task CPU units for Task B."
  type        = number
  default     = 512
}

variable "service_b_memory" {
  description = "Fargate task memory MiB for Task B."
  type        = number
  default     = 1024
}

variable "grafana_cpu" {
  description = "Fargate task CPU units for Grafana."
  type        = number
  default     = 256
}

variable "grafana_memory" {
  description = "Fargate task memory MiB for Grafana."
  type        = number
  default     = 512
}

variable "grafana_admin_password" {
  description = "Initial Grafana admin password for the ephemeral lab."
  type        = string
  sensitive   = true
  default     = "admin"
}

variable "jaeger_cpu" {
  description = "Fargate task CPU units for Jaeger."
  type        = number
  default     = 256
}

variable "jaeger_memory" {
  description = "Fargate task memory MiB for Jaeger."
  type        = number
  default     = 512
}

locals {
  name_prefix = "otel-lab"

  log_groups = {
    service_a               = "/otel-lab/service-a"
    service_b               = "/otel-lab/service-b"
    grafana                 = "/otel-lab/grafana"
    jaeger                  = "/otel-lab/jaeger"
    collector_a             = "/otel-lab/collector-a"
    collector_b             = "/otel-lab/collector-b"
    collector_exported_logs = "/otel-lab/otel-collector-exported"
    collector_metrics       = "/otel-lab/otel-collector-metrics"
  }
}
