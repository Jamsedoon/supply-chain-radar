# Private storage for the app's Docker image.
resource "aws_ecr_repository" "app" {
  name                 = "supply-chain-radar"
  image_tag_mutability = "MUTABLE"

  # Lets "terraform destroy" delete the repository even if images are inside.
  force_delete = true

  # Free basic check for known security problems in each new image.
  image_scanning_configuration {
    scan_on_push = true
  }

  tags = { Name = "supply-chain-radar" }
}

# Keep only the 5 newest images, so storage stays inside the free 500 MB.
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep only the last 5 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 5
      }
      action = { type = "expire" }
    }]
  })
}