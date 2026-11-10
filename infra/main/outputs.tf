output "queue_url" {
  description = "Address of the main queue (the app's SQS_QUEUE_URL)"
  value       = aws_sqs_queue.packages.url
}

output "dlq_url" {
  description = "Address of the dead-letter queue"
  value       = aws_sqs_queue.packages_dlq.url
}

output "ecr_repository_url" {
  description = "Where to push the app image"
  value       = aws_ecr_repository.app.repository_url
}

output "db_endpoint" {
  description = "Database address (host:port) for the app's DB_URL"
  value       = aws_db_instance.main.endpoint
}

output "alb_dns_name" {
  description = "Public address of the backend"
  value       = aws_lb.main.dns_name
}

output "github_deploy_role_arn" {
  description = "Role the GitHub Actions workflow assumes"
  value       = aws_iam_role.github_deploy.arn
}