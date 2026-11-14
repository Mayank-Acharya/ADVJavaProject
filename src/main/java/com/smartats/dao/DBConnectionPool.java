package com.smartats.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBConnectionPool {

    private static volatile DBConnectionPool instance;
    private final Properties props = new Properties();

    private String activeUrl;
    private String activeUser;
    private String activePassword;
    private String activeDriver;
    private boolean isEmbedded = false;

    private DBConnectionPool() {
        loadConfiguration();
        initializeDatabase();
    }

    public static DBConnectionPool getInstance() {
        if (instance == null) {
            synchronized (DBConnectionPool.class) {
                if (instance == null) {
                    instance = new DBConnectionPool();
                }
            }
        }
        return instance;
    }

    private void loadConfiguration() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            System.err.println("[SmartATS DB] Warning: Could not load db.properties, using defaults: " + e.getMessage());
        }

        String dbType = props.getProperty("db.type", "mysql");
        String mysqlDriver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        String mysqlUrl = props.getProperty("db.url", "jdbc:mysql://localhost:3306/smart_ats_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String mysqlUser = props.getProperty("db.user", "root");
        String mysqlPass = props.getProperty("db.password", "root");

        String embDriver = props.getProperty("embedded.driver", "org.h2.Driver");
        String embUrl = props.getProperty("embedded.url", "jdbc:h2:mem:smart_ats_db;DB_CLOSE_DELAY=-1;MODE=MySQL");
        String embUser = props.getProperty("embedded.user", "sa");
        String embPass = props.getProperty("embedded.password", "");

        if (!"embedded".equalsIgnoreCase(dbType)) {
            // Attempt MySQL connection test
            try {
                Class.forName(mysqlDriver);
                try (Connection conn = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPass)) {
                    this.activeDriver = mysqlDriver;
                    this.activeUrl = mysqlUrl;
                    this.activeUser = mysqlUser;
                    this.activePassword = mysqlPass;
                    this.isEmbedded = false;
                    System.out.println("[SmartATS DB] Successfully connected to MySQL server at: " + mysqlUrl);
                    return;
                }
            } catch (Exception ex) {
                System.err.println("[SmartATS DB] MySQL is unreachable (" + ex.getMessage() + ").");
                System.err.println("[SmartATS DB] Activating zero-config embedded MySQL-compatible database for seamless operation.");
            }
        }

        // Fallback or explicit embedded mode
        try {
            Class.forName(embDriver);
            this.activeDriver = embDriver;
            this.activeUrl = embUrl;
            this.activeUser = embUser;
            this.activePassword = embPass;
            this.isEmbedded = true;
            System.out.println("[SmartATS DB] Embedded MySQL-mode database active at: " + embUrl);
        } catch (ClassNotFoundException e) {
            System.err.println("[SmartATS DB] Warning: Embedded driver class not found: " + e.getMessage());
            // Default back to mysql config
            this.activeDriver = mysqlDriver;
            this.activeUrl = mysqlUrl;
            this.activeUser = mysqlUser;
            this.activePassword = mysqlPass;
        }
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            if (isEmbedded) {
                // H2 MySQL-mode DDL
                stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                        + "user_id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "username VARCHAR(50) UNIQUE NOT NULL, "
                        + "password_hash VARCHAR(64) NOT NULL, "
                        + "full_name VARCHAR(100) NOT NULL, "
                        + "company_name VARCHAR(100) NULL, "
                        + "email VARCHAR(100) UNIQUE NOT NULL, "
                        + "mobile VARCHAR(20) NULL, "
                        + "role VARCHAR(20) NOT NULL, "
                        + "resume_file_path VARCHAR(255) NULL, "
                        + "resume_text CLOB NULL, "
                        + "candidate_exp_years INT DEFAULT 0, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ");");

                stmt.execute("CREATE TABLE IF NOT EXISTS job_postings ("
                        + "job_id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "recruiter_id INT NOT NULL, "
                        + "company_name VARCHAR(100) NOT NULL, "
                        + "job_title VARCHAR(100) NOT NULL, "
                        + "job_role VARCHAR(100) NOT NULL, "
                        + "custom_skills_csv CLOB NULL, "
                        + "salary DECIMAL(10, 2) NOT NULL, "
                        + "min_exp_years INT NOT NULL DEFAULT 0, "
                        + "work_mode VARCHAR(20) NOT NULL, "
                        + "min_ats_threshold DECIMAL(5, 2) NOT NULL DEFAULT 60.00, "
                        + "vacancy_limit_k INT NOT NULL DEFAULT 2, "
                        + "status VARCHAR(20) DEFAULT 'OPEN', "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "FOREIGN KEY (recruiter_id) REFERENCES users(user_id) ON DELETE CASCADE"
                        + ");");

                stmt.execute("CREATE TABLE IF NOT EXISTS applications ("
                        + "app_id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "job_id INT NOT NULL, "
                        + "applicant_id INT NOT NULL, "
                        + "calculated_ats_score DECIMAL(5, 2) NOT NULL, "
                        + "matched_skills CLOB NULL, "
                        + "missing_skills CLOB NULL, "
                        + "status VARCHAR(20) DEFAULT 'APPLIED', "
                        + "interview_date VARCHAR(50) NULL, "
                        + "interview_link VARCHAR(255) NULL, "
                        + "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "CONSTRAINT uq_job_applicant UNIQUE (job_id, applicant_id), "
                        + "FOREIGN KEY (job_id) REFERENCES job_postings(job_id) ON DELETE CASCADE, "
                        + "FOREIGN KEY (applicant_id) REFERENCES users(user_id) ON DELETE CASCADE"
                        + ");");

                stmt.execute("CREATE TABLE IF NOT EXISTS notifications ("
                        + "notif_id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "user_id INT NOT NULL, "
                        + "title VARCHAR(150) NOT NULL, "
                        + "message CLOB NOT NULL, "
                        + "is_read BOOLEAN DEFAULT FALSE, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE"
                        + ");");
            } else {
                // Native MySQL DDL
                stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                        + "user_id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "username VARCHAR(50) UNIQUE NOT NULL, "
                        + "password_hash VARCHAR(64) NOT NULL, "
                        + "full_name VARCHAR(100) NOT NULL, "
                        + "company_name VARCHAR(100) NULL, "
                        + "email VARCHAR(100) UNIQUE NOT NULL, "
                        + "mobile VARCHAR(20) NULL, "
                        + "role ENUM('RECRUITER', 'APPLICANT') NOT NULL, "
                        + "resume_file_path VARCHAR(255) NULL, "
                        + "resume_text LONGTEXT NULL, "
                        + "candidate_exp_years INT DEFAULT 0, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ") ENGINE=InnoDB;");

                stmt.execute("CREATE TABLE IF NOT EXISTS job_postings ("
                        + "job_id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "recruiter_id INT NOT NULL, "
                        + "company_name VARCHAR(100) NOT NULL, "
                        + "job_title VARCHAR(100) NOT NULL, "
                        + "job_role VARCHAR(100) NOT NULL, "
                        + "custom_skills_csv TEXT NULL, "
                        + "salary DECIMAL(10, 2) NOT NULL, "
                        + "min_exp_years INT NOT NULL DEFAULT 0, "
                        + "work_mode ENUM('REMOTE', 'ONSITE', 'HYBRID') NOT NULL, "
                        + "min_ats_threshold DECIMAL(5, 2) NOT NULL DEFAULT 60.00, "
                        + "vacancy_limit_k INT NOT NULL DEFAULT 2, "
                        + "status ENUM('OPEN', 'SHORTLISTED', 'CLOSED') DEFAULT 'OPEN', "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "FOREIGN KEY (recruiter_id) REFERENCES users(user_id) ON DELETE CASCADE"
                        + ") ENGINE=InnoDB;");

                stmt.execute("CREATE TABLE IF NOT EXISTS applications ("
                        + "app_id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "job_id INT NOT NULL, "
                        + "applicant_id INT NOT NULL, "
                        + "calculated_ats_score DECIMAL(5, 2) NOT NULL, "
                        + "matched_skills TEXT NULL, "
                        + "missing_skills TEXT NULL, "
                        + "status ENUM('APPLIED', 'SELECTED', 'REJECTED') DEFAULT 'APPLIED', "
                        + "interview_date VARCHAR(50) NULL, "
                        + "interview_link VARCHAR(255) NULL, "
                        + "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "UNIQUE KEY uq_job_applicant (job_id, applicant_id), "
                        + "FOREIGN KEY (job_id) REFERENCES job_postings(job_id) ON DELETE CASCADE, "
                        + "FOREIGN KEY (applicant_id) REFERENCES users(user_id) ON DELETE CASCADE"
                        + ") ENGINE=InnoDB;");

                stmt.execute("CREATE TABLE IF NOT EXISTS notifications ("
                        + "notif_id INT AUTO_INCREMENT PRIMARY KEY, "
                        + "user_id INT NOT NULL, "
                        + "title VARCHAR(150) NOT NULL, "
                        + "message TEXT NOT NULL, "
                        + "is_read BOOLEAN DEFAULT FALSE, "
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                        + "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE"
                        + ") ENGINE=InnoDB;");
            }
            System.out.println("[SmartATS DB] Core schema validated and initialized successfully.");
        } catch (SQLException e) {
            System.err.println("[SmartATS DB] Schema auto-init notice: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName(activeDriver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database driver not found: " + activeDriver, e);
        }
        return DriverManager.getConnection(activeUrl, activeUser, activePassword);
    }

    public boolean isEmbedded() {
        return isEmbedded;
    }

    public Properties getProperties() {
        return props;
    }
}
