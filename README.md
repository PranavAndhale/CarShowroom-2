<div align="center">

```
 █████╗  ██╗   ██╗ ████████╗  ██████╗  ███████╗ ██╗      ██╗ ████████╗ ███████╗
██╔══██╗ ██║   ██║ ╚══██╔══╝ ██╔═══██╗ ██╔════╝ ██║      ██║ ╚══██╔══╝ ██╔════╝
███████║ ██║   ██║    ██║    ██║   ██║ █████╗   ██║      ██║    ██║    █████╗  
██╔══██║ ██║   ██║    ██║    ██║   ██║ ██╔══╝   ██║      ██║    ██║    ██╔══╝  
██║  ██║ ╚██████╔╝    ██║    ╚██████╔╝ ███████╗ ███████╗ ██║    ██║    ███████╗
╚═╝  ╚═╝  ╚═════╝     ╚═╝    ╚═════╝  ╚══════╝ ╚══════╝ ╚═╝    ╚═╝    ╚══════╝
```

### **AutoElite Dealership Management System**
*A complete enterprise dealership platform — built to handle every aspect of a modern car showroom operation from a single, beautifully designed Java desktop application.*


[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![H2](https://img.shields.io/badge/H2-Zero--Config_Fallback-1a7abf?style=for-the-badge)](https://www.h2database.com/)
[![Twilio](https://img.shields.io/badge/Twilio-SMS%202FA-F22F46?style=for-the-badge&logo=twilio&logoColor=white)](https://www.twilio.com/)
[![License](https://img.shields.io/badge/License-Proprietary-red?style=for-the-badge)](LICENSE)

</div>

---

## 🚗 What is AutoElite?

**AutoElite** is a professional, full-stack **Dealership Management System (DMS)** built entirely in Java 17. It replaces every spreadsheet and disconnected tool a car dealership uses with one unified, enterprise-grade desktop platform.

From processing a fully tracked sale and auto-generating a professional invoice, to scheduling a test drive, managing staff commissions, and predicting customer churn with a built-in AI engine — every dealership workflow is handled in one place.

Designed with a clean **MVC architecture** and a custom, modern **dark-themed Swing UI**, AutoElite feels nothing like a typical Java desktop app.

---

## ✨ Feature Highlights

### 🏎️ Vehicle Inventory — Every Detail Tracked
Each vehicle listing goes far beyond just make and model:

- Full spec sheet: **brand, model, year, colour, category, fuel type, transmission, engine displacement**
- **Live availability** flag with real-time stock counts
- **Rich feature tagging** — sunroof, leather seats, ABS, and more, stored as searchable attributes
- High-resolution **vehicle image** display inside the inventory panel
- Filter by price range, availability, or category with instant results

---

### 💰 Complete Sales Pipeline
End-to-end sale management with full cross-entity linking:

- Every sale links a **vehicle → customer → employee** for complete traceability
- **Multiple payment method** support recorded per transaction
- **Status lifecycle management** — from initiation through to completion
- Real-time **revenue and units-sold KPIs** on the main dashboard

---

### 💸 Smart Commission Engine
A real business-grade commission tracking system for dealership staff:

- **Per-employee configurable commission rates** — every salesperson can have a different rate
- **Automatic commission calculation** triggered on each completed sale
- **Paid / Unpaid ledger** — managers can see outstanding payouts at a glance and mark them settled
- Full commission history per employee

---

### 🤖 Proprietary AI Engine
Four purpose-built intelligence modules power the decision layer:

| Module | Description |
|---|---|
| **AI Churn Predictor** | Scores every customer's churn risk using purchase history & behavioural signals |
| **AI Sales Coach** | Analyses completed vs. cancelled deals and surfaces actionable coaching insights |
| **Smart Car Recommender** | Matches customer profiles to the optimal vehicle from live inventory |
| **Dynamic Pricing Engine** | Models demand elasticity alongside inventory depth to recommend optimal sell prices |

> ℹ️ The algorithms powering these modules are proprietary and are not exposed in this repository.

---

### 📅 Test Drive Scheduling
- Book, view, and manage **test drive appointments** linking customers to specific vehicles
- Track status through **Scheduled → Completed → Cancelled** lifecycle
- Full calendar-style appointment history

---

### ⭐ Customer Review System
- Post-purchase **star-rated reviews** tied to specific vehicles and customers
- Helps the dealership gauge satisfaction and identify top-performing inventory

---

### 👥 Customer CRM
- Complete customer profiles with contact details and full **purchase history**
- Instant lookup by name or ID during the sales process
- All historical transactions linked per customer for repeat-buyer context

---

### 👔 Employee & HR Management
- Full staff directory with roles and contact info
- **Manager hierarchy** built into the data model — managers have elevated control over commissions and reports
- Employees are tied to their specific sales, creating clear individual accountability

---

### 🏢 Dealership Identity
- Stores the dealership's own profile — name, address, license details — used to auto-populate **invoice headers** and application branding

---

### 🔐 Security — Enterprise-Grade, Not an Afterthought

#### Two-Factor Authentication
Every staff login is protected by **two layers**: password authentication plus a **live SMS OTP** sent to the employee's registered phone number via Twilio. The session only opens once both factors are verified.

#### Role-Based Access
Managers and employees see different UI capabilities. Sensitive operations (commission adjustments, employee management) are restricted to authorised roles only.

#### Tamper-Evident Audit Log
Every sensitive action taken inside the system — logins, sales, modifications — is written to an **immutable audit trail**. Nothing happens without a trace.

---

### 🗄️ Intelligent Database Layer
- **MySQL** as the production database — normalized, relational, and battle-tested
- **Zero-configuration H2 fallback** — if MySQL isn't set up, the app transparently boots using an in-memory database and populates itself with realistic demo data. No setup required
- **Auto-seeding** — the system seeds itself with demo inventory, customers, and employees on first launch so the app is immediately usable

---

### 🔮 3D Car Viewer *(Companion Feature)*
A browser-based Sketchfab-powered 3D car viewer is bundled as a companion utility. It accepts car metadata (brand, model, year, fuel type) as URL parameters and launches in the user's default browser directly from the Inventory panel.

> This is an optional companion tool — not part of the core dealership workflows.

---

## 🏗️ Project Structure

```
CarShowroom-2/
├── src/
│   ├── controllers/
│   │   ├── CarController.java          # Inventory CRUD & stock management
│   │   ├── CustomerController.java     # Customer profile & history
│   │   ├── EmployeeController.java     # Staff records & role management
│   │   ├── SaleController.java         # Sales pipeline & invoice generation
│   │   ├── CommissionController.java   # Commission calc & payout tracking
│   │   ├── TestDriveController.java    # Appointment scheduling
│   │   ├── ReviewController.java       # Customer review management
│   │   └── DealershipController.java   # Dealership identity & branding
│   ├── models/
│   │   ├── Person.java                 # Base class (name, contact)
│   │   ├── Employee.java               # Extends Person — role, salary
│   │   ├── Manager.java                # Extends Employee — elevated access
│   │   ├── Customer.java               # Extends Person — purchase history
│   │   ├── Car.java                    # Full vehicle spec model
│   │   ├── Sale.java                   # Transaction record
│   │   ├── TestDrive.java              # Appointment model
│   │   ├── Review.java                 # Star-rated review model
│   │   └── Dealership.java             # Dealership profile model
│   ├── ui/
│   │   ├── CarShowroomApp.java         # Main application window & all panels
│   │   ├── ModernSidebar.java          # Animated neon sidebar component
│   │   └── CarConfiguratorEngine.java  # 3D viewer launcher (companion)
│   ├── utils/
│   │   ├── DatabaseManager.java        # MySQL + H2 fallback, all SQL queries
│   │   ├── DataSeeder.java             # Demo data auto-seeder on first launch
│   │   ├── SessionManager.java         # Login session & role state
│   │   ├── AuditLogger.java            # Immutable tamper-evident audit trail
│   │   ├── BackupScheduler.java        # Automated background DB backup
│   │   ├── TwilioService.java          # SMS OTP delivery via Twilio
│   │   ├── OtpService.java             # OTP generation & verification logic
│   │   └── ConfigManager.java          # Reads application.properties
│   └── resources/
│       └── autoelite_logo.png          # Branded Apex "A" logo asset
├── web-configurator/                   # Browser-based 3D car viewer (companion)
├── application.properties.example      # Config template — copy & fill in
├── pom.xml                             # Maven build & dependency definitions
└── run.sh                              # One-command build & launch script
```

**Design pattern**: Clean **MVC** with OOP inheritance — `Person → Employee → Manager` and `Person → Customer`.

---

## ⚙️ Configuration Reference

Copy `application.properties.example` → `application.properties` and supply your values. This file is `.gitignore`-protected and never committed.

| Key | Description |
|-----|-------------|
| `db.url` | JDBC connection string (e.g. `jdbc:mysql://localhost:3306/car_showroom`) |
| `db.user` | MySQL username |
| `db.password` | MySQL password |
| `twilio.account_sid` | Twilio Account SID — for SMS 2FA |
| `twilio.auth_token` | Twilio Auth Token |
| `twilio.phone_number` | Twilio sender phone number |
| `mail.host` | SMTP host (e.g. `smtp.gmail.com`) |
| `mail.port` | SMTP port (e.g. `587`) |
| `mail.user` | Email address for outbound mail |
| `mail.password` | Email / app password |

> **No MySQL?** Leave `db.*` as-is — the app automatically falls back to H2 in-memory with demo data.

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.0 *(optional — zero-config H2 fallback works automatically)*

### 1. Clone the repo
```bash
git clone https://github.com/PranavAndhale/CarShowroom-2.git
cd CarShowroom-2
```

### 2. Configure *(optional — skip for H2 demo mode)*
```bash
cp application.properties.example application.properties
# Fill in your MySQL credentials
```

### 3. Build & Run
```bash
./run.sh
```

The application window launches automatically. On first run, the system auto-seeds realistic demo data so every panel is immediately populated.

---

## 🛠️ Tech Stack

| Technology | Version | Role |
|------------|---------|------|
| **Java** | 17 | Core application language |
| **Java Swing** | Built-in | Desktop GUI framework |
| **Maven** | 3.9+ | Build + dependency management |
| **MySQL** | 8.0 | Primary production database |
| **H2** | 2.2.x | Zero-config in-memory fallback |
| **Twilio SDK** | 9.x | SMS OTP for Two-Factor Authentication |
| **JavaMail** | 1.6 | Email invoice delivery |
| **Sketchfab** | Web API | 3D vehicle viewer (companion) |

---

## 📄 License

This project is protected under a **Proprietary License** — see [LICENSE](LICENSE) for full terms.

For collaboration or feature requests, open a GitHub Discussion or reach out at **pranavandhale07@gmail.com**.

---

<div align="center">

Built with ☕ by [Pranav Andhale](https://github.com/PranavAndhale)

</div>
