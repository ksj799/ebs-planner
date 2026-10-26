package com.planner.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인/로그아웃.
 * 잠금: 같은 아이디 5회 실패 → 5분, 같은 IP 20회 실패 → 15분.
 * 아이디가 없어도 같은 시간이 걸리게 비교해서 아이디 존재 여부를 알 수 없게 한다.
 */
@RestController
@ConditionalOnProperty(name = "planner.auth.enabled", havingValue = "true")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger("planner.audit");
    private final Path usersFile;
    private final Map<String, long[]> byUser = new ConcurrentHashMap<>(); // [실패수, 잠금해제시각]
    private final Map<String, long[]> byIp = new ConcurrentHashMap<>();

    public AuthController(@Value("${planner.users-file:}") String usersFile) {
        this.usersFile = usersFile == null || usersFile.isEmpty() ? UserFile.defaultPath() : Paths.get(usersFile);
        log.info("로그인 사용자 파일: {}", this.usersFile);
    }

    public static class LoginBody {
        public String username;
        public String password;
    }

    private static boolean locked(Map<String, long[]> m, String key, long now) {
        long[] f = m.get(key);
        return f != null && f[1] > now;
    }

    private static void fail(Map<String, long[]> m, String key, long now, int max, long lockMs) {
        long[] f = m.computeIfAbsent(key, k -> new long[2]);
        synchronized (f) {
            if (f[1] != 0 && f[1] <= now) { f[0] = 0; f[1] = 0; }
            if (++f[0] >= max) { f[0] = 0; f[1] = now + lockMs; }
        }
    }

    @PostMapping("/api/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginBody b, HttpServletRequest req) {
        String user = b.username == null ? "" : b.username.trim();
        String ip = req.getRemoteAddr();
        long now = System.currentTimeMillis();
        if (locked(byUser, user, now) || locked(byIp, ip, now)) {
            log.warn("로그인 차단(잠금): {} {}", user, ip);
            return ResponseEntity.status(429).body(Collections.singletonMap("message", "로그인 시도가 많아 잠시 잠겼습니다. 잠시 후 다시 시도해 주세요."));
        }
        if (user.length() > 30 || !UserFile.verify(usersFile, user, b.password)) {
            fail(byUser, user, now, 5, 5 * 60_000L);
            fail(byIp, ip, now, 20, 15 * 60_000L);
            log.warn("로그인 실패: {} {}", user, ip);
            return ResponseEntity.status(401).body(Collections.singletonMap("message", "아이디 또는 비밀번호가 맞지 않습니다."));
        }
        byUser.remove(user);
        byIp.remove(ip);
        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();          // 세션 고정 공격 방지: 로그인 시 새 세션 발급
        HttpSession s = req.getSession(true);
        s.setAttribute(AuthFilter.SESSION_USER, user);
        s.setMaxInactiveInterval(8 * 60 * 60);
        log.info("로그인: {} {}", user, ip);
        return ResponseEntity.ok(Collections.singletonMap("user", user));
    }

    @PostMapping("/api/logout")
    public Map<String, Object> logout(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) {
            log.info("로그아웃: {} {}", s.getAttribute(AuthFilter.SESSION_USER), req.getRemoteAddr());
            s.invalidate();
        }
        return Collections.<String, Object>singletonMap("ok", true);
    }
}
