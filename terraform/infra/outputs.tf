output "instance_public_ip" {
  value = aws_instance.weather_ec2.public_ip
}