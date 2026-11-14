package com.smartats.dao;

import com.smartats.model.JobPosting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JobDAO {

    public boolean createJob(JobPosting job) throws SQLException {
        String sql = "INSERT INTO job_postings (recruiter_id, company_name, job_title, job_role, "
                + "custom_skills_csv, salary, min_exp_years, work_mode, min_ats_threshold, vacancy_limit_k, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, job.getRecruiterId());
            ps.setString(2, job.getCompanyName());
            ps.setString(3, job.getJobTitle());
            ps.setString(4, job.getJobRole());
            ps.setString(5, job.getCustomSkillsCsv());
            ps.setDouble(6, job.getSalary());
            ps.setInt(7, job.getMinExpYears());
            ps.setString(8, job.getWorkMode());
            ps.setDouble(9, job.getMinAtsThreshold());
            ps.setInt(10, job.getVacancyLimitK());
            ps.setString(11, job.getStatus() != null ? job.getStatus() : "OPEN");

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        job.setJobId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public List<JobPosting> getJobsByRecruiter(int recruiterId) throws SQLException {
        List<JobPosting> list = new ArrayList<>();
        String sql = "SELECT j.*, "
                + "(SELECT COUNT(*) FROM applications a WHERE a.job_id = j.job_id) AS applicant_count "
                + "FROM job_postings j WHERE j.recruiter_id = ? ORDER BY j.created_at DESC";

        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, recruiterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JobPosting j = mapResultSetToJob(rs);
                    j.setApplicantCount(rs.getInt("applicant_count"));
                    list.add(j);
                }
            }
        }
        return list;
    }

    public List<JobPosting> getAllOpenJobs() throws SQLException {
        List<JobPosting> list = new ArrayList<>();
        String sql = "SELECT j.*, "
                + "(SELECT COUNT(*) FROM applications a WHERE a.job_id = j.job_id) AS applicant_count "
                + "FROM job_postings j WHERE j.status = 'OPEN' ORDER BY j.created_at DESC";

        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                JobPosting j = mapResultSetToJob(rs);
                j.setApplicantCount(rs.getInt("applicant_count"));
                list.add(j);
            }
        }
        return list;
    }

    public JobPosting getJobById(int jobId) throws SQLException {
        String sql = "SELECT j.*, "
                + "(SELECT COUNT(*) FROM applications a WHERE a.job_id = j.job_id) AS applicant_count "
                + "FROM job_postings j WHERE j.job_id = ?";

        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JobPosting j = mapResultSetToJob(rs);
                    j.setApplicantCount(rs.getInt("applicant_count"));
                    return j;
                }
            }
        }
        return null;
    }

    public boolean updateJobStatus(int jobId, String status) throws SQLException {
        String sql = "UPDATE job_postings SET status = ? WHERE job_id = ?";
        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, jobId);
            return ps.executeUpdate() > 0;
        }
    }

    private JobPosting mapResultSetToJob(ResultSet rs) throws SQLException {
        JobPosting j = new JobPosting();
        j.setJobId(rs.getInt("job_id"));
        j.setRecruiterId(rs.getInt("recruiter_id"));
        j.setCompanyName(rs.getString("company_name"));
        j.setJobTitle(rs.getString("job_title"));
        j.setJobRole(rs.getString("job_role"));
        j.setCustomSkillsCsv(rs.getString("custom_skills_csv"));
        j.setSalary(rs.getDouble("salary"));
        j.setMinExpYears(rs.getInt("min_exp_years"));
        j.setWorkMode(rs.getString("work_mode"));
        j.setMinAtsThreshold(rs.getDouble("min_ats_threshold"));
        j.setVacancyLimitK(rs.getInt("vacancy_limit_k"));
        j.setStatus(rs.getString("status"));
        j.setCreatedAt(rs.getTimestamp("created_at"));
        return j;
    }
}
