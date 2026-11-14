package com.smartats.servlet;

import com.smartats.dao.JobDAO;
import com.smartats.model.JobPosting;
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

@WebServlet(name = "PostJobServlet", urlPatterns = {"/post-job"})
public class PostJobServlet extends HttpServlet {

    private final JobDAO jobDAO = new JobDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null || !"RECRUITER".equalsIgnoreCase(currentUser.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?msg=" + urlEncode("Unauthorized access.") + "&type=error");
            return;
        }

        try {
            String jobTitle = req.getParameter("job_title");
            String jobRole = req.getParameter("job_role");
            String customSkills = req.getParameter("custom_skills");
            String salaryStr = req.getParameter("salary");
            String minExpStr = req.getParameter("min_exp_years");
            String workMode = req.getParameter("work_mode");
            String minAtsStr = req.getParameter("min_ats_threshold");
            String vacancyKStr = req.getParameter("vacancy_limit_k");

            if (jobTitle == null || jobRole == null || salaryStr == null || minExpStr == null || vacancyKStr == null) {
                resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?msg=" + urlEncode("Please fill in all sentence parameters.") + "&type=error");
                return;
            }

            double salary = Double.parseDouble(salaryStr.trim());
            int minExp = Integer.parseInt(minExpStr.trim());
            double minAts = (minAtsStr != null && !minAtsStr.trim().isEmpty())
                    ? Double.parseDouble(minAtsStr.trim())
                    : 60.0;
            int vacancyK = Integer.parseInt(vacancyKStr.trim());
            if (vacancyK <= 0) vacancyK = 1;

            JobPosting job = new JobPosting();
            job.setRecruiterId(currentUser.getUserId());
            job.setCompanyName(currentUser.getCompanyName() != null ? currentUser.getCompanyName() : "Company");
            job.setJobTitle(jobTitle.trim());
            job.setJobRole(jobRole.trim());
            job.setCustomSkillsCsv(customSkills != null ? customSkills.trim() : "");
            job.setSalary(salary);
            job.setMinExpYears(minExp);
            job.setWorkMode(workMode != null ? workMode.trim().toUpperCase() : "REMOTE");
            job.setMinAtsThreshold(minAts);
            job.setVacancyLimitK(vacancyK);
            job.setStatus("OPEN");

            boolean success = jobDAO.createJob(job);
            if (success) {
                resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?tab=jobs&msg="
                        + urlEncode("Job '" + jobTitle + "' successfully published with Vacancy Limit K = " + vacancyK + "!"));
            } else {
                resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?msg=" + urlEncode("Failed to post job. Please try again.") + "&type=error");
            }

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?msg="
                    + urlEncode("Invalid input values: " + e.getMessage()) + "&type=error");
        }
    }

    private String urlEncode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
