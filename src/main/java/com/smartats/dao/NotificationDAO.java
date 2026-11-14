package com.smartats.dao;

import com.smartats.model.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public boolean createNotification(Notification notif) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, title, message, is_read) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, notif.getUserId());
            ps.setString(2, notif.getTitle());
            ps.setString(3, notif.getMessage());
            ps.setBoolean(4, notif.isRead());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        notif.setNotifId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public List<Notification> getRecentNotifications(int userId, int limit) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notification n = new Notification();
                    n.setNotifId(rs.getInt("notif_id"));
                    n.setUserId(rs.getInt("user_id"));
                    n.setTitle(rs.getString("title"));
                    n.setMessage(rs.getString("message"));
                    n.setRead(rs.getBoolean("is_read"));
                    n.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(n);
                }
            }
        }
        return list;
    }

    public int getUnreadCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = false";
        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean markAllAsRead(int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = true WHERE user_id = ?";
        try (Connection conn = DBConnectionPool.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
