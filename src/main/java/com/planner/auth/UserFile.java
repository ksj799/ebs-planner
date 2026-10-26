package com.planner.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.Console;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * 로그인 사용자 파일. 한 줄에 한 명: 사용자ID:반복횟수:솔트(Base64):해시(Base64)
 * 비밀번호는 PBKDF2-HMAC-SHA256 으로만 저장한다 (원문 저장 안 함).
 */
public final class UserFile {
    private static final int ITERATIONS = 210_000;
    private UserFile() {}

    public static Path defaultPath() {
        return Paths.get(System.getProperty("user.home"), ".ebs-planner", "users.txt");
    }

    static byte[] hash(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 사용자 ID·비밀번호가 맞으면 true. 파일은 매번 읽어서, 사용자를 추가해도 재시작이 필요 없다. */
    public static boolean verify(Path file, String user, String password) {
        if (user == null || password == null || password.length() > 200) return false;
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String[] p = line.trim().split(":");
                    if (p.length != 4 || !p[0].equals(user)) continue;
                    byte[] expected = Base64.getDecoder().decode(p[3]);
                    byte[] actual = hash(password.toCharArray(), Base64.getDecoder().decode(p[2]), Integer.parseInt(p[1]));
                    return MessageDigest.isEqual(expected, actual);
                }
            }
        } catch (IOException | RuntimeException e) {
            return false;
        }
        // 없는 아이디도 같은 계산을 해서 응답 시간으로 아이디 존재 여부를 알 수 없게 한다
        hash(password.toCharArray(), new byte[16], ITERATIONS);
        return false;
    }

    /** 콘솔에서: java -jar ebs-planner.jar add-user 사용자ID  /  remove-user 사용자ID */
    public static void cli(String[] args) throws IOException {
        Path file = System.getProperty("planner.users-file") != null ? Paths.get(System.getProperty("planner.users-file")) : defaultPath();
        if (args.length < 2 || !args[1].matches("[A-Za-z0-9._-]{2,30}")) {
            System.out.println("사용법: java -jar ebs-planner.jar add-user <사용자ID>   (영문·숫자 2~30자)");
            System.out.println("        java -jar ebs-planner.jar remove-user <사용자ID>");
            return;
        }
        String user = args[1];
        List<String> lines = Files.exists(file) ? new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8)) : new ArrayList<>();
        lines.removeIf(l -> l.startsWith(user + ":"));
        if ("remove-user".equals(args[0])) {
            Files.write(file, lines, StandardCharsets.UTF_8);
            System.out.println("삭제했습니다: " + user);
            return;
        }
        Console console = System.console();
        if (console == null) throw new IllegalStateException("콘솔에서 실행해 주세요 (비밀번호 입력 필요)");
        char[] pw1 = console.readPassword("%s 의 비밀번호: ", user);
        char[] pw2 = console.readPassword("비밀번호 확인: ");
        if (!Arrays.equals(pw1, pw2)) { System.out.println("비밀번호가 서로 다릅니다."); return; }
        String pw = new String(pw1);
        if (pw.length() < 10 || !pw.matches(".*[A-Za-z].*") || !pw.matches(".*[0-9].*") || pw.toLowerCase().contains(user.toLowerCase())) {
            System.out.println("비밀번호는 10자 이상, 영문과 숫자를 모두 포함하고, 아이디를 포함하지 않아야 합니다.");
            return;
        }
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] h = hash(pw1, salt, ITERATIONS);
        Arrays.fill(pw1, ' ');
        Arrays.fill(pw2, ' ');
        lines.add(user + ":" + ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(h));
        Files.createDirectories(file.getParent());
        Files.write(file, lines, StandardCharsets.UTF_8);
        System.out.println("등록했습니다: " + user + "  (" + file + ")");
    }
}
