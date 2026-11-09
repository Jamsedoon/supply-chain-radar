# Which availability zones (separate data centers) exist in this region.
data "aws_availability_zones" "available" {
  state = "available"
}

# Our private network. 10.0.0.0/16 = about 65,000 private addresses to use.
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = { Name = "radar-vpc" }
}

# The gate between our network and the internet.
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = { Name = "radar-igw" }
}

# Two public subnets, one in each of the first two zones.
# count = 2 tells Terraform to make this resource twice.
resource "aws_subnet" "public" {
  count = 2

  vpc_id                  = aws_vpc.main.id
  cidr_block              = cidrsubnet(aws_vpc.main.cidr_block, 8, count.index)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = { Name = "radar-public-${count.index}" }
}

# The signpost: anything going to the internet (0.0.0.0/0) uses the gate.
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = { Name = "radar-public-rt" }
}

# Attach the signpost to both subnets.
resource "aws_route_table_association" "public" {
  count = 2

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}