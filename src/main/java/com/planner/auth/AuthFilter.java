package com.planner.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * planner.auth.enabled=true (사내 공유 모드) 일 때만 동작.
 * - 로그인하지 않은 요청: 화면은 로그인 페이지로, API 는 401
 * - 수정 요청(GET 이외)은 X-Requested-With: planner 헤더 필수 → 다른 사이트에서 위조한 요청(CSRF) 차단
 * - 보안 헤더 추가, API 응답 캐시 금지, API 접근 기록
 */
@Component
@ConditionalOnProperty(name = "planner.auth.enabled", havingValue = "true")
public class AuthFilter extends OncePerRequestFilter {
    public static final String SESSION_USER = "planner.user";
    public static final String CSRF_HEADER = "X-Requested-With";
    public static final String CSRF_VALUE = "planner";
    private static final Logger audit = LoggerFactory.getLogger("planner.audit");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "no-referrer");
        res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        if (req.isSecure()) res.setHeader("Strict-Transport-Security", "max-age=31536000");

        String path = req.getRequestURI();
        boolean api = path.startsWith("/api/");
        if (api) {
            res.setHeader("Cache-Control", "no-store");
            if (!"GET".equals(req.getMethod()) && !CSRF_VALUE.equals(req.getHeader(CSRF_HEADER))) {
                deny(res, 403, "허용되지 않은 요청입니다");
                return;
            }
        }
        boolean open = path.equals("/login.html") || path.equals("/api/login") || path.equals("/favicon.ico");
        HttpSession s = req.getSession(false);
        Object user = s == null ? null : s.getAttribute(SESSION_USER);
        if (open || user != null) {
            if (api && user != null) audit.info("{} {} {} {}", user, req.getRemoteAddr(), req.getMethod(), path);
            chain.doFilter(req, res);
            return;
        }
        if (api) deny(res, 401, "로그인이 필요합니다");
        else res.sendRedirect("/login.html");
    }

    private static void deny(HttpServletResponse res, int status, String msg) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"message\":\"" + msg + "\"}");
    }
}
