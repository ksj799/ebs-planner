package com.planner.service;

import com.planner.config.PlannerProperties;
import com.planner.model.Model.PlanResult;
import com.planner.model.Model.PlannedJob;
import com.planner.model.Model.ReleaseRequest;
import com.planner.model.Model.ReleaseResult;
import com.planner.repo.WipInterfaceRepository;
import com.planner.repo.WipInterfaceRepository.JobRow;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 선택한 계획 작업지시를 WIP_JOB_SCHEDULE_INTERFACE 에 적재 (한 그룹 = 한 트랜잭션) */
@Service
public class ReleaseService {
    private final PlanningService planning;
    private final WipInterfaceRepository wip;
    private final PlannerProperties props;
    private final ObjectProvider<DemoMassLoad> demo;

    public ReleaseService(PlanningService planning, WipInterfaceRepository wip, PlannerProperties props,
                          ObjectProvider<DemoMassLoad> demo) {
        this.planning = planning;
        this.wip = wip;
        this.props = props;
        this.demo = demo;
    }

    @Transactional
    public ReleaseResult release(ReleaseRequest req) {
        // ===================================================================================
        // [조회 전용] EBS DB 반영 로직 주석 처리 (2026-10-13)
        //  이 메서드가 EBS 에 쓰는 유일한 경로다:
        //   - WIP_JOB_SCHEDULE_INTERFACE_S / WIP_INTERFACE_S 시퀀스 채번
        //   - WIP_JOB_SCHEDULE_INTERFACE INSERT
        //   - FND_REQUEST.SUBMIT_REQUEST (WIP Mass Load)
        //  다시 쓰려면 아래 throw 를 지우고 블록 주석(/* ... */)을 해제한다.
        // ===================================================================================
        throw new IllegalStateException("조회 전용 모드입니다. EBS 작업지시 생성(DB 반영) 로직은 비활성화되어 있습니다.");
        /*
        if (props.getUserId() == null) {
            throw new IllegalStateException("planner.user-id(FND_USER.USER_ID)를 설정해야 작업지시를 생성할 수 있습니다.");
        }
        Set<String> ids = new LinkedHashSet<>(req.ids);
        if (ids.isEmpty()) throw new IllegalArgumentException("선택된 계획이 없습니다.");
        int statusType = req.statusType != null ? req.statusType : props.getWip().getStatusType();
        if (statusType != 1 && statusType != 3) throw new IllegalArgumentException("상태는 1(미해제) 또는 3(해제)만 가능합니다.");

        // 화면의 값 대신 서버에서 최신 EBS 데이터로 다시 계산한 계획을 사용
        PlanResult plan = planning.plan(req);
        List<PlannedJob> selected = new ArrayList<>();
        for (PlannedJob p : plan.planned) {
            if (ids.contains(p.id)) selected.add(p);
        }
        if (selected.size() != ids.size()) {
            throw new IllegalStateException("선택한 계획 중 일부가 최신 계획에 없습니다(이미 생성되었거나 수주가 변경됨). 재계산 후 다시 시도하세요.");
        }
        for (PlannedJob p : selected) {
            if (!p.releasable || p.qty <= 0) throw new IllegalStateException(p.id + " (" + p.itemCode + ") 은 시작일을 정할 수 없어 생성할 수 없습니다.");
        }

        String classCode = blank(req.classCode) ? props.getWip().getClassCode() : req.classCode.trim();
        if (blank(classCode)) classCode = plan.org.defaultClass;

        ReleaseResult result = new ReleaseResult();
        result.groupId = wip.nextGroupId();
        Map<Long, LocalDate> completion = new HashMap<>();
        for (PlannedJob p : selected) {
            JobRow row = new JobRow();
            row.groupId = result.groupId;
            row.interfaceId = wip.nextInterfaceId();
            row.orgId = plan.org.organizationId;
            row.itemId = p.itemId;
            row.qty = p.qty;
            row.statusType = statusType;
            row.schedulingMethod = props.getWip().getSchedulingMethod();
            row.start = p.start;
            row.completion = p.finish != null ? p.finish : p.start;
            row.classCode = classCode;
            row.sourceLineId = p.soLineId;
            row.description = truncate("SO " + p.orderNumber + "-" + p.lineRef + (p.customer == null ? "" : " " + p.customer), 240);
            wip.insert(row, props);
            completion.put(row.interfaceId, row.completion);
            result.count++;
        }
        if (props.getMassLoad().isSubmit()) {
            result.requestId = wip.submitMassLoad(result.groupId, props);
        }
        DemoMassLoad sim = demo.getIfAvailable();
        if (sim != null) {
            sim.process(result.groupId, completion);
            result.demoProcessed = true;
        }
        return result;
        */
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
