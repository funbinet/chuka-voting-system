-- ============================================
--   CHUKA UNIVERSITY VOTING SYSTEM - SCHEMA
-- ============================================

CREATE DATABASE IF NOT EXISTS chuka_voting_db;
USE chuka_voting_db;

CREATE TABLE faculties (
    faculty_id   INT PRIMARY KEY AUTO_INCREMENT,
    faculty_code VARCHAR(10)  NOT NULL UNIQUE,
    faculty_name VARCHAR(100) NOT NULL
);

CREATE TABLE positions (
    position_id   INT PRIMARY KEY AUTO_INCREMENT,
    position_name VARCHAR(100) NOT NULL,
    faculty_id    INT,
    FOREIGN KEY (faculty_id) REFERENCES faculties(faculty_id)
);

CREATE TABLE students (
    student_id          INT PRIMARY KEY AUTO_INCREMENT,
    reg_number          VARCHAR(20)  NOT NULL UNIQUE,
    full_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(100) NOT NULL UNIQUE,
    phone_number        VARCHAR(15)  NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    faculty_id          INT          NOT NULL,
    year_of_study       INT          NOT NULL,
    gpa                 DECIMAL(3,2) DEFAULT 0.00,
    has_discipline_case BOOLEAN      DEFAULT FALSE,
    is_verified         BOOLEAN      DEFAULT FALSE,
    is_active           BOOLEAN      DEFAULT TRUE,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (faculty_id) REFERENCES faculties(faculty_id)
);

CREATE TABLE admins (
    admin_id      INT PRIMARY KEY AUTO_INCREMENT,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone_number  VARCHAR(15)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active     BOOLEAN      DEFAULT TRUE,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE candidate_applications (
    application_id   INT PRIMARY KEY AUTO_INCREMENT,
    student_id       INT          NOT NULL,
    position_id      INT          NOT NULL,
    manifesto        TEXT,
    nomination_count INT          DEFAULT 0,
    status           ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
    rejection_reason VARCHAR(255),
    applied_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    reviewed_at      TIMESTAMP    NULL,
    reviewed_by      INT,
    FOREIGN KEY (student_id)  REFERENCES students(student_id),
    FOREIGN KEY (position_id) REFERENCES positions(position_id),
    FOREIGN KEY (reviewed_by) REFERENCES admins(admin_id),
    UNIQUE KEY unique_application (student_id, position_id)
);

CREATE TABLE nominations (
    nomination_id  INT PRIMARY KEY AUTO_INCREMENT,
    application_id INT NOT NULL,
    nominated_by   INT NOT NULL,
    nominated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (application_id) REFERENCES candidate_applications(application_id),
    FOREIGN KEY (nominated_by)   REFERENCES students(student_id),
    UNIQUE KEY unique_nomination (application_id, nominated_by)
);

CREATE TABLE elections (
    election_id INT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(150) NOT NULL,
    faculty_id  INT          NOT NULL,
    start_date  DATETIME     NOT NULL,
    end_date    DATETIME     NOT NULL,
    status      ENUM('UPCOMING','ACTIVE','CLOSED') DEFAULT 'UPCOMING',
    created_by  INT          NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (faculty_id) REFERENCES faculties(faculty_id),
    FOREIGN KEY (created_by) REFERENCES admins(admin_id)
);

CREATE TABLE election_candidates (
    id             INT PRIMARY KEY AUTO_INCREMENT,
    election_id    INT NOT NULL,
    application_id INT NOT NULL,
    FOREIGN KEY (election_id)    REFERENCES elections(election_id),
    FOREIGN KEY (application_id) REFERENCES candidate_applications(application_id),
    UNIQUE KEY unique_entry (election_id, application_id)
);

CREATE TABLE votes (
    vote_id                  INT PRIMARY KEY AUTO_INCREMENT,
    student_id               INT NOT NULL,
    election_id              INT NOT NULL,
    position_id              INT NOT NULL,
    candidate_application_id INT NOT NULL,
    voted_at                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id)   REFERENCES students(student_id),
    FOREIGN KEY (election_id)  REFERENCES elections(election_id),
    FOREIGN KEY (position_id)  REFERENCES positions(position_id),
    FOREIGN KEY (candidate_application_id) REFERENCES candidate_applications(application_id),
    UNIQUE KEY one_vote_per_position (student_id, election_id, position_id)
);

CREATE TABLE otp_logs (
    otp_id       INT PRIMARY KEY AUTO_INCREMENT,
    phone_number VARCHAR(15) NOT NULL,
    otp_code     VARCHAR(6)  NOT NULL,
    is_used      BOOLEAN     DEFAULT FALSE,
    expires_at   TIMESTAMP   NOT NULL,
    created_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    log_id      INT PRIMARY KEY AUTO_INCREMENT,
    student_id  INT,
    action      VARCHAR(100) NOT NULL,
    description TEXT,
    logged_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(student_id)
);

-- ============================================
--   SEED DATA
-- ============================================
INSERT INTO faculties (faculty_code, faculty_name) VALUES
('FET',  'Faculty of Engineering and Technology'),
('FBE',  'Faculty of Business and Economics'),
('FHSS', 'Faculty of Humanities and Social Sciences'),
('FES',  'Faculty of Education Studies'),
('FNAS', 'Faculty of Natural and Applied Sciences');

INSERT INTO admins (full_name, email, phone_number, password_hash) VALUES
('System Admin', 'admin@chuka.ac.ke', '0700000000',
 SHA2('Admin@1234', 256));

INSERT INTO positions (position_name, faculty_id) VALUES
('Faculty Chairman', 1), ('Faculty Chairman', 2), ('Faculty Chairman', 3),
('Faculty Chairman', 4), ('Faculty Chairman', 5),
('Faculty Secretary', 1), ('Faculty Secretary', 2), ('Faculty Secretary', 3),
('Faculty Secretary', 4), ('Faculty Secretary', 5),
('Faculty Treasurer', 1), ('Faculty Treasurer', 2), ('Faculty Treasurer', 3),
('Faculty Treasurer', 4), ('Faculty Treasurer', 5);
