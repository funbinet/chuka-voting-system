# Chuka University Voting System (CUVS)

A clean, modular, and secure electronic voting platform designed for university elections. This system handles student candidacies, faculty-based administration, and real-time visual analytics for election results.

## Overview

The CUVS is built with a focus on **modular architecture** and **database resilience**. It replaces legacy faculty-specific position structures with a canonical set of 7 position types, enabling dynamic election creation for any faculty or university-wide role.

### Key Features

- **Generic Position Framework**: Support for Faculty Chairman, Secretary, Treasurer, and Residence-based roles without hardcoded faculty dependencies.
- **Robust Database Connectivity**: Implements an auto-reconnect singleton pattern (`DBConnection`) to handle MySQL session timeouts gracefully.
- **Automated Administration**: Bulk student imports via CSV, direct candidate management, and real-time audit logging.
- **Secure Authentication**: OTP-based verification for first-time logins and SHA-256 password hashing.
- **Visual Analytics**: Interactive bar charts and data visualization for live election results.

---

## Technical Stack

- **Lanuage**: Java 17+
- **Frontend**: Java Swing (Custom UI Components)
- **Database**: MySQL 8.0
- **Containerization**: Docker & Docker Compose
- **Build System**: Custom Shell Wrapper (`run.sh`)

---

## Getting Started

### Prerequisites

- **Linux** (Recommended): Docker and Docker Compose installed.
- **Windows**: 
  - **Java JDK 17+** installed.
  - **MySQL Server** installed and running.
  - **MySQL Workbench** or similar tool for schema management.

---

## Installation & Run

### 🐧 For Linux (Automated via Docker)

The setup process for Linux is fully automated using Docker.

1. **Clone & Navigate**:
   ```bash
   git clone https://github.com/funbinet/chuka-voting-system.git
   cd chuka-voting-system
   ```
2. **Run**:
   ```bash
   chmod +x run.sh
   ./run.sh
   ```

*The script will automatically:*
- Spin up the MySQL database using Docker Compose.
- Compile the Java source files and launch the application.

---

### 🪟 For Windows (Manual Configuration)

Windows users should follow these manual steps to set up the environment:

#### 1. Prepare the Database
- Open **MySQL Workbench** or your preferred MySQL client.
- Open and run the SQL schema script located at: `resources/db/schema.sql`.
- This will create the `chuka_voting_db` and all necessary tables.

#### 2. Configure the Application
- Navigate to the `resources/` folder.
- Open `config.properties` in a text editor.
- Update the following fields to match your local MySQL credentials:
  ```properties
  db.host=localhost
  db.port=3306
  db.name=chuka_voting_db
  db.user=root
  db.password=your_password_here
  ```

#### 3. Run the Application
You can run the application using your IDE (like IntelliJ or Eclipse) or via the command line:

- **Using Command Line**:
  1. Open PowerShell or CMD in the project root.
  2. Compile the project (ensure `javac` is in your PATH):
     ```powershell
     mkdir out
     dir /s /b src\*.java > sources.txt
     javac -cp "lib/*" -d out @sources.txt
     del sources.txt
     xcopy /e /i /y resources out
     ```
  3. Launch the application:
     ```powershell
     java -cp "out;lib/*" main.Main
     ```

---

## Configuration

The application uses a multi-layered configuration approach managed in `src/main/utils/DBConfig.java`:

1.  **Environment Variables**: Highest priority (used by Docker on Linux).
2.  **config.properties**: Used for manual setup (Primary method for Windows).
3.  **Default Values**: Fallback credentials.

**Config File Location**: `resources/config.properties`

---

## Database Schema

The system uses a highly normalized schema with foreign key constraints. The core of the modularity lies in the `positions` table, which uses `NULL` faculty references for generic roles, allowing them to be bound to any faculty dynamically during election creation.

Seeding script: `resources/db/schema.sql`

---

## Contributors

Special thanks to the development team at Chuka University. This project is maintained and updated for academic and institutional transparency.

## License

This project is licensed under the MIT License.
