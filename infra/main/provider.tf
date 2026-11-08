provider "aws" {
  region = var.region

  # Stuck on every resource Terraform creates, automatically.
  default_tags {
    tags = {
      Project     = "supply-chain-radar"
      ManagedBy   = "terraform"
      Environment = "demo"
    }
  }
}