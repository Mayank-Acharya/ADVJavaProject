<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String msg = request.getParameter("msg");
    String type = request.getParameter("type");
    String tab = request.getParameter("tab");
    if (tab == null) tab = "applicant";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SmartATS - Automated Resume Parser & ATS Matchmaking Engine</title>
    <link rel="stylesheet" href="assets/css/style.css">
    <style>
        .auth-container {
            max-width: 900px;
            margin: 40px auto;
            background: white;
            border-radius: 16px;
            border: 1px solid var(--border-color);
            box-shadow: var(--shadow-lg);
            overflow: hidden;
            display: grid;
            grid-template-columns: 1fr 1fr;
        }
        .auth-banner {
            background: linear-gradient(135deg, #1e3a8a, #2563eb);
            color: white;
            padding: 40px 30px;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
        }
        .auth-banner h2 { font-size: 26px; margin-bottom: 12px; }
        .auth-banner p { font-size: 14px; opacity: 0.9; line-height: 1.6; }
        .feature-item {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-top: 15px;
            font-size: 13px;
        }
        .feature-icon {
            background: rgba(255,255,255,0.2);
            width: 28px;
            height: 28px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
        }
        .auth-form-side {
            padding: 35px 30px;
        }
        .role-switch {
            display: flex;
            background: #f1f5f9;
            padding: 4px;
            border-radius: 10px;
            margin-bottom: 25px;
        }
        .role-switch button {
            flex: 1;
            padding: 8px 12px;
            border: none;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            background: transparent;
            cursor: pointer;
            transition: all 0.2s;
            color: var(--text-muted);
        }
        .role-switch button.active {
            background: white;
            color: var(--primary);
            box-shadow: var(--shadow-sm);
        }
        .form-group {
            margin-bottom: 16px;
        }
        .form-group label {
            display: block;
            font-size: 12px;
            font-weight: 600;
            color: #475569;
            margin-bottom: 5px;
        }
        .form-control {
            width: 100%;
            padding: 10px 14px;
            border: 1px solid var(--border-color);
            border-radius: 8px;
            font-size: 14px;
            outline: none;
            transition: border 0.2s;
        }
        .form-control:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(37,99,235,0.1);
        }
        .toggle-link {
            font-size: 12px;
            color: var(--primary);
            text-decoration: none;
            cursor: pointer;
            display: inline-block;
            margin-top: 12px;
        }
        .toggle-link:hover { text-decoration: underline; }
    </style>
