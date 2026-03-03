# Marine Management System

A comprehensive yacht management platform with advanced financial tracking, reporting, and document management capabilities.

## 🚀 Features

### 💰 Financial Management
- **Multi-Currency Support**: EUR, USD, TRY with automatic exchange rate tracking
- **Advanced Expense Tracking**: Category-based organization with drill-down analysis
- **Income & Expense Management**: Complete financial entry lifecycle
- **Attachment Management**: Upload receipts, invoices, and supporting documents
- **Smart Search**: Filter by date range, category, amount, and free-text search

### 📊 Reports & Analytics
- **Tree Report**: Hierarchical drill-down analysis (MainCategory → Category → Who)
- **Pivot Report**: Year-over-year comparison with monthly breakdown
- **Custom Date Ranges**: Analyze any period (preset or custom)
- **Visual Analytics**: Interactive charts and trend analysis

### 🔐 Authentication & Authorization
- JWT-based authentication with secure token management
- Role-based access control (ADMIN, MANAGER, CAPTAIN, USER)
- BCrypt password encryption
- Session management with token refresh

### 👥 User Management
- User registration and profile management
- Role assignment and permissions
- Password security policies
- Username/email availability validation

## 🏗️ Architecture

Built following **Domain-Driven Design (DDD)** principles with clean architecture patterns.

### System Context
![Context Diagram](docs/architecture/c4-context.puml)

Shows the system's relationship with users and external systems.

**Users:**
- **Administrator**: System configuration and user management
- **Ship Captain**: Operational data entry and reporting

**External Systems:**
- **External GIS**: Mapping and location services

---

### Container Architecture
![Container Diagram](docs/architecture/c4-container.puml)

High-level technology stack and component interaction.

**Technology Choices:**
- **Frontend**: Angular 20 (Standalone components, Signals-based state)
- **Backend**: Spring Boot 3.5.7 (Java 21, DDD architecture)
- **Database**: PostgreSQL 16 (JPA/Hibernate)
- **Storage**: Local filesystem / S3 for attachments

---

### Backend Components
![Component Diagram](docs/architecture/c4-component.puml)

Internal structure of the Backend API following layered architecture.

**Key Layers:**
- **Controllers** (Spring MVC): REST endpoints
- **Services** (Business Logic): Domain services and report generators
- **Repositories** (Data Access): Spring Data JPA repositories

---

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.7
- **Language**: Java 21
- **Security**: Spring Security + JWT
- **Database**: PostgreSQL 16
- **ORM**: JPA/Hibernate
- **Build Tool**: Maven 3.9+

### Frontend
- **Framework**: Angular 20
- **State Management**: Angular Signals
- **UI Components**: Standalone components
- **HTTP Client**: Angular HttpClient
- **Styling**: Tailwind CSS

### Architecture Patterns
- Domain-Driven Design (DDD)
- CQRS principles for reporting
- Repository pattern
- Value objects (Money, Period)
- Aggregate roots

---

## 📋 Prerequisites

- **Java**: 21+
- **Node.js**: 18+ (for Angular frontend)
- **Maven**: 3.9+
- **PostgreSQL**: 16+
- **IDE**: IntelliJ IDEA (recommended), VS Code, or Eclipse

---

## 🔧 Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd marine-management-system
```

### 2. Database Configuration

Create PostgreSQL database:
```sql
CREATE DATABASE marine_management;
CREATE USER marine_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE marine_management TO marine_user;
```

Configure `application.properties`:
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/marine_management
spring.datasource.username=marine_user
spring.datasource.password=your_password

# JWT Configuration
jwt.secret=your-secret-key-min-256-bits-change-in-production
jwt.expiration=86400000

# File Upload
app.upload.dir=uploads
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Logging
logging.level.com.marine.management=DEBUG
```

### 3. Backend Setup
```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

Backend runs on `http://localhost:8080`

### 4. Frontend Setup
```bash
cd frontend

# Install dependencies
npm install

# Development server
npm start
```

Frontend runs on `http://localhost:4200`

### 5. Default Admin User

First-run auto-creates admin:
- **Username**: `admin`
- **Password**: `admin123`
- **Email**: `admin@marine.com`
- **Role**: ADMIN

⚠️ **CRITICAL**: Change password immediately after first login!

