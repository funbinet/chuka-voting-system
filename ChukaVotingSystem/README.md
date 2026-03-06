# 🎓 Chuka University Voting System

A Java Swing desktop application for managing student elections at Chuka University.

---

## 📋 Features

- **Student Registration & Login** with OTP phone verification
- **Faculty-based voting** — students only vote in their faculty elections
- **Candidate eligibility checks** (GPA, year of study, discipline record)
- **Peer nomination system** — candidates need 10 nominations before approval
- **Admin panel** to create elections, approve/reject candidates, and view results
- **Live results** with winner announcements
- **Audit trail** for all voting activity

---

## 🛠️ Setup Instructions

### 1. Prerequisites
- Java JDK 11 or higher
- IntelliJ IDEA
- MySQL Server 8.0+
- MySQL Workbench (optional)

### 2. Database Setup
```sql
-- Run resources/db/schema.sql in MySQL
-- This creates the database, tables, and seed data
```
Default admin credentials:
- **Email:** admin@chuka.ac.ke
- **Password:** Admin@1234

### 3. Configure Database Connection
Edit `src/main/utils/DBConfig.java`:
```java
public static final String USERNAME = "root";        // Your MySQL username
public static final String PASSWORD = "your_password"; // Your MySQL password
```

### 4. Add MySQL Connector JAR
1. Download from: https://dev.mysql.com/downloads/connector/j/
2. Place the `.jar` file in the `/lib` folder
3. In IntelliJ: Right-click the JAR → **Add as Library**

### 5. Run the Project
- Open `src/main/Main.java`
- Click the green ▶ Run button
- Login screen will appear!

---

## 📁 Project Structure

```
ChukaVotingSystem/
├── src/main/
│   ├── Main.java                    ← Entry point
│   ├── models/                      ← Data classes
│   ├── dao/                         ← Database access
│   ├── services/                    ← Business logic
│   ├── ui/                          ← Swing screens
│   │   ├── admin/                   ← Admin screens
│   │   └── student/                 ← Student screens
│   └── utils/                       ← Helpers & config
├── resources/db/schema.sql          ← Database schema
└── lib/                             ← MySQL JAR goes here
```

---

## 🗺️ User Flow

### Student
1. Register → Login → OTP verification → Dashboard
2. Apply for candidacy (if eligible) → Collect nominations
3. Vote in active faculty elections
4. View results after elections close

### Admin
1. Login → Admin Dashboard
2. Review & approve/reject candidate applications
3. Create elections for each faculty
4. Activate elections / close them
5. View live and final results

---

## 👥 Faculties Supported
| Code | Faculty |
|------|---------|
| FET  | Faculty of Engineering and Technology |
| FBE  | Faculty of Business and Economics |
| FHSS | Faculty of Humanities and Social Sciences |
| FES  | Faculty of Education Studies |
| FNAS | Faculty of Natural and Applied Sciences |

---

## ⚙️ Candidate Eligibility Criteria
- GPA ≥ 2.5
- Year of study ≥ 2 (no first years)
- No active disciplinary cases
- Phone number verified (OTP)
- Minimum 10 peer nominations

---

## 📱 OTP Note
Currently in **simulation mode** — OTPs are printed to the console.
To enable real SMS, replace the `sendOTP()` method in `OTPService.java`
with Africa's Talking or Twilio API integration.

---

*Built with Java Swing | Chuka University 2024*
