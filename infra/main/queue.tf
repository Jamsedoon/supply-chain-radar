# The "problem box": messages that failed 5 times end up here.
resource "aws_sqs_queue" "packages_dlq" {
  name                      = "radar-packages-dlq"
  message_retention_seconds = 1209600 # 14 days, the longest SQS allows

  tags = { Name = "radar-packages-dlq" }
}

# The main queue: collector puts packages in, scorer takes them out.
resource "aws_sqs_queue" "packages" {
  name                       = "radar-packages"
  visibility_timeout_seconds = 60
  message_retention_seconds  = 345600 # 4 days

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.packages_dlq.arn
    maxReceiveCount     = 5
  })

  tags = { Name = "radar-packages" }
}