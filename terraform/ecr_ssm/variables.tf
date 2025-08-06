variable "aws_region" {
  type = string
}

variable "ecr_repo_name" {
  type = string
}

variable "parameters" {
  type = map(string)
}
