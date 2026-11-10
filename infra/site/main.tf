# ---------- The private bucket holding the built dashboard ----------

resource "aws_s3_bucket" "site" {
  bucket_prefix = "supply-chain-radar-site-" # AWS adds a unique ending
  force_destroy = true                       # allow destroy even with files inside
}

resource "aws_s3_bucket_public_access_block" "site" {
  bucket                  = aws_s3_bucket.site.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CloudFront's "ID card" for reading the private bucket.
resource "aws_cloudfront_origin_access_control" "site" {
  name                              = "radar-site"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# ---------- Ready-made CloudFront settings from AWS, looked up by name ----------

data "aws_cloudfront_cache_policy" "optimized" {
  name = "Managed-CachingOptimized" # keep copies of the dashboard files
}

data "aws_cloudfront_cache_policy" "disabled" {
  name = "Managed-CachingDisabled" # never keep copies of API answers
}

data "aws_cloudfront_origin_request_policy" "all_viewer_except_host" {
  name = "Managed-AllViewerExceptHostHeader" # pass the request through to the API
}

locals {
  s3_origin_id  = "site-bucket"
  api_origin_id = "api-alb"
  has_api       = var.api_origin_domain != ""
}

# ---------- CloudFront: one public https address for page + API ----------

resource "aws_cloudfront_distribution" "site" {
  enabled             = true
  comment             = "Supply-Chain Radar dashboard"
  default_root_object = "index.html"
  price_class         = "PriceClass_100" # North America + Europe edge servers only (cheapest)

  # Origin 1: the dashboard files in S3.
  origin {
    domain_name              = aws_s3_bucket.site.bucket_regional_domain_name
    origin_id                = local.s3_origin_id
    origin_access_control_id = aws_cloudfront_origin_access_control.site.id
  }

  # Origin 2: the API's load balancer. Only exists while the backend is up.
  dynamic "origin" {
    for_each = local.has_api ? [1] : []
    content {
      domain_name = var.api_origin_domain
      origin_id   = local.api_origin_id

      custom_origin_config {
        http_port              = 80
        https_port             = 443
        origin_protocol_policy = "http-only"
        origin_ssl_protocols   = ["TLSv1.2"]
      }
    }
  }

  # Everything not matched below: serve files from S3.
  default_cache_behavior {
    target_origin_id       = local.s3_origin_id
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    cache_policy_id        = data.aws_cloudfront_cache_policy.optimized.id
    compress               = true
  }

  # /api/*: forward to the load balancer, never cached.
  dynamic "ordered_cache_behavior" {
    for_each = local.has_api ? [1] : []
    content {
      path_pattern             = "/api/*"
      target_origin_id         = local.api_origin_id
      viewer_protocol_policy   = "redirect-to-https"
      allowed_methods          = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
      cached_methods           = ["GET", "HEAD"]
      cache_policy_id          = data.aws_cloudfront_cache_policy.disabled.id
      origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer_except_host.id
      compress                 = true
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  # The free *.cloudfront.net https certificate.
  viewer_certificate {
    cloudfront_default_certificate = true
  }
}

# ---------- Only THIS CloudFront distribution may read the bucket ----------

data "aws_iam_policy_document" "site_bucket" {
  statement {
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.site.arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.site.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "site" {
  bucket = aws_s3_bucket.site.id
  policy = data.aws_iam_policy_document.site_bucket.json
}