---

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

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

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "uuid",
    "username": "admin",
    "email": "admin@marine.com",
    "role": "ADMIN"
  }
}
```

---

### Financial Entries

#### Create Expense
```http
POST /api/finance/entries/expense
Authorization: Bearer {token}
Content-Type: application/json

{
  "mainCategoryId": 1,
  "categoryId": "uuid",
  "whoId": 5,
  "amount": "500.00",
  "currency": "EUR",
  "entryDate": "2025-01-15",
  "location": "Fethiye Marina",
  "description": "Diesel refill"
}
```

#### Upload Attachment
```http
POST /api/finance/entries/{entryId}/attachments
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [receipt.pdf]
```

---

### Reports

#### Tree Report (Date Range)
```http
POST /api/finance/reports/expense-tree
Authorization: Bearer {token}
Content-Type: application/json

{
  "startDate": "2025-01-01",
  "endDate": "2025-01-31",
  "currency": "EUR"
}
```

**Response:**
```json
{
  "totalAmount": 7331.02,
  "currency": "EUR",
  "nodes": [
    {
      "id": "2",
      "level": 1,
      "type": "MAIN_CATEGORY",
      "name": "Bakım ve Onarım",
      "amount": 4538.98,
      "percentage": 61.91,
      "isTechnical": true,
      "children": [...]
    }
  ]
}
```

#### Pivot Report (Year)
```http
POST /api/finance/reports/expense-tree-pivot
Authorization: Bearer {token}
Content-Type: application/json

