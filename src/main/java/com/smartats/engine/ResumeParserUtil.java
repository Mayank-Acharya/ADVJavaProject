package com.smartats.engine;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResumeParserUtil {

    private static final Pattern EXP_PATTERN = Pattern.compile(
            "(?:(\\d{1,2})\\+?\\s*(?:years?|yrs?)(?:\\s*of)?\\s*(?:experience|exp)?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern YEAR_RANGE_PATTERN = Pattern.compile(
            "\\b(20\\d{2}|19\\d{2})\\s*(?:-|–|to)\\s*(20\\d{2}|present|current)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}"
    );

    public static String extractTextFromStream(InputStream inputStream, String fileName) throws IOException {
        if (fileName == null) {
            fileName = "";
        }
        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            return extractTextFromPdf(inputStream);
        } else {
            // Read as UTF-8 text file (TXT, Markdown, CSV, etc.)
            return readStreamToString(inputStream);
        }
    }

    /**
     * Extracts text from PDF using Apache PDFBox if available via reflection/direct call,
     * or uses a lightweight stream text extractor fallback.
     */
    public static String extractTextFromPdf(InputStream in) {
        byte[] pdfBytes;
        try {
            pdfBytes = in.readAllBytes();
        } catch (IOException e) {
            return "";
        }

        // 1. Try Apache PDFBox via reflection so code compiles even without PDFBox JAR
        try {
            Class<?> pdDocClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> stripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");

            Method loadMethod = pdDocClass.getMethod("load", byte[].class);
            Object pdDoc = loadMethod.invoke(null, (Object) pdfBytes);

            Object stripper = stripperClass.getDeclaredConstructor().newInstance();
            Method getTextMethod = stripperClass.getMethod("getText", pdDocClass);
            String text = (String) getTextMethod.invoke(stripper, pdDoc);

            Method closeMethod = pdDocClass.getMethod("close");
            closeMethod.invoke(pdDoc);

            if (text != null && !text.trim().isEmpty()) {
                return text;
            }
        } catch (Throwable ignored) {
            // PDFBox not present or threw error; use stream scanner fallback
        }

        // 2. Fallback PDF text stream parser
        return parsePdfRawText(pdfBytes);
    }

    private static String parsePdfRawText(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        String raw = new String(bytes, StandardCharsets.ISO_8859_1);

        // Match PDF text objects: BT (Begin Text) ... ET (End Text)
        Pattern btEtPattern = Pattern.compile("BT\\s*(.*?)\\s*ET", Pattern.DOTALL);
        Matcher matcher = btEtPattern.matcher(raw);

        Pattern tjPattern = Pattern.compile("\\((.*?)\\)\\s*T[jJ]");
        while (matcher.find()) {
            String block = matcher.group(1);
            Matcher tjMatcher = tjPattern.matcher(block);
            while (tjMatcher.find()) {
                String token = tjMatcher.group(1);
                // Unescape standard PDF octals and escaped chars
                token = token.replace("\\(", "(").replace("\\)", ")").replace("\\\\", "\\");
                sb.append(token).append(" ");
            }
            sb.append("\n");
        }

        if (sb.length() < 20) {
            // If PDF was encoded/compressed, fallback to regex word extraction
            Pattern wordPattern = Pattern.compile("[A-Za-z0-9#+.-]{2,}");
            Matcher wm = wordPattern.matcher(raw);
            while (wm.find()) {
                String w = wm.group();
                if (w.length() < 30) {
                    sb.append(w).append(" ");
                }
            }
        }

        return sb.toString();
    }

    public static int extractExperienceYears(String resumeText) {
        if (resumeText == null || resumeText.trim().isEmpty()) {
            return 0;
        }

        int maxExp = 0;

        // Check for explicit "X years of experience" pattern
        Matcher matcher = EXP_PATTERN.matcher(resumeText);
        while (matcher.find()) {
            try {
                int exp = Integer.parseInt(matcher.group(1));
                if (exp > 0 && exp <= 40) {
                    maxExp = Math.max(maxExp, exp);
                }
            } catch (NumberFormatException ignored) {}
        }

        // Also check year ranges like 2020 - 2024 (4 years)
        Matcher rangeMatcher = YEAR_RANGE_PATTERN.matcher(resumeText);
        int currentYear = 2026;
        while (rangeMatcher.find()) {
            try {
                int startYear = Integer.parseInt(rangeMatcher.group(1));
                String endStr = rangeMatcher.group(2).toLowerCase();
                int endYear = (endStr.contains("present") || endStr.contains("current"))
                        ? currentYear
                        : Integer.parseInt(endStr);

                int diff = endYear - startYear;
                if (diff > 0 && diff <= 40) {
                    maxExp = Math.max(maxExp, diff);
                }
            } catch (Exception ignored) {}
        }

        return maxExp;
    }

    public static String extractEmail(String resumeText) {
        if (resumeText == null) return null;
        Matcher m = EMAIL_PATTERN.matcher(resumeText);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    public static String extractPhone(String resumeText) {
        if (resumeText == null) return null;
        Matcher m = PHONE_PATTERN.matcher(resumeText);
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    private static String readStreamToString(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
