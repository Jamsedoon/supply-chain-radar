# 1. A strong random password. Nobody types or sees it.
resource "random_password" "db" {
  length  = 32
  special = false # letters and numbers only (RDS rejects some symbols)
}

# 2. Keep the password encrypted in SSM Parameter Store (free).
#    The app's containers will read it from here.
resource "aws_ssm_parameter" "db_password" {
  name  = "/supply-chain-radar/db-password"
  type  = "SecureString"
  value = random_password.db.result
}

# 3. Which subnets the database may use (RDS needs at least 2 zones).
resource "aws_db_subnet_group" "main" {
  name       = "radar-db-subnets"
  subnet_ids = aws_subnet.public[*].id

  tags = { Name = "radar-db-subnets" }
}

# 4. The Postgres database.
resource "aws_db_instance" "main" {
  identifier     = "radar-db"
  engine         = "postgres"
  engine_version = "16"
  instance_class = "db.t4g.micro"

  allocated_storage = 20
  storage_type      = "gp3"

  db_name  = "radar"
  username = "radar"
  password = random_password.db.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.db.id]
  publicly_accessible    = false
  multi_az               = false

  # Demo settings: cheap, and easy to destroy.
  backup_retention_period = 0
  skip_final_snapshot     = true
  deletion_protection     = false
  apply_immediately       = true

  tags = { Name = "radar-db" }
}