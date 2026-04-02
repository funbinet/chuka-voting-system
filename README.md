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

- **Linux** (Recommended): Ubuntu/Debian preferred.
- **Java JDK 17** or higher.
- **Docker** and **Docker Compose**.

### Installation & Run

We've simplified the setup process into a single script for both Linux and Windows.

**For Linux/Mac:**
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

**For Windows:**
1. **Clone & Navigate**:
   ```cmd
   git clone https://github.com/funbinet/chuka-voting-system.git
   cd chuka-voting-system
   ```
2. **Run**:
   ```cmd
   run.bat
   ```

*The script will automatically:*
- Check for Java and Docker dependencies.
- Spin up the MySQL database using Docker Compose.
- Compile the Java source files.
- Launch the application.

---

## Project Structure

```text
.
â”œâ”€â”€ src/                  # Java source code
â”‚   â”œâ”€â”€ main/
â”‚   â”‚   â”œâ”€â”€ dao/          # Data Access Objects (DB logic)
â”‚   â”‚   â”œâ”€â”€ services/     # Business logic layer
â”‚   â”‚   â”œâ”€â”€ models/       # Data entities
â”‚   â”‚   â””â”€â”€ ui/           # GUI Components (Admin/Student)
â”œâ”€â”€ resources/            # SQL scripts and assets
â”œâ”€â”€ lib/                  # External dependencies (.jar)
â”œâ”€â”€ docker-compose.yml    # Database orchestration
â””â”€â”€ run.sh                # Main entry point script
```

---

## Configuration

Default database settings are managed via environment variables in `run.sh`:

- **Host**: `localhost`
- **Port**: `3308`
- **DB Name**: `chuka_voting_db`
- **User**: `root`
- **Password**: `chuka_root_2024`

To use an external database, modify the `DB_*` exports in the `run_app` function of `run.sh`.

---

## Database Schema

The system uses a highly normalized schema with foreign key constraints. The core of the modularity lies in the `positions` table, which uses `NULL` faculty references for generic roles, allowing them to be bound to any faculty dynamically during election creation.

Seeding script: `resources/db/schema.sql`

---

## Contributors

Special thanks to the development team at Chuka University. This project is maintained and updated for academic and institutional transparency.

## License

This project is licensed under the MIT License.
