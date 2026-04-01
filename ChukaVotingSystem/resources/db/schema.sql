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
    password_salt       VARCHAR(50)  NOT NULL,
    faculty_id          INT          NOT NULL,
    year_of_study       INT          NOT NULL,
    gpa                 DECIMAL(3,2) DEFAULT 0.00,
    has_discipline_case BOOLEAN      DEFAULT FALSE,
    is_verified         BOOLEAN      DEFAULT FALSE,
    is_active           BOOLEAN      DEFAULT TRUE,
    password_changed    BOOLEAN      DEFAULT FALSE,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (faculty_id) REFERENCES faculties(faculty_id)
);

CREATE TABLE admins (
    admin_id      INT PRIMARY KEY AUTO_INCREMENT,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone_number  VARCHAR(15)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(50)  NOT NULL,
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

CREATE TABLE announcements (
    id         INT PRIMARY KEY AUTO_INCREMENT,
    title      VARCHAR(200) NOT NULL,
    body       TEXT NOT NULL,
    posted_by  INT NOT NULL,
    is_active  BOOLEAN DEFAULT TRUE,
    posted_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (posted_by) REFERENCES admins(admin_id)
);

-- ============================================
--   SEED DATA
-- ============================================
INSERT INTO faculties (faculty_code, faculty_name) VALUES
('FET',  'Faculty of Engineering and Technology'),
('FBE',  'Faculty of Business and Economics'),
('FHSS', 'Faculty of Humanities and Social Sciences'),
('Faculty of Education Studies'),
('Faculty of Natural and Applied Sciences');

-- Sample Students (Password: Student@123)
-- Hash: 5ea99453f4712f50a409c7177a8e0436da4352a8e830b037f2ba0966320b6900
-- Salt: AAAAAAAAAAAAAAAAAAAAAA==
INSERT INTO students (reg_number, full_name, email, phone_number, password_hash, password_salt, faculty_id, year_of_study, gpa, is_verified, password_changed) VALUES
('EB1/66840/23', 'John Doe', 'john.doe@chuka.ac.ke', '0711111111', '55c7d632829e209bda2099d7d045e331f811276c69d2196add304e72a75070ff', 'AAAAAAAAAAAAAAAAAAAAAA==', 1, 2, 3.50, TRUE, FALSE),
('CS1/12345/22', 'Alice Smith', 'alice.smith@chuka.ac.ke', '0722222222', '55c7d632829e209bda2099d7d045e331f811276c69d2196add304e72a75070ff', 'AAAAAAAAAAAAAAAAAAAAAA==', 5, 3, 3.85, TRUE, FALSE),
('BE1/55443/23', 'Bob Johnson', 'bob.johnson@chuka.ac.ke', '0733333333', '55c7d632829e209bda2099d7d045e331f811276c69d2196add304e72a75070ff', 'AAAAAAAAAAAAAAAAAAAAAA==', 2, 1, 3.20, TRUE, FALSE),
('HS1/99887/21', 'Catherine Mwangi', 'catherine.m@chuka.ac.ke', '0744444444', '55c7d632829e209bda2099d7d045e331f811276c69d2196add304e72a75070ff', 'AAAAAAAAAAAAAAAAAAAAAA==', 3, 4, 3.90, TRUE, FALSE),
('ED1/11223/22', 'David Omondi', 'david.o@chuka.ac.ke', '0755555555', '55c7d632829e209bda2099d7d045e331f811276c69d2196add304e72a75070ff', 'AAAAAAAAAAAAAAAAAAAAAA==', 4, 3, 3.45, TRUE, FALSE),
('EB1/77665/23', 'Eve Wambui', 'eve.w@chuka.ac.ke', '0766666666', '55c7d632829e209bda2099d7d045e331f811276c69d2196add304e72a75070ff', 'AAAAAAAAAAAAAAAAAAAAAA==', 1, 2, 3.10, TRUE, FALSE),
('CS1/88990/22', 'Franklin Mutua', 'franklin.m@chuka.ac.ke', '0777777777', '55c7d632829e209bda2099d7d045e331f811276c69d2196add304e72a75070ff', 'AAAAAAAAAAAAAAAAAAAAAA==', 5, 3, 3.65, TRUE, FALSE),
('BE1/44332/23', 'Grace Atieno', 'grace.a@chuka.ac.ke', '0788888888', '55c7d632829e209bda2099d7d045e331f811276c69d2196add304e72a75070ff', 'AAAAAAAAAAAAAAAAAAAAAA==', 2, 1, 3.75, TRUE, FALSE),
('HS1/22334/21', 'Henry Kiprotich', 'henry.k@chuka.ac.ke', '0799999999', '55c7d632829e209bda2099d7d045e331f811276c69d2196add304e72a75070ff', 'AAAAAAAAAAAAAAAAAAAAAA==', 3, 4, 3.30, TRUE, FALSE),
('ED1/55667/22', 'Irene Nyambura', 'irene.n@chuka.ac.ke', '0700112233', '55c7d632829e209bda2099d7d045e331f811276c69d2196add304e72a75070ff', 'AAAAAAAAAAAAAAAAAAAAAA==', 4, 3, 3.55, TRUE, FALSE);

INSERT INTO admins (full_name, email, phone_number, password_hash, password_salt) VALUES
('System Admin', 'admin@chuka.ac.ke', '0700000000','' '55c7d632829e209bda2099d7d045e331f811276c69d2196add304e72a75070ff',
 'AAAAAAAAAAAAAAAAAAAAAA==');

INSERT INTO positions (position_name, faculty_id) VALUES
('Faculty Chairman', 1), ('Faculty Chairman', 2), ('Faculty Chairman', 3),
('Faculty Chairman', 4), ('Faculty Chairman', 5),
('Faculty Secretary', 1), ('Faculty Secretary', 2), ('Faculty Secretary', 3),
('Faculty Secretary', 4), ('Faculty Secretary', 5),
('Faculty Treasurer', 1), ('Faculty Treasurer', 2), ('Faculty Treasurer', 3),
('Faculty Treasurer', 4), ('Faculty Treasurer', 5),
('Male Resident', 1), ('Male Resident', 2), ('Male Resident', 3), ('Male Resident', 4), ('Male Resident', 5),
('Female Resident', 1), ('Female Resident', 2), ('Female Resident', 3), ('Female Resident', 4), ('Female Resident', 5),
('Male Non-Resident', 1), ('Male Non-Resident', 2), ('Male Non-Resident', 3), ('Male Non-Resident', 4), ('Male Non-Resident', 5),
('Female Non-Resident', 1), ('Female Non-Resident', 2), ('Female Non-Resident', 3), ('Female Non-Resident', 4), ('Female Non-Resident', 5);
