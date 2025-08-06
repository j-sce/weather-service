resource "aws_ecr_repository" "weather_service" {
  name                 = var.ecr_repo_name
  image_tag_mutability = "MUTABLE"
  force_delete = true
}

resource "aws_ssm_parameter" "weather_params" {
  for_each = var.parameters

  name  = "/dev/weather/${each.key}"
  type  = "SecureString"
  value = each.value
}