package com.smartats.filter;

import com.smartats.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(filterName = "AuthenticationFilter", urlPatterns = {"/recruiter-portal.jsp", "/applicant-portal.jsp", "/post-job", "/upload-resume", "/apply", "/auto-shortlist", "/candidate-action"})
public class AuthenticationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String requestUri = req.getRequestURI();

        if (currentUser == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?msg=Please+login+to+continue&type=error");
            return;
        }

        // Role-Based Access Control
        if (requestUri.contains("recruiter") && !"RECRUITER".equalsIgnoreCase(currentUser.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/applicant-portal.jsp?msg=Access+denied+to+recruiter+portal&type=error");
            return;
        }

        if (requestUri.contains("applicant") && !"APPLICANT".equalsIgnoreCase(currentUser.getRole())) {
            resp.sendRedirect(req.getContextPath() + "/recruiter-portal.jsp?msg=Access+denied+to+applicant+portal&type=error");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
