package com.smartats.servlet;

import com.smartats.dao.NotificationDAO;
import com.smartats.model.Notification;
import com.smartats.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

@WebServlet(name = "NotificationServlet", urlPatterns = {"/notifications"})
public class NotificationServlet extends HttpServlet {

    private final NotificationDAO notifDAO = new NotificationDAO();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        if (currentUser == null) {
            out.print("{\"unreadCount\":0,\"notifications\":[]}");
            return;
        }

        try {
            String action = req.getParameter("action");
            if ("unreadCount".equalsIgnoreCase(action)) {
                int count = notifDAO.getUnreadCount(currentUser.getUserId());
                out.print("{\"unreadCount\":" + count + "}");
            } else {
                // Return recent notifications
                List<Notification> list = notifDAO.getRecentNotifications(currentUser.getUserId(), 10);
                int unread = notifDAO.getUnreadCount(currentUser.getUserId());

                StringBuilder sb = new StringBuilder();
                sb.append("{\"unreadCount\":").append(unread).append(",\"notifications\":[");
                for (int i = 0; i < list.size(); i++) {
                    Notification n = list.get(i);
                    sb.append("{")
                            .append("\"notifId\":").append(n.getNotifId()).append(",")
                            .append("\"title\":\"").append(escapeJson(n.getTitle())).append("\",")
                            .append("\"message\":\"").append(escapeJson(n.getMessage())).append("\",")
                            .append("\"isRead\":").append(n.isRead()).append(",")
                            .append("\"date\":\"").append(n.getCreatedAt() != null ? dateFormat.format(n.getCreatedAt()) : "").append("\"")
                            .append("}");
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]}");
                out.print(sb.toString());
            }
        } catch (Exception e) {
            out.print("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        if (currentUser == null) {
            out.print("{\"success\":false}");
            return;
        }

        try {
            boolean success = notifDAO.markAllAsRead(currentUser.getUserId());
            out.print("{\"success\":" + success + "}");
        } catch (Exception e) {
            out.print("{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
