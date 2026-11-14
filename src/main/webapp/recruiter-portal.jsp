<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartats.model.User" %>
<%@ page import="com.smartats.model.JobPosting" %>
<%@ page import="com.smartats.model.Application" %>
<%@ page import="com.smartats.dao.JobDAO" %>
<%@ page import="com.smartats.dao.ApplicationDAO" %>
<%@ page import="com.smartats.engine.XMLTaxonomyParser" %>
<%@ page import="java.util.List" %>
<%
    User currentUser = (User) session.getAttribute("currentUser");
    if (currentUser == null || !"RECRUITER".equalsIgnoreCase(currentUser.getRole())) {
        response.sendRedirect("index.jsp?msg=Please+login+as+recruiter&type=error");
        return;
    }

    String msg = request.getParameter("msg");
    String type = request.getParameter("type");
    String activeTab = request.getParameter("tab");
    if (activeTab == null) activeTab = "post";

    String selectedJobIdStr = request.getParameter("jobId");
    int selectedJobId = 0;
    if (selectedJobIdStr != null && !selectedJobIdStr.trim().isEmpty()) {
        try { selectedJobId = Integer.parseInt(selectedJobIdStr.trim()); } catch (Exception ignored) {}
    }

    JobDAO jobDAO = new JobDAO();
    ApplicationDAO appDAO = new ApplicationDAO();
    XMLTaxonomyParser taxonomy = XMLTaxonomyParser.getInstance();

    List<JobPosting> myJobs = jobDAO.getJobsByRecruiter(currentUser.getUserId());
    List<Application> rankedApplicants = null;
    JobPosting activeJob = null;

    if (selectedJobId > 0) {
        activeJob = jobDAO.getJobById(selectedJobId);
        if (activeJob != null && activeJob.getRecruiterId() == currentUser.getUserId()) {
            rankedApplicants = appDAO.getApplicationsByJob(selectedJobId);
        }
    } else if (!myJobs.isEmpty()) {
        activeJob = myJobs.get(0);
        selectedJobId = activeJob.getJobId();
        rankedApplicants = appDAO.getApplicationsByJob(selectedJobId);
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= currentUser.getCompanyName() != null ? currentUser.getCompanyName() : "Company" %> — Recruiter Portal</title>
    <link rel="stylesheet" href="assets/css/style.css">
</head>
<body>

    <!-- Header Navigation -->
    <header class="app-header">
        <div class="header-inner">
            <a href="recruiter-portal.jsp" class="brand-logo">
                <span>🎯 SmartATS</span>
                <span class="brand-badge">Recruiter Portal</span>
            </a>

            <div class="header-right">
                <!-- Notification Bell -->
                <div class="notification-wrapper">
                    <button id="bellBtn" class="bell-btn" title="View Notifications">
                        🔔
                        <span id="bellBadge" class="bell-badge">0</span>
                    </button>
                    <div id="bellDropdown" class="bell-dropdown">
                        <div class="bell-dropdown-header">
                            <h4>Notifications</h4>
                            <button id="markReadBtn">Mark read</button>
                        </div>
                        <div id="notifList" class="bell-dropdown-list">
                            <!-- Populated via AJAX notifications.js -->
                        </div>
                    </div>
                </div>

                <div class="user-badge">
                    <span class="user-name"><%= currentUser.getFullName() %></span>
                    <span class="user-role"><%= currentUser.getCompanyName() != null ? currentUser.getCompanyName() : "Recruiter" %></span>
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
            <button class="tab-btn <%= "post".equals(activeTab) ? "active" : "" %>" onclick="switchTab('post')">
                ✍️ Post Job (Sentence Builder)
            </button>
            <button class="tab-btn <%= "jobs".equals(activeTab) ? "active" : "" %>" onclick="switchTab('jobs')">
                📋 Active Job Postings (<%= myJobs.size() %>)
            </button>
            <button class="tab-btn <%= "candidates".equals(activeTab) ? "active" : "" %>" onclick="switchTab('candidates')">
                👥 Ranked Candidates & Top-$K$ Selection
            </button>
        </div>

        <!-- ============================================================== -->
        <!-- TAB 1: THE FILL-IN-THE-BLANK SENTENCE JOB BUILDER              -->
        <!-- ============================================================== -->
        <div id="tabContent-post" style="<%= "post".equals(activeTab) ? "" : "display:none;" %>">
            <div class="sentence-builder-card">
                <div style="margin-bottom: 20px;">
                    <h2 style="font-size: 22px; font-weight: 700; color: #1e3a8a;">
                        Natural Sentence Job Builder
                    </h2>
                    <p style="font-size: 14px; color: var(--text-muted);">
                        Fill in the blanks to define your job requirements, ATS cutoff threshold, and shortlist limit $K$.
                    </p>
                </div>

                <form action="post-job" method="POST">
                    <div style="margin-bottom: 20px;">
                        <label style="font-size: 13px; font-weight: 600; color: #475569;">Job Title / Reference Identifier:</label>
                        <input type="text" name="job_title" class="form-control" style="max-width: 400px; margin-top: 5px;"
                               placeholder="e.g. Job Alpha (Full-Stack Engineer)" required>
                    </div>

                    <div class="sentence-builder">
                        "I am looking for a
                        <select name="job_role" id="roleSelect" class="sentence-select" onchange="updateDefaultSkills(this.value)">
                            <option value="Web Developer" selected>Web Developer</option>
                            <option value="App Developer">App Developer</option>
                            <option value="Video Editor">Video Editor</option>
                            <option value="Game Developer">Game Developer</option>
                            <option value="Data Scientist">Data Scientist</option>
                        </select>
                        with core and custom skills in
                        <input type="text" name="custom_skills" id="customSkillsInput" class="sentence-input wide"
                               value="Java, JSP, Servlet, JDBC, SQL, XML, HTML, CSS" placeholder="Java, SQL, React...">
                        offering an annual salary of ₹
                        <input type="number" name="salary" class="sentence-input medium" value="850000" step="10000" required>
                        for candidates with at least
                        <input type="number" name="min_exp_years" class="sentence-input short" value="2" min="0" max="25" required>
                        years of experience, willing to work
                        <select name="work_mode" class="sentence-select">
                            <option value="REMOTE" selected>Remote</option>
                            <option value="ONSITE">On-Site</option>
                            <option value="HYBRID">Hybrid</option>
                        </select>
                        with a minimum ATS qualifying score of
                        <input type="number" name="min_ats_threshold" class="sentence-input short" value="65" min="10" max="100" step="1" required>%
                        to auto-shortlist the top
                        <input type="number" name="vacancy_limit_k" class="sentence-input short" value="2" min="1" max="50" required>
                        candidates."
                    </div>

                    <div style="margin-top: 25px; display: flex; justify-content: flex-end;">
                        <button type="submit" class="btn btn-primary" style="padding: 12px 28px; font-size: 15px;">
                            🚀 Publish Job Alpha
                        </button>
                    </div>
                </form>
            </div>
        </div>

        <!-- ============================================================== -->
        <!-- TAB 2: ACTIVE JOB POSTINGS TABLE                               -->
        <!-- ============================================================== -->
        <div id="tabContent-jobs" style="<%= "jobs".equals(activeTab) ? "" : "display:none;" %>">
            <div class="card">
                <div class="card-title">
                    <span>📋 Posted Jobs by <%= currentUser.getCompanyName() %></span>
                </div>

                <% if (myJobs.isEmpty()) { %>
                    <p style="color: var(--text-muted); font-size: 14px; text-align: center; padding: 30px;">
                        No jobs posted yet. Use the <b>Sentence Builder</b> tab to publish your first job.
                    </p>
                <% } else { %>
                    <div class="table-responsive">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Job Title</th>
                                    <th>Role Taxonomy</th>
                                    <th>Salary</th>
                                    <th>Min Exp</th>
                                    <th>Mode</th>
                                    <th>Cutoff ATS</th>
                                    <th>Limit K</th>
                                    <th>Applicants</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (JobPosting j : myJobs) { %>
                                    <tr>
                                        <td><b><%= j.getJobTitle() %></b></td>
                                        <td><%= j.getJobRole() %></td>
                                        <td>₹<%= String.format("%,.0f", j.getSalary()) %></td>
                                        <td><%= j.getMinExpYears() %> yrs</td>
                                        <td><%= j.getWorkMode() %></td>
                                        <td><span class="score-badge medium"><%= j.getMinAtsThreshold() %>%</span></td>
                                        <td><b><%= j.getVacancyLimitK() %></b></td>
                                        <td><span style="font-weight: 700; color: var(--primary);"><%= j.getApplicantCount() %></span></td>
                                        <td>
                                            <span class="status-pill <%= j.getStatus().toLowerCase() %>">
                                                <%= j.getStatus() %>
                                            </span>
                                        </td>
                                        <td>
                                            <a href="recruiter-portal.jsp?tab=candidates&jobId=<%= j.getJobId() %>" class="btn btn-primary btn-sm">
                                                Rank Candidates →
                                            </a>
                                        </td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                <% } %>
            </div>
        </div>

        <!-- ============================================================== -->
        <!-- TAB 3: RANKED CANDIDATE POOL & TOP-K SHORTLISTING              -->
        <!-- ============================================================== -->
        <div id="tabContent-candidates" style="<%= "candidates".equals(activeTab) ? "" : "display:none;" %>">
            <% if (myJobs.isEmpty()) { %>
                <div class="card" style="text-align: center; padding: 40px;">
                    <p style="color: var(--text-muted);">Please create a job first to view ranked applicants.</p>
                </div>
            <% } else { %>
                <!-- Job Selector Dropdown -->
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <div>
                        <label style="font-size: 13px; font-weight: 600; color: #475569;">Select Job to Inspect Candidate Rankings:</label>
                        <select onchange="window.location.href='recruiter-portal.jsp?tab=candidates&jobId=' + this.value"
                                class="form-control" style="width: 320px; margin-top: 4px; font-weight: 600;">
                            <% for (JobPosting j : myJobs) { %>
                                <option value="<%= j.getJobId() %>" <%= (activeJob != null && activeJob.getJobId() == j.getJobId()) ? "selected" : "" %>>
                                    <%= j.getJobTitle() %> (<%= j.getApplicantCount() %> applicants)
                                </option>
                            <% } %>
                        </select>
                    </div>

                    <% if (activeJob != null) { %>
                        <div style="font-size: 13px; color: var(--text-muted);">
                            <span>Threshold: <b><%= activeJob.getMinAtsThreshold() %>%</b></span> |
                            <span>Shortlist Limit <b>K = <%= activeJob.getVacancyLimitK() %></b></span>
                        </div>
                    <% } %>
                </div>

                <!-- TOP-K AUTO-SHORTLIST BANNER -->
                <% if (activeJob != null && rankedApplicants != null && !rankedApplicants.isEmpty()) { %>
                    <div class="top-k-banner">
                        <div>
                            <h3>⚡ Top-$K$ Automated Selection & Rejection Engine</h3>
                            <p>Uses Java Max-Heap (<code>PriorityQueue</code>) to short-list the top $K$ candidates and auto-reject remaining applications atomically.</p>
                        </div>

                        <form action="auto-shortlist" method="POST" style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
                            <input type="hidden" name="job_id" value="<%= activeJob.getJobId() %>">
                            
                            <div class="top-k-controls">
                                <label style="font-size: 13px; font-weight: 700;">Value of K:</label>
                                <input type="number" name="k_limit" value="<%= activeJob.getVacancyLimitK() %>" min="1" max="<%= rankedApplicants.size() %>" required>
                            </div>

                            <input type="text" name="interview_date" placeholder="📅 Date: e.g. 15th Nov, 10:00 AM" class="form-control" style="width: 220px;" required>
                            <input type="text" name="interview_link" placeholder="🔗 Google Meet / Venue Link" class="form-control" style="width: 200px;">

                            <button type="submit" class="btn btn-success" onclick="return confirm('Execute Top-K selection? Top candidates will be marked SELECTED and receive email invites; remainder will be marked REJECTED.');">
                                Auto-Shortlist Top $K$
                            </button>
                        </form>
                    </div>
                <% } %>

                <!-- Candidates Table -->
                <div class="card">
                    <div class="card-title">
                        <span>👥 Candidate Pool for <%= activeJob != null ? activeJob.getJobTitle() : "Job" %></span>
                        <% if (rankedApplicants != null) { %>
                            <span style="font-size: 13px; font-weight: 400; color: var(--text-muted);">
                                (<%= rankedApplicants.size() %> total applicants, ordered by Max-Heap ATS Score)
                            </span>
                        <% } %>
                    </div>

                    <% if (rankedApplicants == null || rankedApplicants.isEmpty()) { %>
                        <p style="text-align: center; color: var(--text-muted); padding: 30px;">
                            No candidates have applied for this job yet. Resumes uploaded by applicants matching this job's criteria will appear here.
                        </p>
                    <% } else { %>
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Rank</th>
                                        <th>Candidate</th>
                                        <th>Contact</th>
                                        <th>Exp</th>
                                        <th>ATS Match Score</th>
                                        <th>Skill Breakdown</th>
                                        <th>Status</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <%
                                        int rank = 1;
                                        for (Application app : rankedApplicants) {
                                            double score = app.getCalculatedAtsScore();
                                            String scoreClass = score >= 80 ? "high" : (score >= 60 ? "medium" : "low");
                                    %>
                                        <tr>
                                            <td>
                                                <span style="font-weight: 700; font-size: 15px; color: <%= rank <= activeJob.getVacancyLimitK() ? "var(--primary)" : "var(--text-muted)" %>;">
                                                    #<%= rank %>
                                                </span>
                                            </td>
                                            <td>
                                                <b><%= app.getApplicantName() %></b>
                                            </td>
                                            <td>
                                                <div style="font-size: 13px;"><%= app.getApplicantEmail() %></div>
                                                <div style="font-size: 11px; color: var(--text-muted);"><%= app.getApplicantMobile() %></div>
                                            </td>
                                            <td><%= app.getApplicantExpYears() %> yrs</td>
                                            <td>
                                                <div class="progress-bar-container">
                                                    <div class="progress-bar-fill <%= scoreClass %>" style="width: <%= score %>%;"></div>
                                                </div>
                                                <span class="score-badge <%= scoreClass %>">
                                                    <%= score %>%
                                                </span>
                                            </td>
                                            <td style="max-width: 250px;">
                                                <div>
                                                    <span style="font-size: 10px; font-weight: 700; color: #166534;">MATCHED:</span>
                                                    <%
                                                        if (app.getMatchedSkills() != null && !app.getMatchedSkills().isEmpty()) {
                                                            for (String sk : app.getMatchedSkills().split(",")) {
                                                    %>
                                                        <span class="skill-chip matched"><%= sk.trim() %></span>
                                                    <%      }
                                                        } else { %>
                                                        <span style="font-size: 11px; color: #94a3b8;">None</span>
                                                    <% } %>
                                                </div>
                                                <div style="margin-top: 4px;">
                                                    <span style="font-size: 10px; font-weight: 700; color: #991b1b;">MISSING:</span>
                                                    <%
                                                        if (app.getMissingSkills() != null && !app.getMissingSkills().isEmpty()) {
                                                            for (String sk : app.getMissingSkills().split(",")) {
                                                    %>
                                                        <span class="skill-chip missing"><%= sk.trim() %></span>
                                                    <%      }
                                                        } else { %>
                                                        <span style="font-size: 11px; color: #166534;">Full Match!</span>
                                                    <% } %>
                                                </div>
                                            </td>
                                            <td>
                                                <span class="status-pill <%= app.getStatus().toLowerCase() %>">
                                                    <%= app.getStatus() %>
                                                </span>
                                                <% if (app.getInterviewDate() != null && !app.getInterviewDate().isEmpty()) { %>
                                                    <div style="font-size: 10px; color: #0f766e; margin-top: 3px;">
                                                        📅 <%= app.getInterviewDate() %>
                                                    </div>
                                                <% } %>
                                            </td>
                                            <td>
                                                <% if (!"SELECTED".equalsIgnoreCase(app.getStatus())) { %>
                                                    <form action="candidate-action" method="POST" style="display: inline-block;">
                                                        <input type="hidden" name="app_id" value="<%= app.getAppId() %>">
                                                        <input type="hidden" name="status" value="SELECTED">
                                                        <input type="hidden" name="interview_date" value="Upcoming Round 1">
                                                        <input type="hidden" name="interview_link" value="https://meet.google.com/smart-ats-interview">
                                                        <button type="submit" class="btn btn-success btn-sm" title="Shortlist individually">
                                                            ✓ Select
                                                        </button>
                                                    </form>
                                                <% } %>
                                                <% if (!"REJECTED".equalsIgnoreCase(app.getStatus())) { %>
                                                    <form action="candidate-action" method="POST" style="display: inline-block;">
                                                        <input type="hidden" name="app_id" value="<%= app.getAppId() %>">
                                                        <input type="hidden" name="status" value="REJECTED">
                                                        <button type="submit" class="btn btn-outline btn-sm" style="color: var(--danger);" title="Reject individually">
                                                            ✕ Reject
                                                        </button>
                                                    </form>
                                                <% } %>
                                            </td>
                                        </tr>
                                    <%
                                            rank++;
                                        }
                                    %>
                                </tbody>
                            </table>
                        </div>
                    <% } %>
                </div>
            <% } %>
        </div>
    </div>

    <script src="assets/js/notifications.js"></script>
    <script>
        const roleSkills = {
            'Web Developer': 'Java, JSP, Servlet, JDBC, SQL, XML, HTML, CSS, JavaScript, Tomcat',
            'App Developer': 'Android, Kotlin, Java, Flutter, React Native, REST API, SQLite, Firebase, XML',
            'Video Editor': 'Premiere Pro, After Effects, DaVinci Resolve, Final Cut Pro, Color Grading, Audio Sync',
            'Game Developer': 'Unity, Unreal Engine, C#, C++, Blender, Physics Engine, Shaders',
            'Data Scientist': 'Python, Machine Learning, Deep Learning, Pandas, NumPy, SQL, Data Visualization'
        };

        function updateDefaultSkills(role) {
            const input = document.getElementById('customSkillsInput');
            if (roleSkills[role]) {
                input.value = roleSkills[role];
            }
        }

        function switchTab(tabId) {
            document.querySelectorAll('.tabs-nav .tab-btn').forEach(btn => btn.classList.remove('active'));
            document.getElementById('tabContent-post').style.display = 'none';
            document.getElementById('tabContent-jobs').style.display = 'none';
            document.getElementById('tabContent-candidates').style.display = 'none';

            document.getElementById('tabContent-' + tabId).style.display = 'block';
            event.target.classList.add('active');
        }
    </script>
</body>
</html>
