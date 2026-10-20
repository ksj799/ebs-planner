package com.planner.repo;

import com.planner.config.PlannerProperties;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;

/** EBS 쓰기: WIP_JOB_SCHEDULE_INTERFACE 적재와 WIP Mass Load 제출만 한다. */
@Repository
public class WipInterfaceRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final SqlLoader sql;

    public WipInterfaceRepository(NamedParameterJdbcTemplate jdbc, SqlLoader sql) {
        this.jdbc = jdbc;
        this.sql = sql;
    }

    public long nextGroupId() {
        // [조회 전용] 시퀀스 채번 주석 처리 (2026-10-13)
        // return jdbc.queryForObject(sql.get("next_group_id"), new MapSqlParameterSource(), Long.class);
        throw new IllegalStateException("조회 전용 모드: WIP_JOB_SCHEDULE_INTERFACE_S 채번이 비활성화되어 있습니다.");
    }

    public long nextInterfaceId() {
        // [조회 전용] 시퀀스 채번 주석 처리 (2026-10-13)
        // return jdbc.queryForObject(sql.get("next_interface_id"), new MapSqlParameterSource(), Long.class);
        throw new IllegalStateException("조회 전용 모드: WIP_INTERFACE_S 채번이 비활성화되어 있습니다.");
    }

    public static class JobRow {
        public long groupId;
        public long interfaceId;
        public long orgId;
        public long itemId;
        public double qty;
        public int statusType;
        public int schedulingMethod;
        public LocalDate start;
        public LocalDate completion;
        public String classCode;
        public Long sourceLineId;
        public String description;
    }

    public void insert(JobRow row, PlannerProperties props) {
        PlannerProperties.Wip wip = props.getWip();
        Timestamp start = Timestamp.valueOf(row.start.atTime(LocalTime.parse(wip.getStartTime())));
        // 수동 일정(3)일 때만 완료일을 함께 넘긴다. 라우팅 기준(1)은 EBS 가 시작일부터 전진 일정 계산
        Timestamp completion = row.schedulingMethod == 3 && row.completion != null
                ? Timestamp.valueOf(row.completion.atTime(LocalTime.parse(wip.getCompletionTime()))) : null;
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("userId", props.getUserId())
                .addValue("groupId", row.groupId)
                .addValue("interfaceId", row.interfaceId)
                .addValue("sourceCode", props.getSourceCode())
                .addValue("sourceLineId", row.sourceLineId)
                .addValue("orgId", row.orgId)
                .addValue("statusType", row.statusType)
                .addValue("itemId", row.itemId)
                .addValue("qty", row.qty)
                .addValue("startDate", start, Types.TIMESTAMP)
                .addValue("completionDate", completion, Types.TIMESTAMP)
                .addValue("schedulingMethod", row.schedulingMethod)
                .addValue("classCode", row.classCode, Types.VARCHAR)
                .addValue("description", row.description, Types.VARCHAR);
        // [조회 전용] EBS INSERT 주석 처리 (2026-10-13)
        // jdbc.update(sql.get("insert_job_interface"), p);
        throw new IllegalStateException("조회 전용 모드: WIP_JOB_SCHEDULE_INTERFACE INSERT 가 비활성화되어 있습니다.");
    }

    /** WIP Mass Load(WICMLP) 동시요청 제출. 0 이 반환되면 예외 → 같은 트랜잭션의 인터페이스 적재도 롤백된다. */
    public long submitMassLoad(long groupId, PlannerProperties props) {
        // [조회 전용] WIP Mass Load 제출 주석 처리 (2026-10-13)
        throw new IllegalStateException("조회 전용 모드: WIP Mass Load 제출이 비활성화되어 있습니다.");
        /*
        PlannerProperties.MassLoad ml = props.getMassLoad();
        if (ml.getRespId() == null) {
            throw new IllegalStateException("planner.mass-load.resp-id(책임 ID)를 설정하세요.");
        }
        return jdbc.getJdbcOperations().execute(sql.get("submit_mass_load"), (CallableStatementCallback<Long>) cs -> {
            cs.setLong(1, props.getUserId());
            cs.setLong(2, ml.getRespId());
            cs.setLong(3, ml.getRespApplId());
            cs.setString(4, String.valueOf(groupId));
            cs.setString(5, ml.getValidationLevel());
            cs.setString(6, ml.getPrintReport());
            cs.registerOutParameter(7, Types.NUMERIC);
            cs.registerOutParameter(8, Types.VARCHAR);
            cs.execute();
            long requestId = cs.getLong(7);
            if (requestId == 0) {
                throw new IllegalStateException("WIP Mass Load 제출 실패: " + cs.getString(8));
            }
            return requestId;
        });
        */
    }
}
