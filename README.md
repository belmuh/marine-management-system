# Marine Management System

A comprehensive Spring Boot-based financial management system designed for marine operations, featuring robust authentication, financial tracking, and role-based access control.

## 🚀 Features

### Authentication & Authorization
- JWT-based authentication
- Role-based access control (ADMIN, MANAGER, CAPTAIN, USER)
- Secure password encoding with BCrypt
- Token-based session management

### Financial Management
- **Financial Categories**: Create and manage expense/income categories
- **Financial Entries**: Track income and expenses with detailed information
- **Multi-Currency Support**: Handle transactions in different currencies with exchange rate tracking
- **File Attachments**: Attach receipts and documents to financial entries
- **Advanced Search**: Filter entries by date, category, type, and text search
- **Dashboard Analytics**:
    - Period totals (income/expense)
    - Category-wise breakdowns
    - Monthly trends
    - Summary statistics

### User Management
- User registration and profile management
- Role assignment and management
- Password management
- Username and email availability checks

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Language**: Java 21
- **Security**: Spring Security + JWT
- **Database**: PostgreSQL (JPA/Hibernate)
- **Build Tool**: Maven
- **Architecture**: Domain-Driven Design (DDD) principles

## 📋 Prerequisites

- Java 21
- Maven 3.6+
- PostgreSQL 12+
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## 🔧 Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd marine-management-system
```

### 2. Database Configuration

Create a PostgreSQL database:
```sql
CREATE DATABASE marine_management;
```

Update `application.properties` or `application.yml`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/marine_management
spring.datasource.username=your_username
spring.datasource.password=your_password

# JWT Configuration
jwt.secret=your-secret-key-min-256-bits
jwt.expiration=86400000

# File Upload
app.upload.dir=uploads
```

### 3. Build and Run
```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Default Admin User

A default admin user is automatically created on first startup:
- **Username**: `admin`
- **Password**: `admin123`
- **Email**: `admin@marine.com`
- **Role**: ADMIN

⚠️ **Important**: Change this password immediately in production!

## 📚 API Documentation

### Authentication

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "uuid",
    "username": "admin",
    "email": "admin@marine.com",
    "role": "ADMIN",
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

#### Get Current User
```http
GET /api/auth/me
Authorization: Bearer {token}
```

### Financial Categories

#### Create Category (Admin Only)
```http
POST /api/finance/categories
Authorization: Bearer {token}
Content-Type: application/json

{
  "code": "FUEL",
  "name": "Fuel Expenses",
  "description": "Fuel and energy costs",
  "displayOrder": 1
}
```

#### List Categories
```http
GET /api/finance/categories?activeOnly=true
Authorization: Bearer {token}
```

#### Search Categories
```http
GET /api/finance/categories/search?keyword=fuel
Authorization: Bearer {token}
```

### Financial Entries

#### Create Income Entry
```http
POST /api/finance/entries/income
Authorization: Bearer {token}
Content-Type: application/json

{
  "categoryId": "uuid",
  "amount": "1500.00",
  "currency": "EUR",
  "entryDate": "2024-01-15",
  "description": "Charter payment"
}
```

#### Create Expense Entry
```http
POST /api/finance/entries/expense
Authorization: Bearer {token}
Content-Type: application/json

{
  "categoryId": "uuid",
  "amount": "500.00",
  "currency": "EUR",
  "entryDate": "2024-01-15",
  "description": "Fuel refill"
}
```

#### Get Dashboard Summary
```http
GET /api/finance/entries/dashboard/summary?startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {token}
```

#### Search Entries
```http
GET /api/finance/entries/search?categoryId=uuid&entryType=EXPENSE&startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {token}
```

#### Add Attachment to Entry
```http
POST /api/finance/entries/{entryId}/attachments
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [receipt.pdf]
```

### User Management (Admin/Manager Only)

#### Create User
```http
POST /api/users
Authorization: Bearer {token}
Content-Type: application/json

