package com.smartats.servlet;

import com.smartats.dao.UserDAO;
import com.smartats.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "AuthServlet", urlPatterns = {"/auth"})
public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("logout".equalsIgnoreCase(action)) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            resp.sendRedirect(req.getContextPath() + "/index.jsp?msg=" + urlEncode("Logged out successfully.") + "&type=success");
        } else {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");

        try {
            if ("login".equalsIgnoreCase(action)) {
                handleLogin(req, resp);
            } else if ("register".equalsIgnoreCase(action)) {
                handleRegister(req, resp);
            } else {
                resp.sendRedirect(req.getContextPath() + "/index.jsp");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/index.jsp?msg=" + urlEncode("An unexpected error occurred: " + e.getMessage()) + "&type=error");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String usernameOrEmail = req.getParameter("usernameOrEmail");
        String password = req.getParameter("password");
        String role = req.getParameter("role"); // RECRUITER or APPLICANT

        if (usernameOrEmail == null || password == null || role == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?msg=" + urlEncode("All fields are required.") + "&type=error");
            return;
        }

        User user = userDAO.authenticate(usernameOrEmail.trim(), password.trim(), role.trim().toUpperCase());
        if (user != null) {
            HttpSession session = req.getSession(true);
            session.setAttribute("currentUser", user);
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("role", user.getRole());
            session.setAttribute("fullName", user.getFullName());
            session.setAttribute("companyName", user.getCompanyName());

            if ("RECRUITER".equalsIgnoreCase(user.getRole())) {
                resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp");
            } else {
                resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp");
            }
        } else {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?msg=" + urlEncode("Invalid credentials or role mismatch. Please check your username/password.") + "&type=error&tab=" + role.toLowerCase());
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String mobile = req.getParameter("mobile");
        String role = req.getParameter("role");
        String companyName = req.getParameter("companyName");

        if (username == null || password == null || fullName == null || email == null || role == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?msg=" + urlEncode("Missing mandatory registration details.") + "&type=error");
            return;
        }

        username = username.trim();
        email = email.trim();
        role = role.trim().toUpperCase();

        if (userDAO.usernameOrEmailExists(username, email)) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?msg=" + urlEncode("Username or Email already registered. Please login.") + "&type=error&tab=" + role.toLowerCase());
            return;
        }

        User newUser = new User(username, password, fullName.trim(),
                ("RECRUITER".equalsIgnoreCase(role) ? (companyName != null ? companyName.trim() : "Company") : null),
                email, mobile != null ? mobile.trim() : "", role);

        boolean success = userDAO.registerUser(newUser);
        if (success) {
            // Auto login after registration
            HttpSession session = req.getSession(true);
            session.setAttribute("currentUser", newUser);
            session.setAttribute("userId", newUser.getUserId());
            session.setAttribute("role", newUser.getRole());
            session.setAttribute("fullName", newUser.getFullName());
            session.setAttribute("companyName", newUser.getCompanyName());

            if ("RECRUITER".equalsIgnoreCase(role)) {
                resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?msg=" + urlEncode("Welcome! Your recruiter profile has been created."));
            } else {
                resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?msg=" + urlEncode("Welcome! Please upload your resume to discover matching jobs."));
            }
        } else {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?msg=" + urlEncode("Failed to create account. Please try again.") + "&type=error");
        }
    }

    private String urlEncode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
