<div align="center">

<br/>

```
 █████╗ ██╗   ██╗████████╗ ██████╗     ███████╗██╗     ██╗████████╗███████╗
██╔══██╗██║   ██║╚══██╔══╝██╔═══██╗    ██╔════╝██║     ██║╚══██╔══╝██╔════╝
███████║██║   ██║   ██║   ██║   ██║    █████╗  ██║     ██║   ██║   █████╗  
██╔══██║██║   ██║   ██║   ██║   ██║    ██╔══╝  ██║     ██║   ██║   ██╔══╝  
██║  ██║╚██████╔╝   ██║   ╚██████╔╝    ███████╗███████╗██║   ██║   ███████╗
╚═╝  ╚═╝ ╚═════╝    ╚═╝    ╚═════╝     ╚══════╝╚══════╝╚═╝   ╚═╝   ╚══════╝
```

### **AutoElite Dealership Management System**
*A complete, enterprise-grade car dealership platform — Inventory, Sales, CRM, Employee Management, Commissions, Test Drives, Reviews, 3D Car Configurator and SMS 2FA — all in a single Java desktop application.*

<br/>

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![H2](https://img.shields.io/badge/H2-Fallback_DB-1a7abf?style=for-the-badge)](https://www.h2database.com/)
[![Twilio](https://img.shields.io/badge/Twilio-SMS%202FA-F22F46?style=for-the-badge&logo=twilio&logoColor=white)](https://www.twilio.com/)
[![License](https://img.shields.io/badge/License-Proprietary-red?style=for-the-badge)](LICENSE)

</div>

---

## 🚗 What is AutoElite?

**AutoElite** is a full-stack **Dealership Management System (DMS)** built entirely in Java 17. It is designed to run the entire operation of a car dealership from a single desktop application — replacing spreadsheets and disconnected tools with one cohesive, professional platform.

From managing vehicle inventory with 3D visual configurators to tracking employee commissions, scheduling test drives, processing sales with multiple payment methods and invoicing, and securing every login with Twilio SMS 2FA — AutoElite has it all.

---

## ✨ Feature Deep Dive

### 🏎️ Vehicle Inventory Management (`CarController`)
Every aspect of a car's listing is tracked and managed:

| Field | Description |
|-------|-------------|
| `brand`, `model`, `year` | Make, model, and year of manufacture |
| `color` | Exterior colour |
| `price` | Sticker price |
| `category` | E.g. SUV, Sedan, Coupe, Truck |
| `stock` | Units available in inventory |
| `fuel_type` | Petrol, Diesel, Electric, Hybrid |
| `transmission` | Manual or Automatic |
| `engine_size` | Engine displacement (e.g., 2.0L) |
| `features` | Comma-separated list (e.g., "Sunroof, Leather Seats, ABS") |
| `image_path` | Path to vehicle photo asset |
| `sketchfab_model_id` | 3D model ID for the interactive Car Configurator |
| `is_available` | Live availability flag |

**Operations**: Add, Update, Delete, Get by ID, Filter by price range, filter by availability.

---

### 🛍️ Sales Pipeline (`SaleController`)
End-to-end sale transaction management with full relational joins:

- **Create Sale**: Links `car_id → customer_id → employee_id`, records `sale_date`, `sale_price`, `payment_method`, and `status`
- **Payment Methods**: Supports multiple payment types per sale record
- **Sale Status Lifecycle**: `Pending → Completed / Cancelled` — updating to Completed auto-triggers commission recording
- **Revenue Analytics**: `getTotalRevenue()` and `getTotalSales()` for real-time dashboard KPIs
- **Relational Display**: Sales table joins Cars, Customers, and Employees tables to show human-readable names rather than raw IDs

---

### 👥 Customer CRM (`CustomerController`)
Complete customer lifecycle management:

- **Profile**: Full name, contact details, linked purchase history
- **Add / Update / Delete** customer records
- **Customer Lookup**: Search by name or ID for instant retrieval during sales
- **Purchase History**: Each customer is linked to their historical sale records for traceability

---

### 👔 Employee Management (`EmployeeController`)
Full HR module for dealership staff:

- **Employee Profiles**: Name, role, employee ID, contact info
- **Manager Hierarchy**: Separate `Manager` model extends the base `Person` OOP hierarchy
- **Role-Based Assignments**: Employees are assigned to specific sales transactions, creating clear accountability

---

### 💸 Commission Engine (`CommissionController`)
A sophisticated per-employee commission tracking system:

- **Configurable Rates**: Set individual commission rates per employee (default: **2.5%** of sale value)
- **Upsert Logic**: If a rate already exists, it updates in place; otherwise, it inserts — no duplicates
- **Idempotent Recording**: Calling `recordCommission()` twice on the same sale has no double-effect — the system checks for existing entries first
- **Paid/Unpaid Tracking**: Commission entries carry a `paid` boolean flag so finance teams can track outstanding payouts
- **Fallback Resolution**: If an employee ID is stored as either a string (`EMP001`) or integer, the system auto-resolves it against the `employees` table

---

### 🚘 Test Drive Scheduling (`TestDriveController`, `TestDrive` model)
- Book test drive appointments linking a car, a customer, and a date/time
- Track test drive status (Scheduled, Completed, Cancelled)
- Full history retrieval and management

---

### ⭐ Customer Reviews (`ReviewController`, `Review` model)
- Customers can submit star-rated reviews linked to specific vehicles
- `ReviewController` fetches, adds, and manages reviews per car
- Provides a feedback loop for inventory quality assessment

---

### 🏢 Dealership Profile (`DealershipController`, `Dealership` model)
- Stores the dealership's own identity: name, address, contact info, license details
- Used to populate invoice headers and system branding

---

### 🖥️ Modern UI — `CarShowroomApp.java` (645KB)
The main application is a comprehensive **Java Swing** desktop UI with a modern dark sidebar navigation:

- **`ModernSidebar.java`**: A custom-painted, icon-based collapsible sidebar for navigating between all modules
- **Dashboard Panel**: Live KPI cards showing Total Revenue, Cars Sold, Active Inventory count, Pending Test Drives
- **Inventory Panel**: Searchable/filterable table of all vehicles with inline add/edit/delete actions
- **Sales Panel**: Full sales management table with ability to process new sales and update statuses
- **Customers Panel**: CRM table with search, add, edit, and view-history actions
- **Employees Panel**: Staff directory with role labels and commission rate editing
- **Commissions Panel**: Full commission ledger with paid/unpaid flags and manager controls to mark as paid
- **Test Drives Panel**: Calendar-style scheduling view with status management
- **Reviews Panel**: Star-rating display per car, customer attribution
- **Reports Panel**: Exporting sales summaries and financial breakdowns

---

### 🔮 3D Car Configurator (`CarConfiguratorEngine.java`)
A standout feature unique to AutoElite:

- Embeds **Sketchfab 3D models** inside the Java desktop app via an embedded browser panel
- Customers can spin, zoom, and inspect vehicles in full 3D before purchasing
- Each Car record stores a `sketchfab_model_id` which the configurator uses to load the correct 3D model
- Also includes a companion `web-configurator/` frontend directory for browser-based configurator access

---

### 🔐 Security & Authentication

#### Two-Factor Authentication (`TwilioService.java`, `OtpService.java`)
- Staff login is protected by **username + password + SMS OTP**
- `TwilioService` dispatches a time-limited OTP to the employee's registered phone number via the Twilio REST API
- `OtpService` generates and validates the 6-digit codes with expiry enforcement

#### Session Management (`SessionManager.java`)
- Tracks the currently authenticated user throughout the application session
- Stores role context (Manager vs. Employee) to enforce permission-level UI restrictions

#### Audit Logging (`AuditLogger.java`)
- Every sensitive action (login, sale creation, employee modification) is written to an audit log
- Provides a tamper-evident trail of who did what and when

---

### 🗄️ Database Layer (`DatabaseManager.java` — 30KB)
- **Singleton pattern** — one connection instance shared across the entire application
- **MySQL primary** with full schema: `cars`, `customers`, `employees`, `sales`, `test_drives`, `reviews`, `commissions`, `commission_rates`, `dealerships`, `users`, `audit_logs`
- **H2 in-memory fallback** — if MySQL is not configured, the app transparently boots using H2 with auto-schema creation and demo seeding
- **`DataSeeder.java`** — automatically populates the database with realistic demo data on first launch so the app is immediately usable

---

## 🏗️ Architecture

```
AutoElite/
├── src/
│   ├── controllers/
│   │   ├── CarController.java          # Inventory CRUD + price filtering
│   │   ├── SaleController.java         # Sales pipeline + revenue analytics
│   │   ├── CustomerController.java     # CRM operations
│   │   ├── EmployeeController.java     # Staff management
│   │   ├── CommissionController.java   # Commission rates + payouts
│   │   ├── TestDriveController.java    # Appointment scheduling
│   │   ├── ReviewController.java       # Customer review system
│   │   └── DealershipController.java  # Dealership profile management
│   │
│   ├── models/
│   │   ├── Person.java                 # Base class (inheritance root)
│   │   ├── Employee.java               # Extends Person
│   │   ├── Manager.java                # Extends Employee
│   │   ├── Customer.java               # Extends Person
│   │   ├── Car.java                    # Full vehicle model (15+ fields)
│   │   ├── Sale.java                   # Transaction model
│   │   ├── TestDrive.java              # Appointment model
│   │   ├── Review.java                 # Star-rated feedback model
│   │   └── Dealership.java             # Business identity model
│   │
│   ├── ui/
│   │   ├── CarShowroomApp.java         # Main application entry + all panels
│   │   ├── ModernSidebar.java          # Custom-painted navigation sidebar
│   │   └── CarConfiguratorEngine.java  # Sketchfab 3D model viewer
│   │
│   └── utils/
│       ├── DatabaseManager.java        # Singleton DB (MySQL / H2 fallback)
│       ├── DataSeeder.java             # Demo data population on first run
│       ├── TwilioService.java          # SMS 2FA OTP dispatch
│       ├── OtpService.java             # OTP generation + validation
│       ├── SessionManager.java         # User session + role context
│       ├── AuditLogger.java            # Tamper-evident action logging
│       └── ConfigManager.java          # application.properties reader
│
├── web-configurator/                   # Browser-based 3D car configurator
├── application.properties.example     # Config template — copy and fill in
├── pom.xml                             # Maven build + dependencies
└── run.sh                              # One-command build & launch
```

**Design Pattern**: Clean **MVC (Model-View-Controller)** with an OOP inheritance hierarchy for people (`Person → Employee → Manager`, `Person → Customer`).

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.0 *(optional — H2 fallback works out of the box)*

### 1. Clone the repo
```bash
git clone https://github.com/PranavAndhale/CarShowroom-2.git
cd CarShowroom-2
```

### 2. Configure the application
```bash
cp application.properties.example application.properties
# Edit application.properties with your MySQL credentials
```

> If you skip this step entirely, the app runs on the **H2 in-memory database** automatically and seeds itself with demo data. No configuration required.

### 3. Build & Run
```bash
./run.sh
```
Or manually:
```bash
mvn clean package -q
java -jar target/car-showroom-pro-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## ⚙️ Configuration Reference (`application.properties`)

| Key | Description | Default |
|-----|-------------|---------|
| `db.url` | MySQL JDBC connection URL | `jdbc:mysql://localhost:3306/car_showroom` |
| `db.user` | MySQL username | `root` |
| `db.password` | MySQL password | *(required)* |
| `app.name` | Application display name | `AutoElite Management System` |
| `app.debug` | Enable verbose debug logging | `true` |

---

## 🛠️ Tech Stack

| Technology | Version | Role |
|------------|---------|------|
| **Java** | 17 | Core application language |
| **Java Swing** | Built-in | Desktop GUI framework |
| **Maven** | 3.9+ | Build system + dependency management |
| **MySQL Connector/J** | 8.0.33 | Primary production database driver |
| **H2 Database** | 2.2.224 | Zero-config in-memory fallback DB |
| **Twilio SDK** | 9.14.0 | SMS OTP dispatch for 2FA |
| **JavaMail** | 1.6.2 | Email invoice delivery via SMTP |
| **Sketchfab Embed** | Web API | 3D model viewer in Car Configurator |

---

## 📄 License

This project is protected under a **Proprietary License** — see [LICENSE](LICENSE) for full terms.

---

<div align="center">

Built with ☕ by [Pranav Andhale](https://github.com/PranavAndhale)

</div>
