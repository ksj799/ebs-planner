package com.planner.repo;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.planner.repo.EbsRepository.sqlDate;

/**
 * 품목 조회. 속성이 많아 컬럼명(소문자) → 값 Map 으로 돌려주고, 화면이 Oracle 품목 화면 탭 구성대로 표시한다.
 */
@Repository
public class ItemRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final SqlLoader sql;

    public ItemRepository(NamedParameterJdbcTemplate jdbc, SqlLoader sql) {
        this.jdbc = jdbc;
        this.sql = sql;
    }

    private static Map<String, Object> norm(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Timestamp) v = ((Timestamp) v).toLocalDateTime().toLocalDate();
            else if (v instanceof java.sql.Date) v = ((java.sql.Date) v).toLocalDate();
            m.put(e.getKey().toLowerCase(Locale.ROOT), v);
        }
        return m;
    }

    private List<Map<String, Object>> list(String name, MapSqlParameterSource p) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(sql.get(name), p)) out.add(norm(r));
        return out;
    }

    /** makeBuy: null=전체, 1=제조, 2=구매 */
    public List<Map<String, Object>> search(long orgId, String q, Integer makeBuy, int limit) {
        String like = q == null || q.trim().isEmpty() ? "%" : "%" + q.trim().toUpperCase(Locale.ROOT) + "%";
        MapSqlParameterSource p = new MapSqlParameterSource("orgId", orgId)
                .addValue("q", like)
                .addValue("makeBuy", makeBuy == null ? Arrays.asList(1, 2) : Arrays.asList(makeBuy))
                .addValue("limit", limit);
        return list("items_search", p);
    }

    public Map<String, Object> detail(long orgId, long itemId) {
        List<Map<String, Object>> rows = list("item_detail", new MapSqlParameterSource("orgId", orgId).addValue("itemId", itemId));
        if (rows.isEmpty()) throw new IllegalArgumentException("품목이 이 조직에 없습니다: " + itemId);
        return rows.get(0);
    }

    public Map<String, Object> supply(long orgId, long itemId) {
        return list("item_supply", new MapSqlParameterSource("orgId", orgId).addValue("itemId", itemId)).get(0);
    }

    public List<Map<String, Object>> bom(long orgId, long itemId, LocalDate today) {
        return list("item_bom", new MapSqlParameterSource("orgId", orgId).addValue("itemId", itemId).addValue("today", sqlDate(today)));
    }

    public List<Map<String, Object>> whereUsed(long orgId, long itemId, LocalDate today) {
        return list("item_where_used", new MapSqlParameterSource("orgId", orgId).addValue("itemId", itemId).addValue("today", sqlDate(today)));
    }
}
