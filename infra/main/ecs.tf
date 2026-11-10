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
# (Now used by the two helper containers; the app's own logs go to Datadog.)
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
# Three containers per task: the app, the Datadog agent, and the log router.
resource "aws_ecs_task_definition" "app" {
  for_each = local.roles

  family                   = "radar-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512  # 0.5 vCPU for the whole task
  memory                   = 2048 # 2 GB for the whole task

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "ARM64" # Graviton: matches the Mac-built image, ~20% cheaper
  }

  execution_role_arn = aws_iam_role.execution.arn
  task_role_arn      = aws_iam_role.task.arn

  container_definitions = jsonencode([
    # ---------- 1. The app ----------
    {
      name      = "app"
      image     = "${aws_ecr_repository.app.repository_url}:latest"
      essential = true
      memory    = 1024 # the app's own fixed share; helpers can't take it

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
        { name = "STATSD_ENABLED", value = "true" }, # send metrics to the agent
      ]

      secrets = [
        { name = "DB_PASSWORD", valueFrom = aws_ssm_parameter.db_password.arn },
      ]

      # Logs go through the log router (FireLens) straight to Datadog.
      logConfiguration = {
        logDriver = "awsfirelens"
        options = {
          Name       = "datadog"
          Host       = "http-intake.logs.${var.datadog_site}"
          TLS        = "on"
          compress   = "gzip"
          provider   = "ecs"
          dd_service = "supply-chain-radar"
          dd_source  = "java"
          dd_tags    = "role:${each.key},env:demo"
        }
        secretOptions = [
          { name = "apikey", valueFrom = local.datadog_api_key_arn },
        ]
      }
    },

    # ---------- 2. The Datadog agent ----------
    {
      name      = "datadog-agent"
      image     = "public.ecr.aws/datadog/agent:7"
      essential = false # if it fails, the app keeps running

      memoryReservation = 256

      environment = [
        { name = "ECS_FARGATE", value = "true" },
        { name = "DD_SITE", value = var.datadog_site },
        { name = "DD_TAGS", value = "service:supply-chain-radar role:${each.key} env:demo" },
        { name = "DD_DOGSTATSD_TAGS", value = "service:supply-chain-radar role:${each.key} env:demo" },
        { name = "DD_APM_ENABLED", value = "false" },
      ]

      secrets = [
        { name = "DD_API_KEY", valueFrom = local.datadog_api_key_arn },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.app[each.key].name
          "awslogs-region"        = var.region
          "awslogs-stream-prefix" = "datadog-agent"
        }
      }
    },

    # ---------- 3. The log router (Fluent Bit, via FireLens) ----------
    {
      name      = "log-router"
      image     = "public.ecr.aws/aws-observability/aws-for-fluent-bit:stable"
      essential = true

      memoryReservation = 64

      firelensConfiguration = {
        type = "fluentbit"
        options = {
          "enable-ecs-log-metadata" = "true"
        }
      }

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.app[each.key].name
          "awslogs-region"        = var.region
          "awslogs-stream-prefix" = "log-router"
        }
      }
    },
  ])
}