package com.planner.repo;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** classpath:sql/*.sql 을 읽는다. SQL 을 파일로 분리해 DBA 가 검토·튜닝하기 쉽게 했다. */
@Component
public class SqlLoader {
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String get(String name) {
        return cache.computeIfAbsent(name, n -> {
            try (InputStream in = new ClassPathResource("sql/" + n + ".sql").getInputStream()) {
                String text = StreamUtils.copyToString(in, StandardCharsets.UTF_8).trim();
                String head = text.toUpperCase();
                boolean plsql = head.startsWith("DECLARE") || head.startsWith("BEGIN");
                return plsql ? text : text.replaceAll(";\\s*$", "");
            } catch (IOException e) {
                throw new UncheckedIOException("SQL 파일을 읽을 수 없습니다: " + n, e);
            }
        });
    }
}
