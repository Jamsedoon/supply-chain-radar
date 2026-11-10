# The three jobs, all from one image. Used by several resources below.
locals {
  roles = toset(["collector", "scorer", "api"])
}

# A named group for our services. With Fargate there are no machines to manage.
resource "aws_ecs_cluster" "main" {
  name = "radar"

  # Container Insights = extra detailed metrics, but it costs money. Off.
  setting {
    name  = "containerInsights"
    value = "disabled"
  }
}

# One log folder per job, kept for 7 days.
resource "aws_cloudwatch_log_group" "app" {
  for_each = local.roles

  name              = "/ecs/radar/${each.key}"
  retention_in_days = 7
}
# The address the app uses to reach the database.
# sslmode=require: the connection to RDS is always encrypted.
locals {
  db_url = "jdbc:postgresql://${aws_db_instance.main.endpoint}/radar?sslmode=require"
}

# One recipe per job. Same image; only APP_ROLE changes.
resource "aws_ecs_task_definition" "app" {
  for_each = local.roles

  family                   = "radar-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256  # 0.25 vCPU
  memory                   = 1024 # 1 GB

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "ARM64" # Graviton: matches the Mac-built image, ~20% cheaper
  }

  execution_role_arn = aws_iam_role.execution.arn
  task_role_arn      = aws_iam_role.task.arn

  container_definitions = jsonencode([{
    name      = "app"
    image     = "${aws_ecr_repository.app.repository_url}:latest"
    essential = true

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    environment = [
      { name = "APP_ROLE", value = each.key },
      { name = "QUEUE_TYPE", value = "sqs" },
      { name = "SQS_QUEUE_URL", value = aws_sqs_queue.packages.url },
      { name = "AWS_REGION", value = var.region },
      { name = "DB_URL", value = local.db_url },
      { name = "DB_USER", value = "radar" },
      { name = "JAVA_TOOL_OPTIONS", value = "-XX:MaxRAMPercentage=75" },
    ]

    # Read from SSM by the EXECUTION role, handed to the app as DB_PASSWORD.
    secrets = [
      { name = "DB_PASSWORD", valueFrom = aws_ssm_parameter.db_password.arn },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.app[each.key].name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "app"
      }
    }
  }])
}