</head>
<body>

    <header class="app-header">
        <div class="header-inner">
            <a href="index.jsp" class="brand-logo">
                <span>🎯 SmartATS</span>
                <span class="brand-badge">Pure Java EE & XML</span>
            </a>
            <div class="header-right">
                <span style="font-size: 13px; color: var(--text-muted);">College Project Viva Edition</span>
            </div>
        </div>
    </header>

    <div class="container">
        <% if (msg != null && !msg.trim().isEmpty()) { %>
            <div class="alert <%= "success".equalsIgnoreCase(type) ? "alert-success" : "alert-error" %>">
                <span><%= msg %></span>
                <span style="cursor: pointer;" onclick="this.parentElement.style.display='none';">&times;</span>
            </div>
        <% } %>

        <div class="auth-container">
            <!-- Left Info Banner -->
            <div class="auth-banner">
                <div>
                    <h2>Intelligent ATS Matchmaking & Top-K Engine</h2>
                    <p>Engineered completely with pure Java <code>PriorityQueue</code> Max-Heap algorithms, JDBC Transactions, XML Skill Taxonomy, and Asynchronous Gmail SMTP Alerts.</p>
                    
                    <div style="margin-top: 30px;">
                        <div class="feature-item">
                            <span class="feature-icon">✓</span>
                            <span>Reverse-ATS: Resumes matched to jobs in real-time</span>
                        </div>
                        <div class="feature-item">
                            <span class="feature-icon">✓</span>
                            <span>70% Keyword + 30% Experience Weighted Scoring</span>
                        </div>
                        <div class="feature-item">
                            <span class="feature-icon">✓</span>
                            <span>Top-$K$ Auto-Shortlist with Atomic JDBC Transactions</span>
                        </div>
                        <div class="feature-item">
                            <span class="feature-icon">✓</span>
                            <span>Live In-App Notification Bell & Async Email Alerts</span>
                        </div>
                    </div>
                </div>

                <div style="font-size: 11px; opacity: 0.7; margin-top: 25px;">
                    Powered by Java Servlets, JSP 3.1, Apache PDFBox, and MySQL 8.x
                </div>
            </div>

            <!-- Right Form Panel -->
            <div class="auth-form-side">
                <!-- Role Selector Tabs -->
                <div class="role-switch">
                    <button type="button" id="tabApplicant" class="<%= !"recruiter".equalsIgnoreCase(tab) ? "active" : "" %>" onclick="selectRole('APPLICANT')">Applicant Portal</button>
                    <button type="button" id="tabRecruiter" class="<%= "recruiter".equalsIgnoreCase(tab) ? "active" : "" %>" onclick="selectRole('RECRUITER')">Recruiter Portal</button>
                </div>

                <!-- LOGIN FORM -->
                <form id="loginForm" action="auth" method="POST">
                    <input type="hidden" name="action" value="login">
                    <input type="hidden" id="loginRole" name="role" value="<%= "recruiter".equalsIgnoreCase(tab) ? "RECRUITER" : "APPLICANT" %>">

                    <h3 id="formTitle" style="font-size: 20px; font-weight: 700; margin-bottom: 6px;">
                        <%= "recruiter".equalsIgnoreCase(tab) ? "Recruiter Login" : "Applicant Login" %>
                    </h3>
                    <p style="font-size: 13px; color: var(--text-muted); margin-bottom: 20px;">
                        Enter your credentials to access your dashboard.
                    </p>

                    <div class="form-group">
                        <label>Username or Email</label>
                        <input type="text" name="usernameOrEmail" class="form-control" placeholder="e.g. john@example.com" required>
                    </div>

                    <div class="form-group">
                        <label>Password</label>
                        <input type="password" name="password" class="form-control" placeholder="••••••••" required>
                    </div>

                    <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 10px;">
                        Sign In
                    </button>

                    <div style="text-align: center; margin-top: 15px;">
                        <span class="toggle-link" onclick="toggleForm('register')">Don't have an account? Sign up here</span>
                    </div>
                </form>

                <!-- REGISTRATION FORM (Initially Hidden) -->
                <form id="registerForm" action="auth" method="POST" style="display: none;">
                    <input type="hidden" name="action" value="register">
                    <input type="hidden" id="regRole" name="role" value="<%= "recruiter".equalsIgnoreCase(tab) ? "RECRUITER" : "APPLICANT" %>">

                    <h3 id="regTitle" style="font-size: 20px; font-weight: 700; margin-bottom: 6px;">Create Account</h3>
                    <p style="font-size: 13px; color: var(--text-muted); margin-bottom: 18px;">Register to start matchmaking.</p>

                    <div class="form-group">
                        <label>Full Name</label>
                        <input type="text" name="fullName" class="form-control" placeholder="e.g. John Doe" required>
                    </div>

                    <div class="form-group" id="companyField" style="<%= "recruiter".equalsIgnoreCase(tab) ? "" : "display:none;" %>">
                        <label>Company Name</label>
                        <input type="text" name="companyName" class="form-control" placeholder="e.g. Company A Technologies">
                    </div>

                    <div class="form-group">
                        <label>Username</label>
                        <input type="text" name="username" class="form-control" placeholder="e.g. johndoe" required>
                    </div>

                    <div class="form-group">
                        <label>Email Address</label>
                        <input type="email" name="email" class="form-control" placeholder="e.g. john@example.com" required>
                    </div>

                    <div class="form-group">
                        <label>Mobile Number</label>
                        <input type="tel" name="mobile" class="form-control" placeholder="e.g. +91 9876543210">
                    </div>

                    <div class="form-group">
                        <label>Password</label>
                        <input type="password" name="password" class="form-control" placeholder="••••••••" required>
                    </div>

                    <button type="submit" class="btn btn-success" style="width: 100%; margin-top: 10px;">
                        Create Account
                    </button>

                    <div style="text-align: center; margin-top: 15px;">
                        <span class="toggle-link" onclick="toggleForm('login')">Already have an account? Sign in</span>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script>
        let currentRole = '<%= "recruiter".equalsIgnoreCase(tab) ? "RECRUITER" : "APPLICANT" %>';

        function selectRole(role) {
            currentRole = role;
            document.getElementById('loginRole').value = role;
            document.getElementById('regRole').value = role;

            if (role === 'RECRUITER') {
                document.getElementById('tabRecruiter').classList.add('active');
                document.getElementById('tabApplicant').classList.remove('active');
                document.getElementById('formTitle').innerText = 'Recruiter Login';
                document.getElementById('companyField').style.display = 'block';
            } else {
                document.getElementById('tabApplicant').classList.add('active');
                document.getElementById('tabRecruiter').classList.remove('active');
                document.getElementById('formTitle').innerText = 'Applicant Login';
                document.getElementById('companyField').style.display = 'none';
            }
        }

        function toggleForm(mode) {
            if (mode === 'register') {
                document.getElementById('loginForm').style.display = 'none';
                document.getElementById('registerForm').style.display = 'block';
            } else {
                document.getElementById('registerForm').style.display = 'none';
                document.getElementById('loginForm').style.display = 'block';
            }
        }
    </script>
</body>
</html>
