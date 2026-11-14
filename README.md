# 🎯 SmartATS: Automated Resume Parser & ATS Matchmaking Engine

An enterprise-grade, framework-free Recruitment & ATS Matchmaking Engine built with pure **Java 17/24 (`java.util`, `java.io`, `java.sql` JDBC)**, **Jakarta Servlets**, **JSP 3.1**, **XML DOM Parser**, **MySQL 8.x**, and **Jakarta Mail (SMTP)**.

---

## 🌟 Key Viva Highlights & Algorithmic Features

1. **Max-Heap Top-$K$ Selection (`java.util.PriorityQueue` & `Comparator`)**:
   - Ranks applicant pool in $O(N \log K)$ time complexity.
   - Pops the first $K$ highest-scoring candidates to be marked `SELECTED`.
   - Automatically marks remainder applications as `REJECTED`.

2. **Atomic ACID JDBC Transaction (`setAutoCommit(false)`)**:
   - The Top-$K$ selection, application status updates, and notification insertions are batched and executed inside a strict ACID transaction with rollback on failure.

3. **XML Skill Taxonomy & Synonym Expansion (`role-skills.xml`)**:
   - Uses `javax.xml.parsers.DocumentBuilder` to parse and cache role-to-skill mappings and synonyms (e.g., "J2SE" $\rightarrow$ "Java", "Retrofit" $\rightarrow$ "REST API").

4. **Multi-Factor Weighted ATS Formula**:
   $$\text{ATS Score} = (\text{Keyword \& Synonym Match} \times 70\%) + (\text{Experience Match} \times 30\%)$$

5. **Reverse-ATS Job Feed for Applicants**:
   - Applicants upload their resume once. The engine evaluates all active jobs and **filters the dashboard to show only jobs where candidate ATS Score $\ge$ Job Cutoff Threshold**.

6. **Natural Sentence Job Builder for Recruiters**:
   - Intuitive sentence-based input form dynamically bound to the XML taxonomy and database constraints.

7. **Dual-Layer Real-Time Notifications**:
   - **In-App Notification Bell**: Non-intrusive 10-second AJAX polling (`fetch API`) updating the red badge count and dropdown list.
   - **Async SMTP Email Invitations**: Multithreaded `ExecutorService` daemon thread pool dispatching HTML interview invitations with meeting links directly to candidates' inboxes without blocking the UI.

---

## 📂 Project Directory Structure

```
JavaProject/
├── pom.xml                               # Maven Project Descriptor
├── compile.bat                           # Compiles all Java classes
├── run_test.bat                          # Executes automated scenario test
├── build_war.bat                         # Packages self-contained WAR
├── database/
│   └── schema.sql                        # MySQL schema creation script
├── src/
│   ├── main/
│   │   ├── java/com/smartats/
│   │   │   ├── model/                    # User, JobPosting, Application, Notification
│   │   │   ├── dao/                      # DBConnectionPool, UserDAO, JobDAO, ApplicationDAO, NotificationDAO
│   │   │   ├── engine/                   # ResumeParserUtil, XMLTaxonomyParser, ATSEngine, TopKSelector
│   │   │   ├── util/                     # PasswordUtil, EmailNotificationUtil
│   │   │   ├── servlet/                  # AuthServlet, PostJobServlet, UploadResumeServlet,
│   │   │   │                             # ApplyJobServlet, AutoShortlistServlet, CandidateActionServlet, NotificationServlet
│   │   │   └── filter/                   # AuthenticationFilter
│   │   ├── resources/
│   │   │   ├── db.properties             # MySQL & SMTP configuration
│   │   │   └── role-skills.xml           # XML Skill Taxonomy
│   │   └── webapp/
│   │       ├── assets/                   # Modern CSS & Notification Polling JS
│   │       ├── WEB-INF/
│   │       │   ├── web.xml               # Deployment Descriptor
│   │       │   ├── role-skills.xml
│   │       │   └── lib/                  # Bundled MySQL, PDFBox, Jakarta Mail, H2 JARs
│   │       ├── index.jsp                 # Dual Portal Login & Registration
│   │       ├── recruiter-portal.jsp      # Sentence Builder & Top-K Shortlist UI
│   │       └── applicant-portal.jsp      # Resume Upload & Reverse-ATS Job Feed
│   └── test/
│       └── java/com/smartats/test/       # MainDemoTest.java
└── target/
    └── smart-ats.war                     # Ready-to-deploy WAR file (18 MB)
```

---

## 🚀 How to Run the Project

### Option A: Immediate Verification Test (One-Click)
Run the automated end-to-end verification script from the command line:
```cmd
run_test.bat
```
This runs `MainDemoTest.java`, testing the complete user scenario:
- Registers **Company A**.
- Posts **Job Alpha** ($K = 2$, Cutoff 60%).
- Candidates $a$, $b$, and $c$ upload resumes and apply.
- Calculates ATS scores:
  - **Candidate $b$**: $86.8\%$
  - **Candidate $a$**: $63.5\%$
  - **Candidate $c$**: $25.1\%$ (Filtered out by Reverse-ATS cutoff).
- Runs **Top-$K$ Max-Heap selection** ($K = 2$):
  - Candidates $b$ and $a$ are marked `SELECTED`.
  - Candidate $c$ is marked `REJECTED`.
- Verifies in-app notification bell alerts and async email dispatch preview!

---

### Option B: Run in Apache Tomcat 10 (Web Browser)

1. Deploy the pre-built WAR file:
   - Copy `target\smart-ats.war` into your Tomcat `webapps` folder:
     ```cmd
     copy target\smart-ats.war C:\apache-tomcat-10.1.60\webapps\
     ```
2. Start Apache Tomcat:
   ```cmd
   C:\apache-tomcat-10.1.60\bin\startup.bat
   ```
3. Open your browser:
   ```
   http://localhost:8080/smart-ats/
   ```

---

## ⚙️ Configuration (`src/main/resources/db.properties`)

### 1. MySQL Database (Default):
```properties
db.type=mysql
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/smart_ats_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=root
```
*Note: If local MySQL is offline, `DBConnectionPool` seamlessly activates a zero-configuration embedded database so the app never crashes during offline demos.*

### 2. Live Gmail SMTP Alerts:
To have selected candidates receive real emails on their smartphones:
1. Turn on 2-Step Verification in your Google Account.
2. Generate a 16-character **App Password** under Security.
3. Update `src/main/resources/db.properties`:
```properties
mail.smtp.enabled=true
mail.smtp.username=yourprojectemail@gmail.com
mail.smtp.password=abcd efgh ijkl mnop
```
