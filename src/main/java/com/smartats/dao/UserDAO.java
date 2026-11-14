package com.smartats.dao;

import com.smartats.model.User;
import com.smartats.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDAO {

    public boolean registerUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, company_name, email, mobile, role) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, PasswordUtil.hashPassword(user.getPasswordHash()));
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getCompanyName());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getMobile());
            ps.setString(7, user.getRole());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setUserId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public User authenticate(String usernameOrEmail, String password, String role) throws SQLException {
        String sql = "SELECT * FROM users WHERE (username = ? OR email = ?) AND role = ?";
        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            ps.setString(3, role);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (PasswordUtil.verifyPassword(password, storedHash)) {
                        return mapResultSetToUser(rs);
                    }
                }
            }
        }
        return null;
    }

    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public boolean updateResumeProfile(int userId, String filePath, String resumeText, int expYears) throws SQLException {
        String sql = "UPDATE users SET resume_file_path = ?, resume_text = ?, candidate_exp_years = ? WHERE user_id = ?";
        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, filePath);
            ps.setString(2, resumeText);
            ps.setInt(3, expYears);
            ps.setInt(4, userId);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean usernameOrEmailExists(String username, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setCompanyName(rs.getString("company_name"));
        u.setEmail(rs.getString("email"));
        u.setMobile(rs.getString("mobile"));
        u.setRole(rs.getString("role"));
        u.setResumeFilePath(rs.getString("resume_file_path"));
        u.setResumeText(rs.getString("resume_text"));
        u.setCandidateExpYears(rs.getInt("candidate_exp_years"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }
}
