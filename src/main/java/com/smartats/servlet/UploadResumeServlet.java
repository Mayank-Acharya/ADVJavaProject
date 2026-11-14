package com.smartats.servlet;

import com.smartats.dao.NotificationDAO;
import com.smartats.dao.UserDAO;
import com.smartats.engine.ResumeParserUtil;
import com.smartats.model.Notification;
import com.smartats.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "UploadResumeServlet", urlPatterns = {"/upload-resume"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB
        maxFileSize = 1024 * 1024 * 10,       // 10 MB
        maxRequestSize = 1024 * 1024 * 20     // 20 MB
)
public class UploadResumeServlet extends HttpServlet {

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
            Part filePart = req.getPart("resumeFile");
            String manualExpStr = req.getParameter("manual_exp_years");

            if (filePart == null || filePart.getSize() == 0) {
                // If candidate provided direct text input instead
                String directText = req.getParameter("resume_text_direct");
                if (directText != null && !directText.trim().isEmpty()) {
                    int exp = (manualExpStr != null && !manualExpStr.isEmpty())
                            ? Integer.parseInt(manualExpStr.trim())
                            : ResumeParserUtil.extractExperienceYears(directText);

                    userDAO.updateResumeProfile(currentUser.getUserId(), "pasted_text", directText, exp);
                    currentUser.setResumeText(directText);
                    currentUser.setCandidateExpYears(exp);
                    session.setAttribute("currentUser", currentUser);

                    notifDAO.createNotification(new Notification(currentUser.getUserId(),
                            "Resume Updated", "Your resume text was parsed. Experience detected: " + exp + " years."));

                    resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?msg="
                            + urlEncode("Resume parsed successfully! Detected " + exp + " years of experience."));
                    return;
                }

                resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?msg="
                        + urlEncode("Please choose a PDF/Text resume file or paste your resume text.") + "&type=error");
                return;
            }

            String submittedFileName = filePart.getSubmittedFileName();
            String uploadsDirPath = req.getServletContext().getRealPath("/uploads");
            if (uploadsDirPath == null) {
                uploadsDirPath = System.getProperty("java.io.tmpdir") + File.separator + "smartats_uploads";
            }
            File uploadsDir = new File(uploadsDirPath);
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }

            String storedFileName = "resume_" + currentUser.getUserId() + "_" + System.currentTimeMillis() + "_" + submittedFileName;
            File savedFile = new File(uploadsDir, storedFileName);

            try (InputStream in = filePart.getInputStream();
                 FileOutputStream out = new FileOutputStream(savedFile)) {
                byte[] buf = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buf)) != -1) {
                    out.write(buf, 0, bytesRead);
                }
            }

            // Extract plain text from the saved file
            String extractedText;
            try (InputStream in = filePart.getInputStream()) {
                extractedText = ResumeParserUtil.extractTextFromStream(in, submittedFileName);
            }

            int detectedExp = ResumeParserUtil.extractExperienceYears(extractedText);
            if (manualExpStr != null && !manualExpStr.trim().isEmpty()) {
                try {
                    int manualExp = Integer.parseInt(manualExpStr.trim());
                    if (manualExp > 0) {
                        detectedExp = manualExp;
                    }
                } catch (NumberFormatException ignored) {}
            }

            // Update user profile in database
            userDAO.updateResumeProfile(currentUser.getUserId(), savedFile.getAbsolutePath(), extractedText, detectedExp);

            // Update current session user
            currentUser.setResumeFilePath(savedFile.getAbsolutePath());
            currentUser.setResumeText(extractedText);
            currentUser.setCandidateExpYears(detectedExp);
            session.setAttribute("currentUser", currentUser);

            // Create notification
            notifDAO.createNotification(new Notification(currentUser.getUserId(),
                    "Resume Analyzed Successfully",
                    "Your resume (" + submittedFileName + ") was analyzed. Detected Experience: " + detectedExp
                            + " years. Check out your Reverse-ATS matched jobs below!"));

            resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?msg="
                    + urlEncode("Resume parsed successfully! Detected " + detectedExp + " years of experience. See your matched jobs below."));

        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?msg="
                    + urlEncode("Failed to parse resume: " + e.getMessage()) + "&type=error");
        }
    }

    private String urlEncode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }
}