{
  "year": 2025,
  "currency": "EUR"
}
```

**Response:**
```json
{
  "year": 2025,
  "columns": ["2025-01", "2025-02", ..., "TOTAL"],
  "columnTotals": { "2025-01": 500.00, "TOTAL": 3876.00 },
  "rows": [...]
}
```

---

## 🔐 Security & Permissions

### Role Matrix

| Feature | ADMIN | MANAGER | CAPTAIN | USER |
|---------|-------|---------|---------|------|
| User Management |  |  | ❌ | ❌ |
| Category Management |  |  | ❌ | ❌ |
| Financial Entries |  |  |  | 👁️ View Only |
| Reports |  |  |  | ❌ |
| Attachments |  |  |  | ❌ |

### Endpoint Security
```java
/api/auth/**           → Public
/api/users/**          → ADMIN, MANAGER
/api/finance/entries/** → ADMIN, MANAGER, CAPTAIN
/api/finance/reports/** → ADMIN, MANAGER, CAPTAIN
/api/finance/categories/** → ADMIN, MANAGER (write), All (read)
```

---

## 📁 Project Structure
```
marine-management-system/
├── backend/
│   └── src/main/java/com/marine/management/
│       ├── modules/
│       │   ├── auth/              # JWT, login, user context
│       │   ├── finance/           # Financial entries, reports
│       │   │   ├── domain/        # Entities, value objects
│       │   │   ├── application/   # DTOs, services
│       │   │   └── infrastructure/ # Controllers, repositories
│       │   ├── reference/         # Categories, master data
│       │   └── users/             # User management
│       └── shared/
│           ├── config/            # Security, CORS, JWT
│           ├── exceptions/        # Global exception handling
│           └── kernel/            # Shared domain concepts
│
├── frontend/
│   └── src/app/
│       ├── features/
│       │   ├── auth/              # Login, auth guards
│       │   ├── finance/           # Expense management
│       │   └── reports/           # Tree & Pivot reports
│       ├── shared/
│       │   ├── models/            # TypeScript interfaces
│       │   ├── services/          # HTTP services
│       │   └── components/        # Reusable UI
│       └── core/                  # App-wide singletons
│
└── docs/
    └── architecture/              # C4 diagrams
```

---

## 🧪 Testing

### Backend Tests
```bash
# Run all tests
mvn test

# With coverage
mvn clean test jacoco:report

# Integration tests only
mvn test -Dtest=*IT
```

### Frontend Tests
```bash
# Unit tests
npm test

# E2E tests
npm run e2e

# Coverage
npm run test:coverage
```

---

## 🚀 Deployment

### Production Checklist

- [ ] Change default admin password
- [ ] Use strong JWT secret (min 256 bits)
- [ ] Configure CORS for production domain
- [ ] Set up cloud file storage (AWS S3 / Azure Blob)
- [ ] Enable HTTPS/SSL
- [ ] Configure production database with SSL
- [ ] Set up automated database backups
- [ ] Configure logging (ELK stack / CloudWatch)
- [ ] Set up monitoring (Prometheus / Grafana)
- [ ] Configure rate limiting
- [ ] Set up CI/CD pipeline

### Environment Variables
```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/marine_db
SPRING_DATASOURCE_USERNAME=marine_prod_user
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}

# Security
JWT_SECRET=${JWT_SECRET_KEY}
JWT_EXPIRATION=86400000

# File Storage
APP_UPLOAD_DIR=/var/marine/uploads
# Or cloud storage
CLOUD_STORAGE_BUCKET=marine-attachments
CLOUD_STORAGE_REGION=eu-west-1

# CORS
CORS_ALLOWED_ORIGINS=https://marine.yourdomain.com
```

---

## 🎯 Domain Model

### Core Entities

**FinancialEntry**
```java
- id: UUID
- entryNumber: EntryNumber (FE-YYYY-NNN)
- entryType: RecordType (EXPENSE/INCOME)
- mainCategoryId: Long
- category: FinancialCategory (embedded)
- whoId: Long
- originalAmount: Money
- baseAmount: Money (EUR)
- exchangeRate: BigDecimal
- entryDate: LocalDate
- location: String
- description: String
- attachments: List<Attachment>
```

**Money** (Value Object)
```java
- amount: BigDecimal
- currency: String
+ validate(), isPositive(), isZero()
```

**Period** (Value Object)
```java
- startDate: LocalDate
- endDate: LocalDate
+ ofMonth(), ofYear(), parse()
```

---

## 📊 Key Features Explained

### 1. Tree Report (Drill-Down Analysis)

Hierarchical expense breakdown:
```
MainCategory (Level 1)
└─ Category (Level 2)
   └─ Who (Level 3)
```

**Use Cases:**
- "Show me all Technical expenses for January"
- "Which crew member had the most expenses?"
- "Drill down into Maintenance → Engine → Fuel costs"

### 2. Pivot Report (Year-over-Year)

Matrix view with months as columns:
```
Category    | Jan | Feb | Mar | ... | TOTAL
Technical   | 10k | 8k  | 12k | ... | 150k
Operational | 5k  | 6k  | 7k  | ... | 80k
```

**Use Cases:**
- "Compare monthly spending across categories"
- "Identify seasonal expense patterns"
- "Year-end financial summary"

### 3. Attachment System

**Features:**
- Multi-file upload with drag-drop
- Preview (PDF, images)
- Download individual or bulk
- Metadata tracking (filename, size, upload date)

**Storage:**
- Development: Local filesystem
- Production: AWS S3 / Azure Blob (recommended)

---

## 🔄 Version History

### v1.0.0 (Current)
-  User authentication & JWT
-  Financial entry management
-  Tree & Pivot reports
-  Attachment system
-  Multi-currency support
-  Role-based access control

### Upcoming (v1.1.0)
- [ ] Budget planning & forecasting
- [ ] PDF export for reports
- [ ] Advanced filters (saved searches)
- [ ] Email notifications
- [ ] Audit log

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Follow code style guidelines
4. Write tests for new features
5. Commit with clear messages (`git commit -m 'Add: Amazing feature'`)
6. Push to branch (`git push origin feature/AmazingFeature`)
7. Open Pull Request

### Code Style
- **Backend**: Google Java Style Guide
- **Frontend**: Angular Style Guide
- **Commits**: Conventional Commits



## Development Guidelines

### Entity Changes
Before modifying any entity, follow this checklist:

1. Create Flyway migration FIRST
2. Test migration on clean DB
3. Update entity class SECOND
4. Validate with Hibernate (ddl-auto=validate)
5. Commit BOTH files together

**Never commit entity without migration!**

---

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file.

---

## 📧 Support

- **Issues**: [GitHub Issues](https://github.com/your-repo/issues)
- **Email**: support@marine-management.com
- **Documentation**: [Wiki](https://github.com/your-repo/wiki)

---

## 🎓 Learning Resources

This project demonstrates:
- **DDD** patterns in Spring Boot
- **Angular Signals** for state management
- **JWT** authentication flow
- **Report generation** with CQRS
- **File upload** handling
- **Multi-currency** business logic

Perfect for learning modern full-stack development!

---

**Marine Management System** — Modern financial management for marine professionals.

© 2025 Marine Management System. All rights reserved.