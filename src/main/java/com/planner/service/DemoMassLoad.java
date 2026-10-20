package com.planner.service;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** demo 프로파일 전용: EBS 의 WIP Mass Load 를 흉내 내 인터페이스 행을 작업지시로 만든다. */
@Component
@Profile("demo")
public class DemoMassLoad {
    private final NamedParameterJdbcTemplate jdbc;

    public DemoMassLoad(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void process(long groupId, Map<Long, LocalDate> completion) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT interface_id, organization_id, primary_item_id, status_type, start_quantity, "
                        + "first_unit_start_date, source_code, source_line_id, class_code "
                        + "FROM wip_job_schedule_interface WHERE group_id = :g AND process_status = 1",
                new MapSqlParameterSource("g", groupId));
        for (Map<String, Object> r : rows) {
            long interfaceId = ((Number) r.get("INTERFACE_ID")).longValue();
            long id = jdbc.queryForObject("SELECT wip_entities_s.NEXTVAL FROM dual", new MapSqlParameterSource(), Long.class);
            String name = "PP" + id;
            MapSqlParameterSource p = new MapSqlParameterSource()
                    .addValue("id", id).addValue("name", name)
                    .addValue("org", r.get("ORGANIZATION_ID"))
                    .addValue("item", r.get("PRIMARY_ITEM_ID"))
                    .addValue("status", r.get("STATUS_TYPE"))
                    .addValue("qty", r.get("START_QUANTITY"))
                    .addValue("start", r.get("FIRST_UNIT_START_DATE"))
                    .addValue("done", java.sql.Date.valueOf(completion.get(interfaceId)))
                    .addValue("src", r.get("SOURCE_CODE"))
                    .addValue("srcLine", r.get("SOURCE_LINE_ID"))
                    .addValue("cls", r.get("CLASS_CODE"))
                    .addValue("ifId", interfaceId);
            jdbc.update("INSERT INTO wip_entities (wip_entity_id, wip_entity_name, organization_id, entity_type, primary_item_id) "
                    + "VALUES (:id, :name, :org, 1, :item)", p);
            jdbc.update("INSERT INTO wip_discrete_jobs (wip_entity_id, organization_id, primary_item_id, status_type, "
                    + "start_quantity, quantity_completed, quantity_scrapped, scheduled_start_date, scheduled_completion_date, "
                    + "source_code, source_line_id, class_code) "
                    + "VALUES (:id, :org, :item, :status, :qty, 0, 0, :start, :done, :src, :srcLine, :cls)", p);
            jdbc.update("UPDATE wip_job_schedule_interface SET process_phase = 4, process_status = 4, "
                    + "job_name = :name, wip_entity_id = :id WHERE interface_id = :ifId", p);
        }
    }
}
