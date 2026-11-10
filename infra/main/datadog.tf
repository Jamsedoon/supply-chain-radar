variable "datadog_site" {
  description = "Datadog site the data is sent to (US1 = datadoghq.com)"
  type        = string
  default     = "datadoghq.com"
}

# Our AWS account number, looked up (not typed into the code).
data "aws_caller_identity" "current" {}

# The API key was put in SSM by hand. Terraform only points at it,
# so the key itself is never in code, in git, or in Terraform's state.
locals {
  datadog_api_key_arn = "arn:aws:ssm:${var.region}:${data.aws_caller_identity.current.account_id}:parameter/supply-chain-radar/datadog-api-key"
}

# The execution role may read that one parameter, to hand the key to the sidecars.
data "aws_iam_policy_document" "execution_datadog" {
  statement {
    actions   = ["ssm:GetParameters"]
    resources = [local.datadog_api_key_arn]
  }
}

resource "aws_iam_role_policy" "execution_datadog" {
  name   = "read-datadog-api-key"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.execution_datadog.json
}