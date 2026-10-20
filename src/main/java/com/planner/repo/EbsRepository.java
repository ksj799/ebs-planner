package com.planner.repo;

import com.planner.model.Model.Demand;
import com.planner.model.Model.InterfaceRow;
import com.planner.model.Model.Org;
import com.planner.model.Model.Resource;
import com.planner.model.Model.RoutingOp;
import com.planner.model.Model.Supply;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.planner.model.Model.key;

/** EBS 테이블 조회 (읽기 전용) */
@Repository
public class EbsRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final SqlLoader sql;

    public EbsRepository(NamedParameterJdbcTemplate jdbc, SqlLoader sql) {
        this.jdbc = jdbc;
        this.sql = sql;
    }

    static java.sql.Date sqlDate(LocalDate d) {
        return d == null ? null : java.sql.Date.valueOf(d);
    }

    static LocalDate date(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime().toLocalDate();
    }

    /** NUMBER 컬럼을 지수 표기(1.051E+4) 없이 문자열로 */
    static String num(ResultSet rs, String col) throws SQLException {
        java.math.BigDecimal v = rs.getBigDecimal(col);
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }

    static Long longOrNull(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    public List<Org> orgs() {
        return jdbc.query(sql.get("orgs"), new MapSqlParameterSource(), (rs, i) -> {
            Org o = new Org();
            o.organizationId = rs.getLong("organization_id");
            o.code = rs.getString("organization_code");
            o.name = rs.getString("organization_name");
            o.calendarCode = rs.getString("calendar_code");
            o.exceptionSetId = rs.getLong("calendar_exception_set_id");
            o.defaultClass = rs.getString("default_discrete_class");
            return o;
        });
    }

    public Org org(long organizationId) {
        for (Org o : orgs()) {
            if (o.organizationId == organizationId) return o;
        }
        throw new IllegalArgumentException("WIP 사용 조직이 아닙니다: ORGANIZATION_ID=" + organizationId);
    }

    public List<LocalDate> workdays(Org org, LocalDate from, LocalDate to) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("calendarCode", org.calendarCode)
                .addValue("exceptionSetId", org.exceptionSetId)
                .addValue("fromDate", sqlDate(from))
                .addValue("toDate", sqlDate(to));
        return jdbc.query(sql.get("workdays"), p, (rs, i) -> date(rs, "calendar_date"));
    }

    public List<Resource> resources(Org org, LocalDate today) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("orgId", org.organizationId)
                .addValue("today", sqlDate(today))
                .addValue("calendarCode", org.calendarCode);
        Map<String, Double> shiftHours = new HashMap<>();
        jdbc.query(sql.get("resource_shift_hours"), p, (RowCallbackHandler) rs ->
                shiftHours.put(key(rs.getLong("department_id"), rs.getLong("resource_id")), rs.getDouble("shift_unit_hours")));
        return jdbc.query(sql.get("resources"), p, (rs, i) -> {
            Resource r = new Resource();
            r.departmentId = rs.getLong("department_id");
            r.resourceId = rs.getLong("resource_id");
            r.key = key(r.departmentId, r.resourceId);
            r.departmentCode = rs.getString("department_code");
            r.resourceCode = rs.getString("resource_code");
            r.description = rs.getString("description");
            r.uom = rs.getString("unit_of_measure");
            r.capacityUnits = rs.getDouble("capacity_units");
            r.available24h = rs.getInt("available_24_hours_flag") == 1;
            Double sh = shiftHours.get(r.key);
            r.shiftUnitHours = sh == null ? 0 : sh;
            return r;
        });
    }

    /** 지정한 품목들의 라우팅만 읽는다 (조직 전체 라우팅은 수십만 행이 될 수 있음) */
    public List<RoutingOp> routingOps(long orgId, LocalDate today, java.util.Collection<Long> itemIds) {
        List<RoutingOp> out = new ArrayList<>();
        List<Long> ids = new ArrayList<>(itemIds);
        for (int i = 0; i < ids.size(); i += 900) {
            MapSqlParameterSource p = new MapSqlParameterSource("orgId", orgId).addValue("today", sqlDate(today))
                    .addValue("itemIds", ids.subList(i, Math.min(ids.size(), i + 900)));
            out.addAll(routingOpsChunk(p));
        }
        return out;
    }

    private List<RoutingOp> routingOpsChunk(MapSqlParameterSource p) {
        return jdbc.query(sql.get("routing_ops"), p, (rs, i) -> {
            RoutingOp o = new RoutingOp();
            o.itemId = rs.getLong("inventory_item_id");
            o.itemCode = rs.getString("item_code");
            o.itemDesc = rs.getString("item_desc");
            o.uom = rs.getString("primary_uom_code");
            o.operationSeq = rs.getInt("operation_seq_num");
            o.departmentId = rs.getLong("department_id");
            o.resourceId = rs.getLong("resource_id");
            o.resourceUom = rs.getString("resource_uom");
            o.usage = rs.getDouble("usage_rate_or_amount");
            o.basisType = rs.getInt("basis_type");
            return o;
        });
    }

    public List<Demand> demands(long orgId, LocalDate toDate) {
        MapSqlParameterSource p = new MapSqlParameterSource("orgId", orgId).addValue("toDate", sqlDate(toDate));
        return jdbc.query(sql.get("demands"), p, (rs, i) -> {
            Demand d = new Demand();
            d.lineId = rs.getLong("line_id");
            d.orderNumber = num(rs, "order_number");
            String ship = num(rs, "shipment_number");
            d.lineRef = num(rs, "line_number") + (ship == null ? "" : "." + ship);
            d.customer = rs.getString("customer_name");
            d.itemId = rs.getLong("inventory_item_id");
            d.itemCode = rs.getString("item_code");
            d.itemDesc = rs.getString("item_desc");
            d.uom = rs.getString("primary_uom_code");
            d.orderUom = rs.getString("order_quantity_uom");
            d.openQty = rs.getDouble("open_quantity");
            d.due = date(rs, "due_date");
            return d;
        });
    }

    private static String wipStatus(int s) {
        switch (s) {
            case 1: return "미해제";
            case 3: return "해제";
            case 6: return "보류";
            default: return "상태 " + s;
        }
    }

    public List<Supply> openJobs(long orgId, String sourceCode) {
        return jdbc.query(sql.get("open_jobs"), new MapSqlParameterSource("orgId", orgId), (rs, i) -> {
            Supply s = new Supply();
            s.source = "WIP";
            s.id = rs.getLong("wip_entity_id");
            s.ref = rs.getString("wip_entity_name");
            s.itemId = rs.getLong("primary_item_id");
            s.itemCode = rs.getString("item_code");
            s.itemDesc = rs.getString("item_desc");
            s.statusType = rs.getInt("status_type");
            s.status = wipStatus(s.statusType);
            s.remainQty = rs.getDouble("remain_quantity");
            s.start = date(rs, "scheduled_start_date");
            s.completion = date(rs, "scheduled_completion_date");
            s.plannerCreated = sourceCode.equals(rs.getString("source_code"));
            return s;
        });
    }

    public List<Supply> pendingInterface(long orgId, String sourceCode) {
        return jdbc.query(sql.get("pending_interface"), new MapSqlParameterSource("orgId", orgId), (rs, i) -> {
            Supply s = new Supply();
            s.source = "INTERFACE";
            s.id = rs.getLong("interface_id");
            s.ref = "IF-" + s.id + " (G" + rs.getLong("group_id") + ")";
            s.itemId = rs.getLong("primary_item_id");
            s.itemCode = rs.getString("item_code");
            s.itemDesc = rs.getString("item_desc");
            s.statusType = rs.getInt("status_type");
            s.status = "생성 대기";
            s.remainQty = rs.getDouble("start_quantity");
            s.start = date(rs, "first_unit_start_date");
            s.completion = date(rs, "last_unit_completion_date");
            s.plannerCreated = sourceCode.equals(rs.getString("source_code"));
            return s;
        });
    }

    public Map<Long, Double> onhand(long orgId) {
        Map<Long, Double> m = new HashMap<>();
        jdbc.query(sql.get("onhand"), new MapSqlParameterSource("orgId", orgId), (RowCallbackHandler) rs ->
                m.put(rs.getLong("inventory_item_id"), rs.getDouble("onhand_quantity")));
        return m;
    }

    private static String phaseLabel(int phase, int status) {
        switch (status) {
            case 1: return "대기";
            case 2: return "처리중";
            case 3: return "오류";
            case 4: return phase >= 4 ? "완료" : "처리됨";
            case 5: return "경고";
            default: return "상태 " + status;
        }
    }

    public List<InterfaceRow> interfaceRows(long orgId, String sourceCode, LocalDate from) {
        MapSqlParameterSource p = new MapSqlParameterSource("orgId", orgId)
                .addValue("sourceCode", sourceCode).addValue("fromDate", sqlDate(from));
        Map<Long, InterfaceRow> rows = new LinkedHashMap<>();
        jdbc.query(sql.get("interface_rows"), p, (RowCallbackHandler) rs -> {
            InterfaceRow r = new InterfaceRow();
            r.groupId = rs.getLong("group_id");
            r.interfaceId = rs.getLong("interface_id");
            r.sourceLineId = longOrNull(rs, "source_line_id");
            r.itemCode = rs.getString("item_code");
            r.qty = rs.getDouble("start_quantity");
            r.startDate = date(rs, "first_unit_start_date");
            r.phase = rs.getInt("process_phase");
            r.processStatus = rs.getInt("process_status");
            r.status = phaseLabel(r.phase, r.processStatus);
            r.jobName = rs.getString("job_name");
            r.wipEntityId = longOrNull(rs, "wip_entity_id");
            r.created = date(rs, "creation_date");
            r.description = rs.getString("description");
            rows.put(r.interfaceId, r);
        });
        if (!rows.isEmpty()) {
            List<Long> ids = new ArrayList<>(rows.keySet());
            // Oracle IN 목록 1000개 제한
            for (int i = 0; i < ids.size(); i += 900) {
                List<Long> chunk = ids.subList(i, Math.min(ids.size(), i + 900));
                jdbc.query(sql.get("interface_errors"), new MapSqlParameterSource("ids", chunk), (RowCallbackHandler) rs -> {
                    InterfaceRow r = rows.get(rs.getLong("interface_id"));
                    if (r != null) r.errors.add(rs.getString("error"));
                });
            }
        }
        return new ArrayList<>(rows.values());
    }
}
