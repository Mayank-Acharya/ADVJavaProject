-- ====================================================================
-- SmartATS: Resume Matchmaking and Top-K Shortlisting Database Schema
-- Database Engine: MySQL 8.x
-- ====================================================================

CREATE DATABASE IF NOT EXISTS smart_ats_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smart_ats_db;

-- 1. Users Table (Authentication and profile for Recruiters & Applicants)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(64) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    company_name VARCHAR(100) NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    mobile VARCHAR(20) NULL,
    role ENUM('RECRUITER', 'APPLICANT') NOT NULL,
    resume_file_path VARCHAR(255) NULL,
    resume_text LONGTEXT NULL,
    candidate_exp_years INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Job Postings Table (Stores the Fill-in-the-Blank Sentence Job Postings)
CREATE TABLE IF NOT EXISTS job_postings (
    job_id INT AUTO_INCREMENT PRIMARY KEY,
    recruiter_id INT NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    job_title VARCHAR(100) NOT NULL,
    job_role VARCHAR(100) NOT NULL,
    custom_skills_csv TEXT NULL,
    salary DECIMAL(10, 2) NOT NULL,
    min_exp_years INT NOT NULL DEFAULT 0,
    work_mode ENUM('REMOTE', 'ONSITE', 'HYBRID') NOT NULL,
    min_ats_threshold DECIMAL(5, 2) NOT NULL DEFAULT 60.00,
    vacancy_limit_k INT NOT NULL DEFAULT 2,
    status ENUM('OPEN', 'SHORTLISTED', 'CLOSED') DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recruiter_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Applications Table (Stores matched scores and status for applicants)
CREATE TABLE IF NOT EXISTS applications (
    app_id INT AUTO_INCREMENT PRIMARY KEY,
    job_id INT NOT NULL,
    applicant_id INT NOT NULL,
    calculated_ats_score DECIMAL(5, 2) NOT NULL,
    matched_skills TEXT NULL,
    missing_skills TEXT NULL,
    status ENUM('APPLIED', 'SELECTED', 'REJECTED') DEFAULT 'APPLIED',
    interview_date VARCHAR(50) NULL,
    interview_link VARCHAR(255) NULL,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_job_applicant (job_id, applicant_id),
    FOREIGN KEY (job_id) REFERENCES job_postings(job_id) ON DELETE CASCADE,
    FOREIGN KEY (applicant_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. Notifications Table (Dual-layer in-app notification bell)
CREATE TABLE IF NOT EXISTS notifications (
    notif_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Indexes for lightning-fast queries during reverse-matching and candidate rankings
CREATE INDEX idx_job_status ON job_postings(status);
CREATE INDEX idx_app_job_score ON applications(job_id, calculated_ats_score DESC);
CREATE INDEX idx_notif_user ON notifications(user_id, is_read);
