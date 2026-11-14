<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartats.model.User" %>
<%@ page import="com.smartats.model.JobPosting" %>
<%@ page import="com.smartats.model.Application" %>
<%@ page import="com.smartats.dao.UserDAO" %>
<%@ page import="com.smartats.dao.JobDAO" %>
<%@ page import="com.smartats.dao.ApplicationDAO" %>
<%@ page import="com.smartats.engine.ATSEngine" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%
    User sessionUser = (User) session.getAttribute("currentUser");
    if (sessionUser == null || !"APPLICANT".equalsIgnoreCase(sessionUser.getRole())) {
        response.sendRedirect("index.jsp?msg=Please+login+as+an+applicant&type=error");
        return;
    }

    UserDAO userDAO = new UserDAO();
    JobDAO jobDAO = new JobDAO();
    ApplicationDAO appDAO = new ApplicationDAO();

    // Reload fresh user from DB to catch resume updates
    User currentUser = userDAO.getUserById(sessionUser.getUserId());
    if (currentUser == null) currentUser = sessionUser;

    String msg = request.getParameter("msg");
    String type = request.getParameter("type");
    String activeTab = request.getParameter("tab");
    if (activeTab == null) activeTab = (currentUser.getResumeText() == null || currentUser.getResumeText().trim().isEmpty()) ? "resume" : "matched";

    List<JobPosting> allJobs = jobDAO.getAllOpenJobs();
    List<Application> myApplications = appDAO.getApplicationsByApplicant(currentUser.getUserId());

    Set<Integer> appliedJobIds = new HashSet<>();
    for (Application a : myApplications) {
        appliedJobIds.add(a.getJobId());
    }

    // Reverse-ATS Matching Evaluation:
    // Filter all jobs to find only those where candidate ATS score >= job's min cutoff
    class MatchedJobItem {
        JobPosting job;
        ATSEngine.ScoreResult scoreResult;
        boolean hasApplied;
        MatchedJobItem(JobPosting job, ATSEngine.ScoreResult scoreResult, boolean hasApplied) {
            this.job = job;
            this.scoreResult = scoreResult;
            this.hasApplied = hasApplied;
        }
    }

    List<MatchedJobItem> reverseMatchedJobs = new ArrayList<>();
    if (currentUser.getResumeText() != null && !currentUser.getResumeText().trim().isEmpty()) {
        for (JobPosting job : allJobs) {
            ATSEngine.ScoreResult res = ATSEngine.evaluateResume(
                    currentUser.getResumeText(),
                    currentUser.getCandidateExpYears(),
                    job
            );

            // Reverse-ATS Filter: Show if qualified or if already applied
            boolean alreadyApplied = appliedJobIds.contains(job.getJobId());
            if (res.getTotalAtsScore() >= job.getMinAtsThreshold() || alreadyApplied) {
                reverseMatchedJobs.add(new MatchedJobItem(job, res, alreadyApplied));
            }
        }
        // Sort matching jobs by candidate's ATS score descending
        reverseMatchedJobs.sort((m1, m2) -> Double.compare(m2.scoreResult.getTotalAtsScore(), m1.scoreResult.getTotalAtsScore()));
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= currentUser.getFullName() %> — Applicant Dashboard | SmartATS</title>
    <link rel="stylesheet" href="assets/css/style.css">
</head>
<body>

    <!-- Header Navigation -->
    <header class="app-header">
        <div class="header-inner">
            <a href="applicant-portal.jsp" class="brand-logo">
                <span>🎯 SmartATS</span>
                <span class="brand-badge">Candidate Portal</span>
            </a>

            <div class="header-right">
                <!-- Notification Bell with Real-Time AJAX Polling -->
                <div class="notification-wrapper">
                    <button id="bellBtn" class="bell-btn" title="View Notifications">
                        🔔
                        <span id="bellBadge" class="bell-badge">0</span>
                    </button>
                    <div id="bellDropdown" class="bell-dropdown">
                        <div class="bell-dropdown-header">
                            <h4>My Recruitment Alerts</h4>
                            <button id="markReadBtn">Mark read</button>
                        </div>
                        <div id="notifList" class="bell-dropdown-list">
                            <!-- Populated via AJAX notifications.js -->
                        </div>
                    </div>
                </div>

                <div class="user-badge">
                    <span class="user-name"><%= currentUser.getFullName() %></span>
                    <span class="user-role">Applicant (<%= currentUser.getCandidateExpYears() %> yrs exp)</span>
                </div>

                <a href="auth?action=logout" class="btn btn-outline btn-sm">Logout</a>
            </div>
        </div>
    </header>

    <div class="container">
        <% if (msg != null && !msg.trim().isEmpty()) { %>
            <div class="alert <%= "error".equalsIgnoreCase(type) ? "alert-error" : "alert-success" %>">
                <span><%= msg %></span>
                <span style="cursor: pointer;" onclick="this.parentElement.style.display='none';">&times;</span>
            </div>
        <% } %>

        <!-- Navigation Tabs -->
        <div class="tabs-nav">
            <button class="tab-btn <%= "resume".equals(activeTab) ? "active" : "" %>" onclick="switchTab('resume')">
                📄 Resume & Profile
            </button>
            <button class="tab-btn <%= "matched".equals(activeTab) ? "active" : "" %>" onclick="switchTab('matched')">
                ✨ Reverse-ATS Matched Jobs (<%= reverseMatchedJobs.size() %>)
            </button>
            <button class="tab-btn <%= "applications".equals(activeTab) ? "active" : "" %>" onclick="switchTab('applications')">
                📬 My Applications (<%= myApplications.size() %>)
            </button>
        </div>

        <!-- ============================================================== -->
        <!-- TAB 1: UPLOAD & PARSE RESUME                                   -->
        <!-- ============================================================== -->
        <div id="tabContent-resume" style="<%= "resume".equals(activeTab) ? "" : "display:none;" %>">
            <div class="card">
                <div class="card-title">
                    <span>📄 Upload & Analyze Resume (PDF / Text)</span>
                </div>
                <p style="font-size: 14px; color: var(--text-muted); margin-bottom: 20px;">
                    Our pure Java resume parser extracts technical skills, education, and years of experience to calculate reverse matchmaking against all live vacancies.
                </p>

                <form action="upload-resume" method="POST" enctype="multipart/form-data">
                    <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 20px;">
                        <div>
                            <label style="font-size: 13px; font-weight: 600; color: #475569;">Choose Resume File (.pdf or .txt):</label>
                            <input type="file" name="resumeFile" class="form-control" accept=".pdf,.txt,.docx" style="margin-top: 5px;">
                            
                            <div style="margin-top: 15px;">
                                <label style="font-size: 13px; font-weight: 600; color: #475569;">Or paste plain text resume directly:</label>
                                <textarea name="resume_text_direct" rows="6" class="form-control" style="margin-top: 5px; font-family: monospace; font-size: 13px;"
                                          placeholder="Paste resume content here if you don't have a PDF file..."></textarea>
                            </div>
                        </div>

                        <div>
                            <div style="background: #f8fafc; border: 1px solid var(--border-color); border-radius: 8px; padding: 16px;">
                                <label style="font-size: 13px; font-weight: 600; color: #475569;">Years of Professional Experience:</label>
                                <input type="number" name="manual_exp_years" class="form-control" style="margin-top: 5px;"
                                       value="<%= currentUser.getCandidateExpYears() %>" min="0" max="30"
                                       placeholder="Auto-detected or enter manually">
                                <span style="font-size: 11px; color: var(--text-muted); display: block; margin-top: 5px;">
                                    Our regex auto-extracts experience from PDF, but you can override it here.
                                </span>

                                <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 20px;">
                                    Upload & Run ATS Parser
                                </button>
                            </div>
                        </div>
                    </div>
                </form>

                <% if (currentUser.getResumeText() != null && !currentUser.getResumeText().trim().isEmpty()) { %>
                    <div style="margin-top: 25px; border-top: 1px solid var(--border-color); padding-top: 20px;">
                        <h4 style="font-size: 14px; font-weight: 700; color: #166534; margin-bottom: 8px;">
                            ✓ Active Parsed Resume (Detected <%= currentUser.getCandidateExpYears() %> Years Experience)
                        </h4>
                        <div style="background: #f1f5f9; padding: 12px; border-radius: 8px; max-height: 140px; overflow-y: auto; font-size: 12px; color: #334155; font-family: monospace;">
                            <%= currentUser.getResumeText().length() > 600
                                    ? currentUser.getResumeText().substring(0, 600) + "..."
                                    : currentUser.getResumeText() %>
                        </div>
                    </div>
                <% } %>
            </div>
        </div>

        <!-- ============================================================== -->
        <!-- TAB 2: REVERSE-ATS MATCHED JOBS FEED                           -->
        <!-- ============================================================== -->
        <div id="tabContent-matched" style="<%= "matched".equals(activeTab) ? "" : "display:none;" %>">
            <div style="margin-bottom: 20px;">
                <h2 style="font-size: 20px; font-weight: 700; color: #1e3a8a;">
                    🎯 Reverse-ATS Matchmaking Feed
                </h2>
                <p style="font-size: 14px; color: var(--text-muted);">
                    Jobs automatically filtered for your profile where your calculated <b>ATS Score &ge; Job Cutoff Threshold</b>.
                </p>
            </div>

            <% if (currentUser.getResumeText() == null || currentUser.getResumeText().trim().isEmpty()) { %>
                <div class="card" style="text-align: center; padding: 40px;">
                    <p style="color: var(--text-muted); font-size: 15px; margin-bottom: 15px;">
                        No resume uploaded yet. Please upload your resume in the <b>Resume & Profile</b> tab to activate reverse matchmaking!
                    </p>
                    <button class="btn btn-primary" onclick="switchTab('resume')">Upload Resume Now</button>
                </div>
            <% } else if (reverseMatchedJobs.isEmpty()) { %>
                <div class="card" style="text-align: center; padding: 40px;">
                    <p style="color: var(--text-muted); font-size: 15px;">
                        No open jobs currently match your resume's minimum ATS score threshold. Check back soon as new recruiters post openings!
                    </p>
                </div>
            <% } else { %>
                <div class="job-grid">
                    <% for (MatchedJobItem item : reverseMatchedJobs) {
                        JobPosting j = item.job;
                        ATSEngine.ScoreResult sr = item.scoreResult;
                        double score = sr.getTotalAtsScore();
                        String scoreClass = score >= 80 ? "high" : (score >= 60 ? "medium" : "low");
                    %>
                        <div class="job-card">
                            <div>
                                <div class="job-card-header">
                                    <div>
                                        <div class="job-card-title"><%= j.getJobTitle() %></div>
                                        <div class="job-card-company"><%= j.getCompanyName() %> • <%= j.getJobRole() %></div>
                                    </div>
                                    <span class="score-badge <%= scoreClass %>">
                                        <%= score %>% ATS
                                    </span>
                                </div>

                                <div class="job-card-meta">
                                    <span>💰 ₹<%= String.format("%,.0f", j.getSalary()) %></span>
                                    <span>📍 <%= j.getWorkMode() %></span>
                                    <span>⏳ Req: <%= j.getMinExpYears() %> yrs</span>
                                </div>

                                <!-- Progress bar -->
                                <div style="margin-bottom: 12px;">
                                    <div class="progress-bar-container" style="width: 100%;">
                                        <div class="progress-bar-fill <%= scoreClass %>" style="width: <%= score %>%;"></div>
                                    </div>
                                    <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--text-muted); margin-top: 4px;">
                                        <span>Keywords: <%= sr.getKeywordScore() %>/70</span>
                                        <span>Experience: <%= sr.getExperienceScore() %>/30</span>
                                        <span>Cutoff: <%= j.getMinAtsThreshold() %>%</span>
                                    </div>
                                </div>

                                <!-- Matched Skills -->
                                <div style="margin-bottom: 8px;">
                                    <span style="font-size: 11px; font-weight: 700; color: #166534;">Matched Skills:</span>
                                    <div style="margin-top: 2px;">
                                        <% for (String s : sr.getMatchedSkills()) { %>
                                            <span class="skill-chip matched"><%= s %></span>
                                        <% } %>
                                    </div>
                                </div>

                                <!-- Missing Skills -->
                                <% if (!sr.getMissingSkills().isEmpty()) { %>
                                    <div style="margin-bottom: 14px;">
                                        <span style="font-size: 11px; font-weight: 700; color: #991b1b;">Missing Skills:</span>
                                        <div style="margin-top: 2px;">
                                            <% for (String s : sr.getMissingSkills()) { %>
                                                <span class="skill-chip missing"><%= s %></span>
                                            <% } %>
                                        </div>
                                    </div>
                                <% } %>
                            </div>

                            <div>
                                <% if (item.hasApplied) { %>
                                    <button class="btn btn-outline" style="width: 100%; cursor: default; color: #166534; border-color: #bbf7d0; background: #f0fdf4;" disabled>
                                        ✓ Application Submitted
                                    </button>
                                <% } else { %>
                                    <form action="apply" method="POST">
                                        <input type="hidden" name="job_id" value="<%= j.getJobId() %>">
                                        <button type="submit" class="btn btn-primary" style="width: 100%;">
                                            Apply Now with Resume
                                        </button>
                                    </form>
                                <% } %>
                            </div>
                        </div>
                    <% } %>
                </div>
            <% } %>
        </div>

        <!-- ============================================================== -->
        <!-- TAB 3: MY APPLICATIONS & STATUS TRACKING                       -->
        <!-- ============================================================== -->
        <div id="tabContent-applications" style="<%= "applications".equals(activeTab) ? "" : "display:none;" %>">
            <div class="card">
                <div class="card-title">
                    <span>📬 My Applied Jobs & Matchmaking Status</span>
                </div>

                <% if (myApplications.isEmpty()) { %>
                    <p style="text-align: center; color: var(--text-muted); padding: 30px;">
                        You have not applied to any jobs yet. Check the <b>Reverse-ATS Matched Jobs</b> tab to find matching openings.
                    </p>
                <% } else { %>
                    <div class="table-responsive">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Job Title</th>
                                    <th>Company</th>
                                    <th>Salary</th>
                                    <th>ATS Score</th>
                                    <th>Status</th>
                                    <th>Interview Details / Action</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (Application app : myApplications) {
                                    double score = app.getCalculatedAtsScore();
                                    String scoreClass = score >= 80 ? "high" : (score >= 60 ? "medium" : "low");
                                %>
                                    <tr>
                                        <td><b><%= app.getJobTitle() %></b></td>
                                        <td><%= app.getCompanyName() %></td>
                                        <td>₹<%= String.format("%,.0f", app.getSalary()) %></td>
                                        <td>
                                            <div class="progress-bar-container">
                                                <div class="progress-bar-fill <%= scoreClass %>" style="width: <%= score %>%;"></div>
                                            </div>
                                            <span class="score-badge <%= scoreClass %>"><%= score %>%</span>
                                        </td>
                                        <td>
                                            <span class="status-pill <%= app.getStatus().toLowerCase() %>">
                                                <%= app.getStatus() %>
                                            </span>
                                        </td>
                                        <td>
                                            <% if ("SELECTED".equalsIgnoreCase(app.getStatus())) { %>
                                                <div style="background: #f0fdf4; border: 1px solid #bbf7d0; padding: 8px 12px; border-radius: 8px;">
                                                    <div style="color: #166534; font-weight: 700; font-size: 13px;">
                                                        🎉 Shortlisted for Interview!
                                                    </div>
                                                    <div style="font-size: 12px; color: #334155; margin-top: 3px;">
                                                        <b>Date:</b> <%= app.getInterviewDate() != null ? app.getInterviewDate() : "TBA" %>
                                                    </div>
                                                    <% if (app.getInterviewLink() != null && !app.getInterviewLink().trim().isEmpty()) { %>
                                                        <div style="font-size: 12px; margin-top: 2px;">
                                                            <b>Link:</b> <a href="<%= app.getInterviewLink() %>" target="_blank" style="color: var(--primary); text-decoration: underline;">Join Interview</a>
                                                        </div>
                                                    <% } %>
                                                </div>
                                            <% } else if ("REJECTED".equalsIgnoreCase(app.getStatus())) { %>
                                                <span style="font-size: 12px; color: #94a3b8;">
                                                    Not shortlisted in Top-K round.
                                                </span>
                                            <% } else { %>
                                                <span style="font-size: 12px; color: #0284c7;">
                                                    Under Recruiter Review (Pending Top-K selection)
                                                </span>
                                            <% } %>
                                        </td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                <% } %>
            </div>
        </div>
    </div>

    <script src="assets/js/notifications.js"></script>
    <script>
        function switchTab(tabId) {
            document.querySelectorAll('.tabs-nav .tab-btn').forEach(btn => btn.classList.remove('active'));
            document.getElementById('tabContent-resume').style.display = 'none';
            document.getElementById('tabContent-matched').style.display = 'none';
            document.getElementById('tabContent-applications').style.display = 'none';

            document.getElementById('tabContent-' + tabId).style.display = 'block';
            event.target.classList.add('active');
        }
    </script>
</body>
</html>
