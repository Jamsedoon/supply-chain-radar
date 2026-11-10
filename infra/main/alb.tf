# The front door: a public web address in both subnets.
resource "aws_lb" "main" {
  name               = "radar-alb"
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id
}

# The list of healthy API containers that may receive requests.
resource "aws_lb_target_group" "api" {
  name        = "radar-api"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip" # Fargate containers are registered by their address
  vpc_id      = aws_vpc.main.id

  # How long to let a stopping container finish its open requests.
  deregistration_delay = 30

  health_check {
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

# Listen on port 80 and send everything to the API containers.
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}