<div align="center">

<br/>

```
 ██████╗ █████╗ ██████╗      ███████╗██╗  ██╗ ██████╗ ██╗    ██╗██████╗  ██████╗  ██████╗ ███╗   ███╗
██╔════╝██╔══██╗██╔══██╗     ██╔════╝██║  ██║██╔═══██╗██║    ██║██╔══██╗██╔═══██╗██╔═══██╗████╗ ████║
██║     ███████║██████╔╝     ███████╗███████║██║   ██║██║ █╗ ██║██████╔╝██║   ██║██║   ██║██╔████╔██║
██║     ██╔══██║██╔══██╗     ╚════██║██╔══██║██║   ██║██║███╗██║██╔══██╗██║   ██║██║   ██║██║╚██╔╝██║
╚██████╗██║  ██║██║  ██║     ███████║██║  ██║╚██████╔╝╚███╔███╔╝██║  ██║╚██████╔╝╚██████╔╝██║ ╚═╝ ██║
 ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝     ╚══════╝╚═╝  ╚═╝ ╚═════╝  ╚══╝╚══╝ ╚═╝  ╚═╝ ╚═════╝  ╚═════╝ ╚═╝     ╚═╝
```

### **AutoElite Dealership Management System**
*A full-featured enterprise dealership platform — inventory, sales, customer management, invoicing and 2FA security, all in one Java desktop application.*

<br/>

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Twilio](https://img.shields.io/badge/Twilio-2FA%20SMS-F22F46?style=for-the-badge&logo=twilio&logoColor=white)](https://www.twilio.com/)
[![License](https://img.shields.io/badge/License-Proprietary-red?style=for-the-badge)](LICENSE)

</div>

---

## 🚗 What is AutoElite?

**AutoElite** is a professional **Dealership Management System (DMS)** built entirely in Java 17. It gives car dealerships a single, powerful desktop application to manage their entire operation — from tracking inventory and processing sales transactions to maintaining customer records and generating polished PDF invoices.

It's engineered with a clean **MVC (Model-View-Controller)** architecture, making it maintainable, extensible, and production-ready.

---

## ✨ Key Features

### 🏎️ Inventory Management
- Add, update, and remove vehicle listings with full spec details (make, model, year, VIN, price)
- Real-time stock tracking — know exactly what's on the lot
- Filter and search inventory by brand, price range, or availability status

### 👥 Customer Relationship Management (CRM)
- Store and manage complete customer profiles (contact details, purchase history)
- Link customers to their vehicle purchases for full traceability
- Customer search and quick lookup by name, phone, or ID

### 💰 Sales & Transactions
- Process complete end-to-end vehicle sale transactions
- Automatic inventory deduction on sale completion
- Sales history and transaction audit trail

### 🧾 Invoice Generation
- Generate professional, formatted PDF invoices on every sale
- Invoices emailed directly to the customer via **JavaMail (SMTP)** integration
- Itemized billing with tax and total calculations

### 🔐 Security — Two-Factor Authentication (2FA)
- Staff login secured with username + password **plus** an SMS OTP via **Twilio**
- Prevents unauthorized access to sensitive dealership data
- Session management with secure logout

### 🗄️ Database
- **MySQL** as the primary production database
- **H2 (in-memory)** as an automatic fallback for development/testing — zero setup required
- Clean schema with normalized tables for vehicles, customers, sales, and users

---

## 🏗️ Architecture

```
AutoElite/
├── src/
│   ├── controllers/        # Business logic — bridges UI and models
│   ├── models/             # Java POJOs: Vehicle, Customer, Sale, User
│   ├── ui/                 # Swing-based GUI panels and forms
│   ├── utils/              # DB connection, email sender, OTP generator
│   └── resources/          # Config files, assets
├── web-configurator/       # Web-based setup wizard for first-time config
├── application.properties.example  # Config template (copy → application.properties)
├── pom.xml                 # Maven build file
└── run.sh                  # One-command build + launch script
```

**Pattern**: MVC — `controllers` handle all logic, `models` define data structures, `ui` renders the interface, `utils` wraps external services.

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.0 (or use the built-in H2 fallback)
- A Twilio account (for 2FA SMS)

### 1. Clone the repo
```bash
git clone https://github.com/PranavAndhale/CarShowroom-2.git
cd CarShowroom-2
```

### 2. Configure the application
```bash
# Copy the example config
cp application.properties.example application.properties

# Edit application.properties with your actual values:
#   db.url      → your MySQL JDBC URL
#   db.user     → MySQL username
#   db.password → MySQL password
```

> **Note**: `application.properties` is already in `.gitignore` — your credentials are never committed.

### 3. Build & Run (one command)
```bash
./run.sh
```

Or manually:
```bash
mvn clean package -q
java -jar target/car-showroom-pro-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

The application window will launch automatically.

---

## ⚙️ Configuration Reference

| Property | Description | Example |
|----------|-------------|---------|
| `db.url` | MySQL JDBC connection string | `jdbc:mysql://localhost:3306/car_showroom` |
| `db.user` | Database username | `root` |
| `db.password` | Database password | `yourpassword` |
| `app.debug` | Enable verbose logging | `true` / `false` |

---

## 🛠️ Tech Stack

| Technology | Role |
|------------|------|
| **Java 17** | Core application language |
| **Swing** | Desktop GUI framework |
| **Maven** | Build + dependency management |
| **MySQL 8.0** | Primary production database |
| **H2** | In-memory DB fallback (dev/test) |
| **Twilio SDK** | SMS-based Two-Factor Authentication |
| **JavaMail** | Email invoice delivery via SMTP |

---

## 📄 License

This project is licensed under a **Proprietary License** — see [LICENSE](LICENSE) for details.

---

<div align="center">

Built with ☕ by [Pranav Andhale](https://github.com/PranavAndhale)

</div>
