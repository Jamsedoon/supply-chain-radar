# Guard 1: the load balancer. Open to the internet on port 80 only.
resource "aws_security_group" "alb" {
  name        = "radar-alb"
  description = "Load balancer: HTTP from the internet"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP from anywhere"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "radar-alb" }
}

# Guard 2: the app containers. Port 8080, only from the load balancer.
resource "aws_security_group" "app" {
  name        = "radar-app"
  description = "App containers: 8080 from the load balancer only"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "App port from the load balancer"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # Outbound open: the app must reach npm, SQS, ECR, and the database.
  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "radar-app" }
}

# Guard 3: the database. Port 5432, only from the app.
resource "aws_security_group" "db" {
  name        = "radar-db"
  description = "Database: Postgres from the app only"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Postgres from the app"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  tags = { Name = "radar-db" }
}