# AutoElite Dealership Management System

![Java Version](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.8%2B-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![Twilio](https://img.shields.io/badge/Twilio-Integrated-F22F46?style=for-the-badge&logo=twilio&logoColor=white)
![License](https://img.shields.io/badge/License-Proprietary-red?style=for-the-badge)

> **AutoElite** is a production-ready, ultra-premium Desktop Application for managing every aspect of a modern car dealership — built entirely in Java (Swing/Java2D) with zero external UI frameworks.

---

## ✨ Highlights

| Area | What's Inside |
|---|---|
| 🎨 **UI & Branding** | Cinematic fullscreen login, custom Apex "A" logo, animated neon sidebar, real-time IST clock |
| 📊 **Analytics** | Custom Java2D Bézier charts — quarterly trends, category splits, payroll insights |
| 🤖 **AI Engine** | Sales Coach, Predictive Churn scoring, AI-powered Smart Car Recommender |
| 🛡️ **Security** | 2FA via Twilio SMS, RBAC, SHA password hashing, Immutable Audit Log |
| ⚡ **Search** | Real-time table filtering across Inventory, Customers, and Sales |
| 📧 **Communication** | Email & SMS notifications, test drive booking confirmations |
| 🗄️ **Database** | MySQL primary + H2 in-memory auto-fallback for zero-config testing |

---

## 🚀 Key Features

### 💎 Ultra-Premium User Interface
- **Cinematic Fullscreen Login** — edge-to-edge dark canvas with a centered glassmorphism card and the custom Apex "A" logo
- **AnimatedModernSidebar** — 60fps hover-glows, active indicator bars, and noise-texture depth, all rendered with raw Java2D
- **Custom Branded Identity** — original "Apex A" metallic + neon logo deployed across Login, Dashboard header, and Sidebar
- **Live IST Clock** — ticks every second, pinned to `Asia/Kolkata` in the top navigation bar
- **Real-Time Search & Filtering** — instant, DB-free `TableRowSorter` filtering across all major panels

### 📊 Advanced Data Visualization — Zero External Libraries
All charts are hand-drawn using `Graphics2D` and Bézier curves:
- Dual-line quarterly revenue trend (current vs. previous year)
- Category revenue bar charts with metallic gradients
- Low-stock highlights, top employee rankings, payroll summaries

### 🛡️ Enterprise-Grade Backend Logic
- **Role-Based Access Control (RBAC)** — Admin, Sales, Finance, HR roles with per-panel permission guards
- **Two-Factor Authentication (2FA)** — Twilio SMS OTP on every admin login
- **AI Predictive Churn** — algorithmically scores customers at risk of leaving based on purchase history
- **AI Sales Coach** — analyses completed and cancelled sales to provide actionable coaching insights
- **Automated DB Backup** — background scheduler silently exports MySQL snapshots
- **Immutable Audit Log** — every create/update/delete action tracked with user ID and timestamp

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| UI | Java Swing + Java2D (no external UI libs) |
| Database | MySQL 8.0+ (H2 in-memory fallback) |
| Build | Apache Maven |
| SMS / OTP | Twilio SDK |
| Email | JavaMail API |
| Config | Externalized `application.properties` |

---

## ⚙️ Setup & Installation

### Option 1: Maven (Recommended)
```bash
git clone https://github.com/PranavAndhale/CarShowroom-2.git
cd autoelite-dealership
```

Edit `application.properties` with your MySQL credentials:
```properties
db.url=jdbc:mysql://localhost:3306/car_showroom
db.user=root
db.password=yourpassword
```

Build and run:
```bash
mvn clean package
java -jar target/car-showroom-pro-1.0.0-SNAPSHOT-jar-with-dependencies.jar
```

> **No MySQL?** Leave the config as-is — the app automatically falls back to an H2 in-memory database so you can explore the full UI instantly.

### Option 2: IDE (VS Code / IntelliJ / Eclipse)
1. Open the project folder
2. Ensure Java 17+ is configured
3. Run `src/ui/CarShowroomApp.java`

---

## � Security Practices

- **Externalized Config** — DB credentials live in `application.properties`, never hardcoded
- **Password Hashing** — user passwords processed via SHA `MessageDigest` before storage
- **Safe Table Indexing** — all UI row indices converted to DB model IDs before any mutation
- **Audit Trail** — tamper-evident log of every admin action with timestamp and user context

---

## 📄 License

This project is proprietary software. Viewing is permitted; copying, redistribution, decompilation, or commercial use are strictly prohibited. See [LICENSE](LICENSE) for full terms.

---

*Built with passion by [Pranav Andhale](https://github.com/PranavAndhale)*
