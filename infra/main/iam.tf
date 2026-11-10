# Who may wear these badges: only ECS tasks.
data "aws_iam_policy_document" "ecs_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

# ---------- Execution role: used by AWS to START the container ----------

resource "aws_iam_role" "execution" {
  name               = "radar-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

# AWS's ready-made policy: pull images from ECR and write logs.
resource "aws_iam_role_policy_attachment" "execution_managed" {
  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Plus: read ONLY our one database password parameter.
data "aws_iam_policy_document" "execution_ssm" {
  statement {
    actions   = ["ssm:GetParameters"]
    resources = [aws_ssm_parameter.db_password.arn]
  }
}

resource "aws_iam_role_policy" "execution_ssm" {
  name   = "read-db-password"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.execution_ssm.json
}

# ---------- Task role: used by YOUR JAVA CODE while it runs ----------

resource "aws_iam_role" "task" {
  name               = "radar-ecs-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

# Only what SqsQueueClient does, and only on our main queue.
data "aws_iam_policy_document" "task_sqs" {
  statement {
    actions = [
      "sqs:SendMessage",
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes",
    ]
    resources = [aws_sqs_queue.packages.arn]
  }
}

resource "aws_iam_role_policy" "task_sqs" {
  name   = "use-packages-queue"
  role   = aws_iam_role.task.id
  policy = data.aws_iam_policy_document.task_sqs.json
}