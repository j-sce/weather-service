variable "aws_region" {
  type = string
}

variable "account_id" {
  type = string
}

variable "image_tag" {
  type = string
}

variable "instance_type" {
  type    = string
  default = "t2.micro"
}

variable "ecr_repo_name" {
  type = string
  default = "weather-service"
}