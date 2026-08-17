resource "aws_ecs_cluster" "main" {
  name = "otel-lab-cluster"
}

resource "aws_service_discovery_private_dns_namespace" "main" {
  name        = "otel-lab.local"
  description = "Private namespace for the ephemeral OTel lab"
  vpc         = aws_vpc.main.id
}

resource "aws_service_discovery_service" "service_b" {
  name = "service-b"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

resource "aws_service_discovery_service" "jaeger" {
  name = "jaeger"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

resource "aws_ecs_task_definition" "service_a" {
  family                   = "otel-lab-task-a"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.service_a_cpu
  memory                   = var.service_a_memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.service_a_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name         = "service-a"
      image        = "${aws_ecr_repository.service_a.repository_url}:${var.image_tag}"
      essential    = true
      portMappings = [{ containerPort = 8080, protocol = "tcp" }]
      environment = [
        { name = "OTEL_ENABLED", value = tostring(var.otel_enabled) },
        { name = "OTEL_EXPORTER_OTLP_ENDPOINT", value = "http://127.0.0.1:4317" },
        { name = "SERVICE_B_URL", value = "http://service-b.otel-lab.local:8080" },
        { name = "DEPLOYMENT_ENVIRONMENT", value = "lab" },
        { name = "CLOUD_PROVIDER", value = "aws" },
        { name = "AWS_REGION", value = var.aws_region }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = local.log_groups.service_a
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    },
    {
      name      = "otel-collector-a"
      image     = "otel/opentelemetry-collector-contrib:0.116.1"
      essential = true
      command   = ["--config=env:OTEL_CONFIG"]
      environment = [
        { name = "AWS_REGION", value = var.aws_region },
        { name = "OTEL_LOG_GROUP", value = local.log_groups.collector_exported_logs },
        { name = "OTEL_LOG_STREAM", value = "service-a-otlp-logs" },
        { name = "OTEL_CONFIG", value = file("${path.module}/../otel/collector-aws.yaml") }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = local.log_groups.collector_a
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_task_definition" "service_b" {
  family                   = "otel-lab-task-b"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.service_b_cpu
  memory                   = var.service_b_memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.service_b_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name         = "service-b"
      image        = "${aws_ecr_repository.service_b.repository_url}:${var.image_tag}"
      essential    = true
      portMappings = [{ containerPort = 8080, protocol = "tcp" }]
      environment = [
        { name = "OTEL_ENABLED", value = tostring(var.otel_enabled) },
        { name = "OTEL_EXPORTER_OTLP_ENDPOINT", value = "http://127.0.0.1:4317" },
        { name = "PRODUCTS_TABLE_NAME", value = aws_dynamodb_table.products.name },
        { name = "DEPLOYMENT_ENVIRONMENT", value = "lab" },
        { name = "CLOUD_PROVIDER", value = "aws" },
        { name = "AWS_REGION", value = var.aws_region }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = local.log_groups.service_b
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    },
    {
      name      = "otel-collector-b"
      image     = "otel/opentelemetry-collector-contrib:0.116.1"
      essential = true
      command   = ["--config=env:OTEL_CONFIG"]
      environment = [
        { name = "AWS_REGION", value = var.aws_region },
        { name = "OTEL_LOG_GROUP", value = local.log_groups.collector_exported_logs },
        { name = "OTEL_LOG_STREAM", value = "service-b-otlp-logs" },
        { name = "OTEL_CONFIG", value = file("${path.module}/../otel/collector-aws.yaml") }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = local.log_groups.collector_b
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "service_b" {
  name            = "otel-lab-service-b"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.service_b.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.public.id]
    security_groups  = [aws_security_group.service_b.id]
    assign_public_ip = true
  }

  service_registries {
    registry_arn = aws_service_discovery_service.service_b.arn
  }

}

resource "aws_ecs_service" "service_a" {
  name            = "otel-lab-service-a"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.service_a.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.public.id]
    security_groups  = [aws_security_group.service_a.id]
    assign_public_ip = true
  }

  depends_on = [
    aws_ecs_service.service_b,
    aws_ecs_service.jaeger
  ]
}

resource "aws_ecs_task_definition" "jaeger" {
  family                   = "otel-lab-task-jaeger"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.jaeger_cpu
  memory                   = var.jaeger_memory
  execution_role_arn       = aws_iam_role.task_execution.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name      = "jaeger"
      image     = "jaegertracing/all-in-one:1.63.0"
      essential = true
      portMappings = [
        { containerPort = 16686, protocol = "tcp" },
        { containerPort = 4317, protocol = "tcp" }
      ]
      environment = [
        { name = "COLLECTOR_OTLP_ENABLED", value = "true" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = local.log_groups.jaeger
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "jaeger" {
  name            = "otel-lab-jaeger"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.jaeger.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.public.id]
    security_groups  = [aws_security_group.jaeger.id]
    assign_public_ip = true
  }

  service_registries {
    registry_arn = aws_service_discovery_service.jaeger.arn
  }
}

resource "aws_ecs_task_definition" "grafana" {
  family                   = "otel-lab-task-grafana"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.grafana_cpu
  memory                   = var.grafana_memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.grafana_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name         = "grafana"
      image        = "${aws_ecr_repository.grafana.repository_url}:${var.image_tag}"
      essential    = true
      portMappings = [{ containerPort = 3000, protocol = "tcp" }]
      environment = [
        { name = "AWS_REGION", value = var.aws_region },
        { name = "GF_SECURITY_ADMIN_USER", value = "admin" },
        { name = "GF_SECURITY_ADMIN_PASSWORD", value = var.grafana_admin_password },
        { name = "GF_AUTH_ANONYMOUS_ENABLED", value = "true" },
        { name = "GF_AUTH_ANONYMOUS_ORG_ROLE", value = "Admin" },
        { name = "GF_USERS_DEFAULT_THEME", value = "light" }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = local.log_groups.grafana
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "grafana" {
  name            = "otel-lab-grafana"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.grafana.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.public.id]
    security_groups  = [aws_security_group.grafana.id]
    assign_public_ip = true
  }
}
