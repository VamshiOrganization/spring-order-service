POST :http://localhost:8083/api/orders
```
Request:
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
Response:
{
    "orderId": "ad28384f-a21a-42cb-abb2-7365fdafd84f",
    "status": "PLACED",
    "totalAmount": 132998.0,
    "currency": "INR",
    "estimatedDelivery": "3-5 business days",
    "message": "Order placed successfully! You will receive a confirmation email and SMS.",
    "createdAt": "2026-07-23T15:21:52.865073"
}
```