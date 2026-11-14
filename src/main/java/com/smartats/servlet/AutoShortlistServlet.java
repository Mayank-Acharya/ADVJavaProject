package com.smartats.servlet;

import com.smartats.dao.ApplicationDAO;
import com.smartats.dao.JobDAO;
import com.smartats.dao.NotificationDAO;
import com.smartats.model.Application;
import com.smartats.model.JobPosting;
import com.smartats.model.Notification;
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
import java.util.List;

@WebServlet(name = "AutoShortlistServlet", urlPatterns = {"/auto-shortlist"})
public class AutoShortlistServlet extends HttpServlet {

    private final JobDAO jobDAO = new JobDAO();
    private final ApplicationDAO appDAO = new ApplicationDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null || !"RECRUITER".equalsIgnoreCase(currentUser.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?msg=" + urlEncode("Unauthorized access.") + "&type=error");
            return;
        }

        try {
            String jobIdStr = req.getParameter("job_id");
            String kLimitStr = req.getParameter("k_limit");
            String interviewDate = req.getParameter("interview_date");
            String interviewLink = req.getParameter("interview_link");

            if (jobIdStr == null || jobIdStr.trim().isEmpty()) {
                resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?msg=" + urlEncode("Invalid job specified.") + "&type=error");
                return;
            }

            int jobId = Integer.parseInt(jobIdStr.trim());
            JobPosting job = jobDAO.getJobById(jobId);
            if (job == null) {
                resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?msg=" + urlEncode("Job not found.") + "&type=error");
                return;
            }

            int kLimit = (kLimitStr != null && !kLimitStr.trim().isEmpty())
                    ? Integer.parseInt(kLimitStr.trim())
                    : job.getVacancyLimitK();

            if (kLimit <= 0) kLimit = 1;

            // Execute Top-K Selection with Max-Heap & ACID JDBC Transaction
            List<Application> selectedCandidates = appDAO.processTopKSelection(
                    jobId,
                    kLimit,
                    job.getCompanyName(),
                    job.getJobTitle(),
                    interviewDate != null ? interviewDate.trim() : "",
                    interviewLink != null ? interviewLink.trim() : ""
            );

            // Dispatch Async SMTP Email Notifications to all selected candidates
            for (Application selected : selectedCandidates) {
                if (selected.getApplicantEmail() != null && !selected.getApplicantEmail().trim().isEmpty()) {
                    EmailNotificationUtil.sendSelectionEmailAsync(
                            selected.getApplicantEmail(),
                            selected.getApplicantName() != null ? selected.getApplicantName() : "Candidate",
                            job.getCompanyName(),
                            job.getJobTitle(),
                            selected.getCalculatedAtsScore(),
                            interviewDate,
                            interviewLink
                    );
                }
            }

            // Recruiter In-App Confirmation
            notifDAO.createNotification(new Notification(
                    currentUser.getUserId(),
                    "Top-" + kLimit + " Shortlist Completed for " + job.getJobTitle(),
                    "Selected " + selectedCandidates.size() + " top candidates using the ATS Max-Heap ranking. Notifications and email invites dispatched."
            ));

            resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?tab=candidates&jobId=" + jobId
                    + "&msg=" + urlEncode("Top-" + kLimit + " candidates successfully shortlisted and notified! Email alerts sent."));

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?msg="
                    + urlEncode("Failed to execute Top-K selection: " + e.getMessage()) + "&type=error");
        }
    }

    private String urlEncode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
