# VortexShop - Full Stack E-Commerce Platform

A modern full-stack e-commerce platform built with Spring Boot, React, MySQL, JWT Authentication, and Flyway. The application provides a complete online shopping experience with product management, order processing, user authentication, and an admin dashboard.

## Features

### Customer Features

* User Registration & Login
* JWT Authentication & Authorization
* Product Catalog
* Product Search, Filtering & Sorting
* Product Details Page
* Shopping Cart
* Checkout Flow
* Order History
* Profile Management
* Address Management
* Product Reviews

### Admin Features

* Dashboard Analytics
* Product Management
* Category Management
* Order Management
* User Management
* Coupon Management
* Inventory Tracking
* Order Status Updates

### Technical Features

* RESTful API Architecture
* Role-Based Access Control
* Flyway Database Migrations
* Responsive Design
* Image Upload Support
* Form Validation
* Toast Notifications

---

## Technology Stack

### Backend

* Java 17
* Spring Boot 3
* Spring Security
* JWT Authentication
* Spring Data JPA
* Hibernate
* MySQL 8
* Flyway
* Maven

### Frontend

* React 18
* Vite
* Tailwind CSS
* React Router
* Axios
* Lucide React
* Recharts

---

## Project Structure

```text
VortexShop
│
├── backend
│   ├── src
│   ├── pom.xml
│
├── frontend
│   ├── src
│   ├── public
│   ├── package.json
│
├── .gitignore
└── README.md
```

---

## Setup Instructions

### Clone Repository

```bash
git clone <repository-url>
cd VortexShop
```

### Create Database

```sql
CREATE DATABASE vortex_ecom;
```

### Configure Environment Variables

Set the following environment variables in your IDE or operating system:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD

JWT_SECRET

MAIL_USERNAME
MAIL_PASSWORD
```

### Run Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend runs on:

```text
http://localhost:8080
```

### Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on:

```text
http://localhost:5173
```

---

## Database Migration

Flyway automatically manages:

* Database schema creation
* Relationships and constraints
* Seed categories
* Seed products
* Initial application data

No manual SQL import is required.

---

## Security

* JWT Authentication
* BCrypt Password Encryption
* Role-Based Authorization
* Protected API Endpoints
* Protected Admin Routes

---

## Future Enhancements

* Payment Gateway Integration
* Product Recommendations
* Order Tracking
* Returns & Refunds
* Multi-Vendor Support
* Email Notifications

---

## Author

**Rohan Agarwal**

Java Backend Developer | Spring Boot | React | MySQL

---

## License

This project is intended for educational and portfolio purposes.
