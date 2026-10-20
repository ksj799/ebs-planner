package com.planner.repo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planner.config.PlannerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 플래너 전용 설정 저장소 (JSON 파일). EBS 데이터는 바꾸지 않고
 * 입고예정일 수정값·리드타임 수정값만 조직별로 보관한다.
 */
@Component
public class PlannerStore {
    private static final Logger log = LoggerFactory.getLogger(PlannerStore.class);

    public static class ReceiptOverride {
        public String date;
        public String note;
    }

    public static class LeadTimeOverride {
        public Double pre;
        public Double full;
        public Double post;
    }

    public static class OrgSettings {
        public Map<String, ReceiptOverride> receipts = new TreeMap<>();
        public Map<String, LeadTimeOverride> leadTimes = new TreeMap<>();
    }

    private final Path file;
    private final ObjectMapper om;
    private Map<String, OrgSettings> data = new TreeMap<>();

    public PlannerStore(PlannerProperties props, ObjectMapper om) {
        String path = props.getStorePath();
        this.file = path == null || path.trim().isEmpty() ? null : Paths.get(path).toAbsolutePath();
        this.om = om;
        if (file == null) {
            log.info("플래너 설정: 메모리 저장 (파일 없음)");
            return;
        }
        if (Files.exists(file)) {
            try {
                data = om.readValue(file.toFile(), new TypeReference<TreeMap<String, OrgSettings>>() {});
            } catch (IOException e) {
                throw new UncheckedIOException("설정 파일을 읽을 수 없습니다: " + file, e);
            }
        }
        log.info("플래너 설정 파일: {}", file);
    }

    public synchronized Map<Long, ReceiptOverride> receipts(long orgId) {
        Map<Long, ReceiptOverride> m = new HashMap<>();
        OrgSettings s = data.get(String.valueOf(orgId));
        if (s != null) s.receipts.forEach((k, v) -> m.put(Long.valueOf(k), v));
        return m;
    }

    public synchronized Map<Long, LeadTimeOverride> leadTimes(long orgId) {
        Map<Long, LeadTimeOverride> m = new HashMap<>();
        OrgSettings s = data.get(String.valueOf(orgId));
        if (s != null) s.leadTimes.forEach((k, v) -> m.put(Long.valueOf(k), v));
        return m;
    }

    public synchronized void setReceipt(long orgId, long lineLocationId, ReceiptOverride o) {
        OrgSettings s = data.computeIfAbsent(String.valueOf(orgId), k -> new OrgSettings());
        if (o == null || o.date == null || o.date.isEmpty()) s.receipts.remove(String.valueOf(lineLocationId));
        else s.receipts.put(String.valueOf(lineLocationId), o);
        save();
    }

    public synchronized void setLeadTime(long orgId, long itemId, LeadTimeOverride o) {
        OrgSettings s = data.computeIfAbsent(String.valueOf(orgId), k -> new OrgSettings());
        if (o == null || (o.pre == null && o.full == null && o.post == null)) s.leadTimes.remove(String.valueOf(itemId));
        else s.leadTimes.put(String.valueOf(itemId), o);
        save();
    }

    private void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            om.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), data);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("설정 파일을 저장할 수 없습니다: " + file, e);
        }
    }
}
