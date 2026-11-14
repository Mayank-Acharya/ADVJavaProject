package com.smartats.servlet;

import com.smartats.dao.ApplicationDAO;
import com.smartats.dao.JobDAO;
import com.smartats.model.Application;
import com.smartats.model.JobPosting;
import com.smartats.model.User;
import com.smartats.util.EmailNotificationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "CandidateActionServlet", urlPatterns = {"/candidate-action"})
public class CandidateActionServlet extends HttpServlet {

    private final ApplicationDAO appDAO = new ApplicationDAO();
    private final JobDAO jobDAO = new JobDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null || !"RECRUITER".equalsIgnoreCase(currentUser.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?msg=" + urlEncode("Unauthorized.") + "&type=error");
            return;
        }

        try {
            int appId = Integer.parseInt(req.getParameter("app_id"));
            String status = req.getParameter("status"); // 'SELECTED' or 'REJECTED'
            String interviewDate = req.getParameter("interview_date");
            String interviewLink = req.getParameter("interview_link");

            Application app = appDAO.getApplicationById(appId);
            if (app == null) {
                resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?msg=" + urlEncode("Application not found.") + "&type=error");
                return;
            }

            JobPosting job = jobDAO.getJobById(app.getJobId());
            String compName = (job != null) ? job.getCompanyName() : currentUser.getCompanyName();
            String title = (job != null) ? job.getJobTitle() : "Position";

            String notifTitle = "SELECTED".equalsIgnoreCase(status)
                    ? "Congratulations! You have been Selected by " + compName
                    : "Application Update: " + title + " at " + compName;

            String notifMsg = "SELECTED".equalsIgnoreCase(status)
                    ? "Company " + compName + " has selected your application for an interview for " + title
                    + "! Interview Date: " + (interviewDate != null ? interviewDate : "TBA")
                    + ". Check your registered email for meeting link and details."
                    : "Your application for " + title + " at " + compName + " has been reviewed and closed. We thank you for your interest.";

            boolean success = appDAO.updateSingleApplicationStatus(
                    appId,
                    status.toUpperCase(),
                    interviewDate,
                    interviewLink,
                    app.getApplicantId(),
                    notifTitle,
                    notifMsg
            );

            if (success && "SELECTED".equalsIgnoreCase(status) && app.getApplicantEmail() != null) {
                EmailNotificationUtil.sendSelectionEmailAsync(
                        app.getApplicantEmail(),
                        app.getApplicantName() != null ? app.getApplicantName() : "Candidate",
                        compName,
                        title,
                        app.getCalculatedAtsScore(),
                        interviewDate,
                        interviewLink
                );
            }

            resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?tab=candidates&jobId=" + app.getJobId()
                    + "&msg=" + urlEncode("Candidate marked as " + status + " successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?msg="
                    + urlEncode("Error updating candidate: " + e.getMessage()) + "&type=error");
        }
    }

    private String urlEncode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
