data "aws_iam_policy_document" "ecs_tasks_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "task_execution" {
  name               = "otel-lab-task-execution-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}

resource "aws_iam_role_policy_attachment" "task_execution_managed" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "service_a_task" {
  name               = "otel-lab-service-a-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}

resource "aws_iam_role" "service_b_task" {
  name               = "otel-lab-service-b-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}

resource "aws_iam_role" "grafana_task" {
  name               = "otel-lab-grafana-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}

data "aws_iam_policy_document" "xray_write" {
  statement {
    actions = [
      "xray:PutTraceSegments",
      "xray:PutTelemetryRecords"
    ]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "xray_write" {
  name   = "otel-lab-xray-write"
  policy = data.aws_iam_policy_document.xray_write.json
}

resource "aws_iam_role_policy_attachment" "service_a_xray" {
  role       = aws_iam_role.service_a_task.name
  policy_arn = aws_iam_policy.xray_write.arn
}

resource "aws_iam_role_policy_attachment" "service_b_xray" {
  role       = aws_iam_role.service_b_task.name
  policy_arn = aws_iam_policy.xray_write.arn
}

data "aws_iam_policy_document" "collector_logs_write" {
  statement {
    actions = [
      "logs:CreateLogStream",
      "logs:DescribeLogStreams",
      "logs:PutLogEvents"
    ]
    resources = [
      "${aws_cloudwatch_log_group.app["collector_exported_logs"].arn}:*",
      "${aws_cloudwatch_log_group.app["collector_metrics"].arn}:*"
    ]
  }
}

resource "aws_iam_policy" "collector_logs_write" {
  name   = "otel-lab-collector-logs-write"
  policy = data.aws_iam_policy_document.collector_logs_write.json
}

resource "aws_iam_role_policy_attachment" "service_a_collector_logs" {
  role       = aws_iam_role.service_a_task.name
  policy_arn = aws_iam_policy.collector_logs_write.arn
}

resource "aws_iam_role_policy_attachment" "service_b_collector_logs" {
  role       = aws_iam_role.service_b_task.name
  policy_arn = aws_iam_policy.collector_logs_write.arn
}

data "aws_iam_policy_document" "service_b_dynamodb" {
  statement {
    actions = [
      "dynamodb:GetItem",
      "dynamodb:Query"
    ]
    resources = [aws_dynamodb_table.products.arn]
  }
}

resource "aws_iam_policy" "service_b_dynamodb" {
  name   = "otel-lab-service-b-dynamodb"
  policy = data.aws_iam_policy_document.service_b_dynamodb.json
}

resource "aws_iam_role_policy_attachment" "service_b_dynamodb" {
  role       = aws_iam_role.service_b_task.name
  policy_arn = aws_iam_policy.service_b_dynamodb.arn
}

data "aws_iam_policy_document" "grafana_observability_read" {
  statement {
    actions = [
      "cloudwatch:DescribeAlarmHistory",
      "cloudwatch:DescribeAlarms",
      "cloudwatch:DescribeAlarmsForMetric",
      "cloudwatch:GetMetricData",
      "cloudwatch:GetMetricStatistics",
      "cloudwatch:ListMetrics",
      "logs:DescribeLogGroups",
      "logs:DescribeLogStreams",
      "logs:FilterLogEvents",
      "logs:GetLogEvents",
      "logs:GetLogGroupFields",
      "logs:GetQueryResults",
      "logs:StartQuery",
      "logs:StopQuery",
      "xray:BatchGetTraces",
      "xray:GetServiceGraph",
      "xray:GetTraceSummaries"
    ]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "grafana_observability_read" {
  name   = "otel-lab-grafana-observability-read"
  policy = data.aws_iam_policy_document.grafana_observability_read.json
}

resource "aws_iam_role_policy_attachment" "grafana_observability_read" {
  role       = aws_iam_role.grafana_task.name
  policy_arn = aws_iam_policy.grafana_observability_read.arn
}
