package com.smartats.model;

import java.sql.Timestamp;

public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String companyName;
    private String email;
    private String mobile;
    private String role; // 'RECRUITER' or 'APPLICANT'
    private String resumeFilePath;
    private String resumeText;
    private int candidateExpYears;
    private Timestamp createdAt;

    public User() {}

    public User(String username, String passwordHash, String fullName, String companyName,
                String email, String mobile, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.companyName = companyName;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getResumeFilePath() {
        return resumeFilePath;
    }

    public void setResumeFilePath(String resumeFilePath) {
        this.resumeFilePath = resumeFilePath;
    }

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    public int getCandidateExpYears() {
        return candidateExpYears;
    }

    public void setCandidateExpYears(int candidateExpYears) {
        this.candidateExpYears = candidateExpYears;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
