package com.smartats.model;

import java.sql.Timestamp;

public class JobPosting {
    private int jobId;
    private int recruiterId;
    private String companyName;
    private String jobTitle;
    private String jobRole;
    private String customSkillsCsv;
    private double salary;
    private int minExpYears;
    private String workMode; // 'REMOTE', 'ONSITE', 'HYBRID'
    private double minAtsThreshold;
    private int vacancyLimitK;
    private String status; // 'OPEN', 'SHORTLISTED', 'CLOSED'
    private Timestamp createdAt;

    // Derived fields for dashboard presentation
    private int applicantCount;

    public JobPosting() {}

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(int recruiterId) {
        this.recruiterId = recruiterId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getJobRole() {
        return jobRole;
    }

    public void setJobRole(String jobRole) {
        this.jobRole = jobRole;
    }

    public String getCustomSkillsCsv() {
        return customSkillsCsv;
    }

    public void setCustomSkillsCsv(String customSkillsCsv) {
        this.customSkillsCsv = customSkillsCsv;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public int getMinExpYears() {
        return minExpYears;
    }

    public void setMinExpYears(int minExpYears) {
        this.minExpYears = minExpYears;
    }

    public String getWorkMode() {
        return workMode;
    }

    public void setWorkMode(String workMode) {
        this.workMode = workMode;
    }

    public double getMinAtsThreshold() {
        return minAtsThreshold;
    }

    public void setMinAtsThreshold(double minAtsThreshold) {
        this.minAtsThreshold = minAtsThreshold;
    }

    public int getVacancyLimitK() {
        return vacancyLimitK;
    }

    public void setVacancyLimitK(int vacancyLimitK) {
        this.vacancyLimitK = vacancyLimitK;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getApplicantCount() {
        return applicantCount;
    }

    public void setApplicantCount(int applicantCount) {
        this.applicantCount = applicantCount;
    }
}
