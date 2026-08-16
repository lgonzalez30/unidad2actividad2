resource "aws_cloudwatch_log_group" "app" {
  for_each = local.log_groups

  name              = each.value
  retention_in_days = 1
}
