# The API: behind the load balancer. Started FIRST, so it sets up the database.
resource "aws_ecs_service" "api" {
  name            = "radar-api"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app["api"].arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = true # needed to pull the image and reach SQS (no NAT Gateway)
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = "app"
    container_port   = 8080
  }

  # Spring Boot on 0.25 vCPU needs time to start. Don't judge health before 3 minutes.
  health_check_grace_period_seconds = 180

  # If a new version keeps failing, stop and go back to the last working one.
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  # Terraform waits here until the API is running and healthy.
  wait_for_steady_state = true

  depends_on = [aws_lb_listener.http]
}

# The collector and scorer: no load balancer. Started AFTER the API is healthy.
resource "aws_ecs_service" "worker" {
  for_each = toset(["collector", "scorer"])

  name            = "radar-${each.key}"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app[each.key].arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = true
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  depends_on = [aws_ecs_service.api]
}
