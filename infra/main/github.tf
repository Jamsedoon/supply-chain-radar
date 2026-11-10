# Which GitHub repo may deploy. Only this one.
variable "github_repo" {
  description = "GitHub repo allowed to deploy, as owner/name"
  type        = string
  default     = "Jamsedoon@150087524/supply-chain-radar@1330188212"
}

# 1. AWS trusts login tokens from GitHub Actions.
resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
}

# 2. The robot's role. Only runs on the main branch of our repo may wear it.
data "aws_iam_policy_document" "github_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repo}:ref:refs/heads/main"]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  name               = "radar-github-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_assume.json
}

# 3. What the robot may do: push to OUR image storage, restart OUR services.
data "aws_iam_policy_document" "github_deploy" {
  statement {
    sid       = "EcrLogin"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"] # this one action can't be limited to a repository
  }

  statement {
    sid = "EcrPush"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
      "ecr:PutImage",
      "ecr:BatchGetImage",
    ]
    resources = [aws_ecr_repository.app.arn]
  }

  statement {
    sid     = "EcsDeploy"
    actions = ["ecs:UpdateService", "ecs:DescribeServices"]
    resources = concat(
      [aws_ecs_service.api.id],
      [for s in aws_ecs_service.worker : s.id],
    )
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  name   = "deploy-app"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.github_deploy.json
}