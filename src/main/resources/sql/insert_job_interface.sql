-- LOAD_TYPE 1 = 표준 작업지시 생성, PROCESS_PHASE 2 = 검증, PROCESS_STATUS 1 = 대기
INSERT INTO wip_job_schedule_interface
       (last_update_date, last_updated_by, creation_date, created_by,
        group_id, interface_id, source_code, source_line_id,
        process_phase, process_status, organization_id, load_type, status_type,
        primary_item_id, start_quantity, net_quantity,
        first_unit_start_date, last_unit_completion_date, scheduling_method,
        class_code, description)
VALUES (SYSDATE, :userId, SYSDATE, :userId,
        :groupId, :interfaceId, :sourceCode, :sourceLineId,
        2, 1, :orgId, 1, :statusType,
        :itemId, :qty, :qty,
        :startDate, :completionDate, :schedulingMethod,
        :classCode, :description)
