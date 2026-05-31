# spring-order-service

A production-ready Spring Boot REST API for placing and managing customer orders. When an order is placed, it is saved to the database and an event is published to **AWS SQS**, which triggers an **AWS Lambda** function that sends an **Email (SES)** and **SMS (SNS)** notification to the customer.

---

## Architecture

```
Customer → POST /api/orders
                │
                ▼
        OrderController
                │
                ▼
        OrderService ──────────────► MySQL / PostgreSQL
                │                    (saves order)
                ▼
       SqsPublisherService
                │
                ▼
          AWS SQS Queue
      (order-notification-queue)
                │
                ▼
          AWS Lambda
      (order-notification-lambda)
           │         │
           ▼         ▼
       AWS SES     AWS SNS
       (Email)      (SMS)
```

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.3.0 | Framework |
| Spring Data JPA | 3.3.0 | Database ORM |
| Spring Validation | 3.3.0 | Request validation |
| Spring Cloud AWS SQS | 3.1.1 | Publish events to SQS |
| MySQL | 8.x | Database |
| Lombok | 1.18.32 | Reduce boilerplate |
| Maven | 3.x | Build tool |

---

## Project Structure

```
spring-order-service/
├── src/main/java/com/example/
│   ├── OrderServiceApplication.java        # Main entry point
│   ├── controller/
│   │   ├── OrderController.java            # REST endpoints
│   │   └── GlobalExceptionHandler.java     # Validation error handling
│   ├── service/
│   │   ├── OrderService.java               # Core business logic
│   │   └── SqsPublisherService.java        # Publishes events to SQS
│   ├── repository/
│   │   └── OrderRepository.java            # JPA repository
│   └── model/
│       ├── Order.java                      # JPA entity
│       ├── OrderItem.java                  # JPA entity (line items)
│       └── OrderDtos.java                  # Request & Response DTOs
└── src/main/resources/
    └── application.yml                     # App configuration
```

---

## Prerequisites

- Java 21
- Maven 3.x
- MySQL 8.x (or PostgreSQL)
- AWS Account with:
  - SQS queue named `order-notification-queue`
  - IAM user with `sqs:SendMessage` permission
- AWS credentials configured (see setup below)

---

## Local Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-username/spring-order-service.git
cd spring-order-service
```

### 2. Create the database

```sql
CREATE DATABASE orderdb;
```

### 3. Configure AWS credentials

Create the credentials file at `C:\Users\<your-user>\.aws\credentials` (Windows) or `~/.aws/credentials` (Mac/Linux):

```ini
[default]
aws_access_key_id     = YOUR_ACCESS_KEY_ID
aws_secret_access_key = YOUR_SECRET_ACCESS_KEY
```

Create the config file at `C:\Users\<your-user>\.aws\config` (Windows) or `~/.aws/config` (Mac/Linux):

```ini
[default]
region = ap-south-1
```

> This is the same credentials file used by AWS CLI — no extra setup needed if you've already configured AWS CLI.

### 4. Update `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/orderdb
    username: your_db_username
    password: your_db_password
```

### 5. Run the application

```bash
mvn spring-boot:run
```

App starts at: `http://localhost:8080`

---

## API Endpoints

### Place Order

```
POST /api/orders
Content-Type: application/json
```

**Request Body:**

```json
{
  "customerId": "CUST-001",
  "customerName": "Ravi Kumar",
  "customerEmail": "ravi.kumar@gmail.com",
  "customerPhone": "+919876543210",
  "deliveryAddress": "Flat 4B, Hitech City, Hyderabad - 500081",
  "items": [
    {
      "productId": "PROD-001",
      "productName": "Samsung Galaxy S24 Ultra",
      "quantity": 1,
      "price": 129999.00
    },
    {
      "productId": "PROD-002",
      "productName": "Samsung 45W Charger",
      "quantity": 1,
      "price": 2999.00
    }
  ]
}
```

**Response `201 Created`:**

```json
{
  "orderId": "be79b3d2-f08a-4cad-bb30-ce53ed9fe4b3",
  "status": "PLACED",
  "totalAmount": 132998.0,
  "currency": "INR",
  "estimatedDelivery": "3-5 business days",
  "message": "Order placed successfully! You will receive a confirmation email and SMS.",
  "createdAt": "2026-05-31T22:13:20.510258"
}
```

---

### Get Order by ID

```
GET /api/orders/{orderId}
```

---

### Get All Orders for a Customer

```
GET /api/orders/customer/{customerId}
```

---

## Validation Rules

| Field | Rule |
|---|---|
| `customerId` | Required |
| `customerName` | Required |
| `customerEmail` | Required, valid email format |
| `customerPhone` | Must match `+91XXXXXXXXXX` format |
| `deliveryAddress` | Required |
| `items` | At least one item required |
| `items[].productId` | Required |
| `items[].productName` | Required |
| `items[].quantity` | Minimum 1 |
| `items[].price` | Minimum 0 |

**Validation error response `400 Bad Request`:**

```json
{
  "status": 400,
  "error": "Validation Failed",
  "fields": {
    "customerEmail": "Invalid email format",
    "customerPhone": "Phone must be in format +91XXXXXXXXXX"
  },
  "timestamp": "2026-05-31T22:13:20"
}
```

---

## How the SQS Integration Works

1. Customer calls `POST /api/orders`
2. `OrderService` saves the order to the database
3. `SqsPublisherService` publishes a JSON event to `order-notification-queue`
4. AWS Lambda (`order-notification-lambda`) is triggered automatically
5. Lambda sends an **email via AWS SES** and **SMS via AWS SNS** to the customer
6. If SQS publish fails, the order is **still saved** — notification failure does not roll back the order

---

## AWS SQS Queue Setup

1. Go to **AWS Console → SQS → Create Queue**
2. Name: `order-notification-queue`
3. Type: `Standard`
4. Attach IAM policy to your user:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sqs:SendMessage",
        "sqs:GetQueueUrl",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:ap-south-1:YOUR_ACCOUNT_ID:order-notification-queue"
    }
  ]
}
```

---

## Environment-based Configuration

| Environment | Credentials approach |
|---|---|
| Local (Windows/Mac) | `~/.aws/credentials` file |
| EC2 / ECS (production) | IAM Instance Profile (`instance-profile: true` in yml) |
| IntelliJ Run Config | Set `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` as env vars in Run Configuration |

---

## Related Repository

| Repository | Description |
|---|---|
| [order-notification-lambda](https://github.com/your-username/order-notification-lambda) | AWS Lambda (Java 21) that reads from SQS and sends Email + SMS |

---

## Sample cURL

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "customerName": "Ravi Kumar",
    "customerEmail": "ravi.kumar@gmail.com",
    "customerPhone": "+919876543210",
    "deliveryAddress": "Flat 4B, Hitech City, Hyderabad - 500081",
    "items": [
      { "productId": "PROD-001", "productName": "Samsung Galaxy S24 Ultra", "quantity": 1, "price": 129999.00 },
      { "productId": "PROD-002", "productName": "Samsung 45W Charger", "quantity": 1, "price": 2999.00 }
    ]
  }'
```

---

## License

MIT
