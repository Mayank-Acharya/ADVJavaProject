package com.smartats.dao;

import com.smartats.model.Application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class ApplicationDAO {

    public boolean submitApplication(Application app) throws SQLException {
        String sql = "INSERT INTO applications (job_id, applicant_id, calculated_ats_score, "
                + "matched_skills, missing_skills, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, app.getJobId());
            ps.setInt(2, app.getApplicantId());
            ps.setDouble(3, app.getCalculatedAtsScore());
            ps.setString(4, app.getMatchedSkills());
            ps.setString(5, app.getMissingSkills());
            ps.setString(6, app.getStatus() != null ? app.getStatus() : "APPLIED");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        app.setAppId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean hasApplied(int jobId, int applicantId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM applications WHERE job_id = ? AND applicant_id = ?";
        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, jobId);
            ps.setInt(2, applicantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public List<Application> getApplicationsByJob(int jobId) throws SQLException {
        List<Application> list = new ArrayList<>();
        String sql = "SELECT a.*, u.full_name, u.email, u.mobile, u.candidate_exp_years, u.resume_file_path "
                + "FROM applications a "
                + "JOIN users u ON a.applicant_id = u.user_id "
                + "WHERE a.job_id = ? "
                + "ORDER BY a.calculated_ats_score DESC, a.applied_at ASC";

        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToApplication(rs, true));
                }
            }
        }
        return list;
    }

    public List<Application> getApplicationsByApplicant(int applicantId) throws SQLException {
        List<Application> list = new ArrayList<>();
        String sql = "SELECT a.*, j.job_title, j.job_role, j.company_name, j.salary, j.work_mode "
                + "FROM applications a "
                + "JOIN job_postings j ON a.job_id = j.job_id "
                + "WHERE a.applicant_id = ? "
                + "ORDER BY a.applied_at DESC";

        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, applicantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToApplication(rs, false));
                }
            }
        }
        return list;
    }

    public Application getApplicationById(int appId) throws SQLException {
        String sql = "SELECT a.*, u.full_name, u.email, u.mobile, u.candidate_exp_years, u.resume_file_path, "
                + "j.job_title, j.job_role, j.company_name, j.salary, j.work_mode "
                + "FROM applications a "
                + "JOIN users u ON a.applicant_id = u.user_id "
                + "JOIN job_postings j ON a.job_id = j.job_id "
                + "WHERE a.app_id = ?";

        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, appId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToApplication(rs, true);
                }
            }
        }
        return null;
    }

    /**
     * Top-K Automated Selection & Auto-Rejection Algorithm
     * Uses java.util.PriorityQueue (Max-Heap) with custom Comparator ordering by ATS Score descending.
     * Entire operation is executed inside an atomic ACID JDBC transaction.
     *
     * @return List of newly SELECTED applications (to trigger async SMTP alerts)
     */
    public List<Application> processTopKSelection(int jobId, int kLimit, String companyName,
                                                  String jobTitle, String interviewDate,
                                                  String interviewLink) throws SQLException {

        List<Application> selectedCandidates = new ArrayList<>();
        Connection conn = null;

        try {
            conn = DBConnectionPool.getInstance().getConnection();
            conn.setAutoCommit(false); // Begin ACID Transaction

            // 1. Fetch all candidate applications for this job
            List<Application> allApplicants = new ArrayList<>();
            String fetchSql = "SELECT a.*, u.full_name, u.email, u.mobile, u.candidate_exp_years, u.resume_file_path "
                    + "FROM applications a "
                    + "JOIN users u ON a.applicant_id = u.user_id "
                    + "WHERE a.job_id = ?";

            try (PreparedStatement psFetch = conn.prepareStatement(fetchSql)) {
                psFetch.setInt(1, jobId);
                try (ResultSet rs = psFetch.executeQuery()) {
                    while (rs.next()) {
                        allApplicants.add(mapResultSetToApplication(rs, true));
                    }
                }
            }

            if (allApplicants.isEmpty()) {
                conn.commit();
                return selectedCandidates;
            }

            // 2. Load into a PriorityQueue (Max-Heap) sorted by ATS Score descending
            PriorityQueue<Application> maxHeap = new PriorityQueue<>(
                    (app1, app2) -> Double.compare(app2.getCalculatedAtsScore(), app1.getCalculatedAtsScore())
            );
            maxHeap.addAll(allApplicants);

            // 3. Prepare Batch Statements for Status and In-App Notifications
            String updateSql = "UPDATE applications SET status = ?, interview_date = ?, interview_link = ? WHERE app_id = ?";
            String notifSql = "INSERT INTO notifications (user_id, title, message, is_read) VALUES (?, ?, ?, false)";

            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                 PreparedStatement psNotif = conn.prepareStatement(notifSql)) {

                int rank = 1;
                while (!maxHeap.isEmpty()) {
                    Application candidate = maxHeap.poll();
                    boolean isSelected = (rank <= kLimit);
                    String newStatus = isSelected ? "SELECTED" : "REJECTED";

                    // Update Candidate status
                    psUpdate.setString(1, newStatus);
                    psUpdate.setString(2, isSelected ? interviewDate : null);
                    psUpdate.setString(3, isSelected ? interviewLink : null);
                    psUpdate.setInt(4, candidate.getAppId());
                    psUpdate.addBatch();

                    // Insert Notification for candidate
                    String notifTitle = isSelected
                            ? "Congratulations! You have been SHORTLISTED by " + companyName
                            : "Application Update: " + jobTitle + " at " + companyName;

                    String notifMsg = isSelected
                            ? "Congratulations! You scored " + candidate.getCalculatedAtsScore()
                            + "% on your resume ATS evaluation and ranked #" + rank
                            + " out of the Top " + kLimit + " candidates! Interview Date: "
                            + (interviewDate != null && !interviewDate.trim().isEmpty() ? interviewDate : "TBA")
                            + ". Meeting Link: " + (interviewLink != null && !interviewLink.trim().isEmpty() ? interviewLink : "Shared via email.")
                            : "Thank you for applying for " + jobTitle + ". Your ATS match score was "
                            + candidate.getCalculatedAtsScore() + "%. Company " + companyName
                            + " has completed the Top " + kLimit + " shortlist round. We wish you the best in your job search!";

                    psNotif.setInt(1, candidate.getApplicantId());
                    psNotif.setString(2, notifTitle);
                    psNotif.setString(3, notifMsg);
                    psNotif.addBatch();

                    if (isSelected) {
                        candidate.setStatus("SELECTED");
                        candidate.setInterviewDate(interviewDate);
                        candidate.setInterviewLink(interviewLink);
                        selectedCandidates.add(candidate);
                    }

                    rank++;
                }

                // Update job status to 'SHORTLISTED'
                String updateJobSql = "UPDATE job_postings SET status = 'SHORTLISTED' WHERE job_id = ?";
                try (PreparedStatement psJob = conn.prepareStatement(updateJobSql)) {
                    psJob.setInt(1, jobId);
                    psJob.executeUpdate();
                }

                psUpdate.executeBatch();
                psNotif.executeBatch();
                conn.commit(); // Atomic commit of all candidate status changes and notifications!
            }

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback all changes if any error occurs
                } catch (SQLException rbEx) {
                    rbEx.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }

        return selectedCandidates;
    }

    public boolean updateSingleApplicationStatus(int appId, String status, String interviewDate,
                                                 String interviewLink, int applicantId,
                                                 String notifTitle, String notifMessage) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnectionPool.getInstance().getConnection();
            conn.setAutoCommit(false);

            String updateSql = "UPDATE applications SET status = ?, interview_date = ?, interview_link = ? WHERE app_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, status);
                ps.setString(2, interviewDate);
                ps.setString(3, interviewLink);
                ps.setInt(4, appId);
                ps.executeUpdate();
            }

            String notifSql = "INSERT INTO notifications (user_id, title, message, is_read) VALUES (?, ?, ?, false)";
            try (PreparedStatement psNotif = conn.prepareStatement(notifSql)) {
                psNotif.setInt(1, applicantId);
                psNotif.setString(2, notifTitle);
                psNotif.setString(3, notifMessage);
                psNotif.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    private Application mapResultSetToApplication(ResultSet rs, boolean isRecruiterView) throws SQLException {
        Application a = new Application();
        a.setAppId(rs.getInt("app_id"));
        a.setJobId(rs.getInt("job_id"));
        a.setApplicantId(rs.getInt("applicant_id"));
        a.setCalculatedAtsScore(rs.getDouble("calculated_ats_score"));
        a.setMatchedSkills(rs.getString("matched_skills"));
        a.setMissingSkills(rs.getString("missing_skills"));
        a.setStatus(rs.getString("status"));
        a.setInterviewDate(rs.getString("interview_date"));
        a.setInterviewLink(rs.getString("interview_link"));
        a.setAppliedAt(rs.getTimestamp("applied_at"));

        if (isRecruiterView) {
            try {
                a.setApplicantName(rs.getString("full_name"));
                a.setApplicantEmail(rs.getString("email"));
                a.setApplicantMobile(rs.getString("mobile"));
                a.setApplicantExpYears(rs.getInt("candidate_exp_years"));
                a.setResumeFilePath(rs.getString("resume_file_path"));
            } catch (SQLException ignored) {}
        }

        try {
            a.setJobTitle(rs.getString("job_title"));
            a.setJobRole(rs.getString("job_role"));
            a.setCompanyName(rs.getString("company_name"));
            a.setSalary(rs.getDouble("salary"));
            a.setWorkMode(rs.getString("work_mode"));
        } catch (SQLException ignored) {}

        return a;
    }
}
