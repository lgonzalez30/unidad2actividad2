data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "main" {
  cidr_block           = "10.42.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${local.name_prefix}-vpc"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${local.name_prefix}-igw"
  }
}

resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.42.1.0/24"
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = {
    Name = "${local.name_prefix}-public-subnet"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${local.name_prefix}-public-rt"
  }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

resource "aws_security_group" "service_a" {
  name        = "${local.name_prefix}-service-a-sg"
  description = "Allow public lab traffic to service-a only"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "Public access for k6 and manual tests"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ingress_cidr]
  }

  egress {
    description = "Outbound to service-b, AWS APIs, and image pulls"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "service_b" {
  name        = "${local.name_prefix}-service-b-sg"
  description = "Allow service-a to call service-b"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "HTTP from service-a"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.service_a.id]
  }

  egress {
    description = "Outbound to DynamoDB, X-Ray, logs, and image pulls"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "grafana" {
  name        = "${local.name_prefix}-grafana-sg"
  description = "Allow public lab traffic to Grafana"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "Grafana web UI"
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ingress_cidr]
  }

  egress {
    description = "Outbound to AWS APIs and image pulls"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "jaeger" {
  name        = "${local.name_prefix}-jaeger-sg"
  description = "Allow public Jaeger UI and internal OTLP"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "Jaeger UI"
    from_port   = 16686
    to_port     = 16686
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ingress_cidr]
  }

  ingress {
    description = "OTLP gRPC from service-a collector"
    from_port   = 4317
    to_port     = 4317
    protocol    = "tcp"
    security_groups = [
      aws_security_group.service_a.id,
      aws_security_group.service_b.id
    ]
  }

  egress {
    description = "Outbound to AWS APIs and image pulls"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
