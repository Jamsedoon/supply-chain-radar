output "site_url" {
  description = "Public link to the dashboard"
  value       = "https://${aws_cloudfront_distribution.site.domain_name}"
}

output "bucket_name" {
  description = "Where to upload the built dashboard"
  value       = aws_s3_bucket.site.bucket
}

output "distribution_id" {
  description = "CloudFront ID, used to refresh cached files after an upload"
  value       = aws_cloudfront_distribution.site.id
}