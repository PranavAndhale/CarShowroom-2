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
*A complete enterprise dealership platform — built to handle every aspect of a modern car showroom operation from a single, beautifully designed Java desktop application.*

<br/>

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

From browsing a live inventory with an interactive **3D car configurator**, to processing a fully tracked sale, auto-generating a professional invoice, scheduling a test drive, and submitting a customer review — every dealership workflow is handled in one place.

Designed with a clean **MVC architecture** and a custom, modern **dark-themed Swing UI**, AutoElite feels nothing like a typical Java desktop app.

---

## ✨ Feature Highlights

### 🔮 Interactive 3D Car Configurator
The standout feature of AutoElite. Before any purchase decision, customers can explore vehicles in immersive **real-time 3D** — rotating, zooming, and inspecting the car from every angle, directly inside the application. A companion web-based configurator is also available for browser use.

---

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

## 🏗️ Architecture

```
AutoElite/
├── src/
│   ├── controllers/      # Business logic layer (8 controllers)
│   ├── models/           # OOP data models with inheritance hierarchy
│   ├── ui/               # Modern Swing GUI — sidebar, panels, configurator
│   └── utils/            # DB, security, session, config, audit utilities
├── web-configurator/     # Browser-based 3D car configurator
├── application.properties.example
├── pom.xml
└── run.sh
```

**Design**: Clean **MVC** with OOP inheritance (`Person → Employee → Manager`, `Person → Customer`).

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
| **Sketchfab** | Web API | 3D vehicle model viewer |

---

## 📄 License

This project is protected under a **Proprietary License** — see [LICENSE](LICENSE) for full terms.

For collaboration or feature requests, open a GitHub Discussion or reach out at **pranavandhale07@gmail.com**.

---

<div align="center">

Built with ☕ by [Pranav Andhale](https://github.com/PranavAndhale)

</div>
