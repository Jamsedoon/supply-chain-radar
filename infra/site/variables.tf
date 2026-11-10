variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

# Where /api/* requests go: the backend's load balancer.
# Set to "" on teardown day to remove the /api route (snapshot mode).
variable "api_origin_domain" {
  description = "Load balancer address for /api/*, or empty for snapshot mode"
  type        = string
  default     = "radar-alb-1589662153.us-east-1.elb.amazonaws.com"
}