{
  "username": "captain_john",
  "email": "john@marine.com",
  "password": "SecurePass123!",
  "role": "CAPTAIN"
}
```

#### Update User Profile
```http
PUT /api/users/{userId}/profile
Authorization: Bearer {token}
Content-Type: application/json

{
  "username": "new_username",
  "email": "new@email.com"
}
```

## 🔐 Security & Permissions

### Role Hierarchy

| Role | Permissions |
|------|------------|
| **ADMIN** | Full system access, user management, all financial operations |
| **MANAGER** | Manage operations, financial entries, view reports |
| **CAPTAIN** | Vessel operations, crew management, view reports |
| **USER** | Basic access, own financial entries |

### Endpoint Protection

- `/api/auth/**` - Public
- `/api/users/**` - ADMIN, MANAGER
- `/api/finance/**` - ADMIN, MANAGER, CAPTAIN, USER
- `/api/reports/**` - ADMIN, MANAGER, CAPTAIN

## 📁 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com.marine.management/
│   │       ├── modules/
│   │       │   ├── auth/          # Authentication & JWT
│   │       │   ├── finance/       # Financial management
│   │       │   └── users/         # User management
│   │       └── shared/
│   │           ├── config/        # Security & global config
│   │           ├── exceptions/    # Custom exceptions
│   │           └── kernel/        # Shared domain concepts
│   └── resources/
│       └── application.properties
└── test/
```

## 🎯 Domain Model

### Key Entities

- **User**: System users with roles and credentials
- **FinancialCategory**: Categories for organizing financial entries
- **FinancialEntry**: Income/expense transactions
- **Money**: Value object for handling amounts with currency
- **EntryNumber**: Unique identifier for entries (FE-YYYY-NNN format)

### Value Objects

- **Money**: Encapsulates amount and currency with validation
- **EntryNumber**: Auto-generated unique entry identifier

## 🧪 Testing

Run tests:
```bash
mvn test
```

Run with coverage:
```bash
mvn clean test jacoco:report
```

## 🚀 Deployment

### Production Checklist

1. ✅ Change default admin password
2. ✅ Use strong JWT secret (min 256 bits)
3. ✅ Configure CORS for your frontend domain
4. ✅ Set up proper file storage (cloud storage recommended)
5. ✅ Enable HTTPS
6. ✅ Configure production database
7. ✅ Set up database backups
8. ✅ Configure logging and monitoring

### Environment Variables

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/marine_db
SPRING_DATASOURCE_USERNAME=db_user
SPRING_DATASOURCE_PASSWORD=db_password
JWT_SECRET=your-production-secret-key
JWT_EXPIRATION=86400000
APP_UPLOAD_DIR=/var/marine/uploads
```

## 📝 API Response Format

### Success Response
```json
{
  "id": "uuid",
  "entryNumber": "FE-2024-001",
  "entryType": "EXPENSE",
  "categoryName": "Fuel",
  "originalAmount": {
    "amount": "500.00",
    "currency": "EUR"
  },
  "entryDate": "2024-01-15",
  "createdBy": "admin",
  "createdAt": "2024-01-15T10:30:00"
}
```

### Error Response
```json
{
  "message": "Validation failed",
  "detail": "Username cannot be blank",
  "errorCode": "VALIDATION_ERROR",
  "referenceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00"
}
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📧 Contact

For questions or support, please contact the development team.

## 🔄 Version History

- **v1.0.0** - Initial release
    - User authentication and authorization
    - Financial category management
    - Financial entry tracking
    - File attachment support
    - Dashboard analytics

## 🛣️ Roadmap

- [ ] Multi-vessel support
- [ ] Advanced reporting with PDF export
- [ ] Budget planning and forecasting
- [ ] Mobile application
- [ ] Integration with accounting systems
- [ ] Real-time notifications
- [ ] Crew management module
- [ ] Maintenance tracking

---
