package com.smartats.servlet;

import com.smartats.dao.ApplicationDAO;
import com.smartats.dao.JobDAO;
import com.smartats.dao.NotificationDAO;
import com.smartats.dao.UserDAO;
import com.smartats.engine.ATSEngine;
import com.smartats.model.Application;
import com.smartats.model.JobPosting;
import com.smartats.model.Notification;
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

@WebServlet(name = "ApplyJobServlet", urlPatterns = {"/apply"})
public class ApplyJobServlet extends HttpServlet {

    private final JobDAO jobDAO = new JobDAO();
    private final ApplicationDAO appDAO = new ApplicationDAO();
    private final UserDAO userDAO = new UserDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null || !"APPLICANT".equalsIgnoreCase(currentUser.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?msg=" + urlEncode("Please login as an applicant.") + "&type=error");
            return;
        }

        try {
            String jobIdStr = req.getParameter("job_id");
            if (jobIdStr == null || jobIdStr.trim().isEmpty()) {
                resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?msg=" + urlEncode("Invalid job selected.") + "&type=error");
                return;
            }

            int jobId = Integer.parseInt(jobIdStr.trim());
            JobPosting job = jobDAO.getJobById(jobId);
            if (job == null) {
                resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?msg=" + urlEncode("Job not found.") + "&type=error");
                return;
            }

            // Reload user from DB to ensure freshest resume data
            User freshUser = userDAO.getUserById(currentUser.getUserId());
            if (freshUser == null || freshUser.getResumeText() == null || freshUser.getResumeText().trim().isEmpty()) {
                resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?msg="
                        + urlEncode("Please upload your resume before applying to jobs.") + "&type=error");
                return;
            }

            // Check if already applied
            if (appDAO.hasApplied(jobId, freshUser.getUserId())) {
                resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?tab=applications&msg="
                        + urlEncode("You have already submitted an application for " + job.getJobTitle() + "."));
                return;
            }

            // Evaluate ATS score for this specific job
            ATSEngine.ScoreResult result = ATSEngine.evaluateResume(
                    freshUser.getResumeText(),
                    freshUser.getCandidateExpYears(),
                    job
            );

            Application app = new Application();
            app.setJobId(jobId);
            app.setApplicantId(freshUser.getUserId());
            app.setCalculatedAtsScore(result.getTotalAtsScore());
            app.setMatchedSkills(result.getMatchedSkillsCsv());
            app.setMissingSkills(result.getMissingSkillsCsv());
            app.setStatus("APPLIED");

            boolean success = appDAO.submitApplication(app);
            if (success) {
                // Notify applicant
                notifDAO.createNotification(new Notification(
                        freshUser.getUserId(),
                        "Application Submitted: " + job.getJobTitle(),
                        "Your application for " + job.getJobTitle() + " at " + job.getCompanyName()
                                + " was submitted with an ATS match score of " + result.getTotalAtsScore() + "%."
                ));

                // Notify recruiter
                notifDAO.createNotification(new Notification(
                        job.getRecruiterId(),
                        "New Applicant for " + job.getJobTitle(),
                        freshUser.getFullName() + " applied for " + job.getJobTitle()
                                + " (ATS Score: " + result.getTotalAtsScore() + "%)."
                ));

                resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?tab=applications&msg="
                        + urlEncode("Application submitted! Your ATS Score for this role is " + result.getTotalAtsScore() + "%."));
            } else {
                resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?msg=" + urlEncode("Could not submit application.") + "&type=error");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?msg="
                    + urlEncode("Error processing application: " + e.getMessage()) + "&type=error");
        }
    }

    private String urlEncode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
