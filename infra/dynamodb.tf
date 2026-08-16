resource "aws_dynamodb_table" "products" {
  name         = "otel-lab-products"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "product_id"

  attribute {
    name = "product_id"
    type = "S"
  }
}

resource "aws_dynamodb_table_item" "product_1" {
  table_name = aws_dynamodb_table.products.name
  hash_key   = aws_dynamodb_table.products.hash_key

  item = jsonencode({
    product_id = { S = "product-1" }
    name       = { S = "Notebook" }
    price      = { N = "49.90" }
  })
}

resource "aws_dynamodb_table_item" "product_2" {
  table_name = aws_dynamodb_table.products.name
  hash_key   = aws_dynamodb_table.products.hash_key

  item = jsonencode({
    product_id = { S = "product-2" }
    name       = { S = "Backpack" }
    price      = { N = "79.90" }
  })
}

resource "aws_dynamodb_table_item" "product_3" {
  table_name = aws_dynamodb_table.products.name
  hash_key   = aws_dynamodb_table.products.hash_key

  item = jsonencode({
    product_id = { S = "product-3" }
    name       = { S = "Headphones" }
    price      = { N = "99.90" }
  })
}
