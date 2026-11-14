package com.smartats.model;

import java.sql.Timestamp;

public class Application {
    private int appId;
    private int jobId;
    private int applicantId;
    private double calculatedAtsScore;
    private String matchedSkills;
    private String missingSkills;
    private String status; // 'APPLIED', 'SELECTED', 'REJECTED'
    private String interviewDate;
    private String interviewLink;
    private Timestamp appliedAt;

    // Joined applicant details for Recruiter Dashboard view
    private String applicantName;
    private String applicantEmail;
    private String applicantMobile;
    private int applicantExpYears;
    private String resumeFilePath;

    // Joined job details for Candidate Dashboard view
    private String jobTitle;
    private String jobRole;
    private String companyName;
    private double salary;
    private String workMode;

    public Application() {}

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(int applicantId) {
        this.applicantId = applicantId;
    }

    public double getCalculatedAtsScore() {
        return calculatedAtsScore;
    }

    public void setCalculatedAtsScore(double calculatedAtsScore) {
        this.calculatedAtsScore = calculatedAtsScore;
    }

    public String getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(String matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public String getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(String missingSkills) {
        this.missingSkills = missingSkills;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(String interviewDate) {
        this.interviewDate = interviewDate;
    }

    public String getInterviewLink() {
        return interviewLink;
    }

    public void setInterviewLink(String interviewLink) {
        this.interviewLink = interviewLink;
    }

    public Timestamp getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Timestamp appliedAt) {
        this.appliedAt = appliedAt;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public String getApplicantMobile() {
        return applicantMobile;
    }

    public void setApplicantMobile(String applicantMobile) {
        this.applicantMobile = applicantMobile;
    }

    public int getApplicantExpYears() {
        return applicantExpYears;
    }

    public void setApplicantExpYears(int applicantExpYears) {
        this.applicantExpYears = applicantExpYears;
    }

    public String getResumeFilePath() {
        return resumeFilePath;
    }

    public void setResumeFilePath(String resumeFilePath) {
        this.resumeFilePath = resumeFilePath;
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public String getWorkMode() {
        return workMode;
    }

    public void setWorkMode(String workMode) {
        this.workMode = workMode;
    }
}
