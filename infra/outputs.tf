output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "service_a_url" {
  value       = "Run `make service-a-url` after deploy to resolve the current Fargate public IP."
  description = "No ALB is used for cost control, so the public IP is discovered after the task starts."
}

output "dynamodb_table" {
  value = aws_dynamodb_table.products.name
}

output "cloudwatch_log_groups" {
  value = values(local.log_groups)
}

output "xray_information" {
  value = "Open AWS X-Ray console in the selected region and filter service names service-a and service-b."
}

output "aws_region" {
  value = var.aws_region
}

output "budget_name" {
  value = aws_budgets_budget.lab.name
}

output "service_a_repository_url" {
  value = aws_ecr_repository.service_a.repository_url
}

output "service_b_repository_url" {
  value = aws_ecr_repository.service_b.repository_url
}

output "grafana_repository_url" {
  value = aws_ecr_repository.grafana.repository_url
}

output "grafana_url" {
  value       = "Run `make grafana-url` after deploy to resolve the current Fargate public IP."
  description = "No ALB is used for cost control, so the public IP is discovered after the task starts."
}

output "jaeger_url" {
  value       = "Run `make jaeger-url` after deploy to resolve the current Fargate public IP."
  description = "No ALB is used for cost control, so the public IP is discovered after the task starts."
}
