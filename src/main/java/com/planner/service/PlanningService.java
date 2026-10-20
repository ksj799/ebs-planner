package com.planner.service;

import com.planner.config.PlannerProperties;
import com.planner.model.Model.Day;
import com.planner.model.Model.Demand;
import com.planner.model.Model.ItemRouting;
import com.planner.model.Model.MasterData;
import com.planner.model.Model.Org;
import com.planner.model.Model.Adjust;
import com.planner.model.Model.PlanRequest;
import com.planner.model.Model.PlanResult;
import com.planner.model.Model.PlannedJob;
import com.planner.model.Model.Resource;
import com.planner.model.Model.RoutingOp;
import com.planner.model.Model.Supply;
import com.planner.repo.EbsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import static com.planner.model.Model.key;

/**
 * 계획 엔진.
 * 1) 수주라인 수요를 납기순으로 현재고·기존 작업지시·처리대기 인터페이스와 순소요 계산(Lot-for-Lot)
 * 2) 부족분마다 계획 작업지시를 만들고, 1단계 BOM 구매부품의 가용일(현재고·PO 입고예정·리드타임)을 구해
 *    그 이후로만 시작하게 한다 (MaterialService).
 * 3) 품목의 병목 자원(라우팅에서 개당 시간이 가장 큰 부서-자원)에 기존 작업지시 부하를 먼저 올린 뒤
 *    남는 능력으로 납기순 전진 유한능력 일정을 잡는다.
 */
@Service
public class PlanningService {
    private static final double EPS = 1e-9;
    private final EbsRepository repo;
    private final MaterialService material;
    private final PlannerProperties props;

    public PlanningService(EbsRepository repo, MaterialService material, PlannerProperties props) {
        this.repo = repo;
        this.material = material;
        this.props = props;
    }

    public Map<String, Resource> loadResources(Org org, LocalDate today) {
        Map<String, Resource> res = new LinkedHashMap<>();
        for (Resource r : repo.resources(org, today)) {
            if (props.getPlanning().hoursFactor(r.uom) == null) continue; // 시간 단위가 아닌 자원 제외
            if (r.available24h) r.hoursPerDay = 24 * r.capacityUnits;
            else if (r.shiftUnitHours > 0) r.hoursPerDay = r.shiftUnitHours;
            else r.hoursPerDay = props.getPlanning().getDefaultHoursPerDay() * r.capacityUnits;
            res.put(r.key, r);
        }
        return res;
    }

    public Map<Long, ItemRouting> loadRoutings(long orgId, LocalDate today, Map<String, Resource> res, Collection<Long> itemIds) {
        Map<Long, List<RoutingOp>> byItem = new LinkedHashMap<>();
        for (RoutingOp op : repo.routingOps(orgId, today, itemIds)) {
            byItem.computeIfAbsent(op.itemId, k -> new ArrayList<>()).add(op);
        }
        Map<Long, ItemRouting> out = new LinkedHashMap<>();
        for (Map.Entry<Long, List<RoutingOp>> e : byItem.entrySet()) {
            Map<String, double[]> perRes = new LinkedHashMap<>(); // [개당시간, 셋업시간]
            RoutingOp first = e.getValue().get(0);
            for (RoutingOp op : e.getValue()) {
                Double f = props.getPlanning().hoursFactor(op.resourceUom);
                if (f == null) continue;
                double[] v = perRes.computeIfAbsent(key(op.departmentId, op.resourceId), k -> new double[2]);
                if (op.basisType == 2) v[1] += op.usage * f;
                else v[0] += op.usage * f;
            }
            String best = null;
            for (Map.Entry<String, double[]> r : perRes.entrySet()) {
                if (!res.containsKey(r.getKey()) || r.getValue()[0] <= 0) continue;
                if (best == null || r.getValue()[0] > perRes.get(best)[0]) best = r.getKey();
            }
            if (best == null) continue;
            Resource r = res.get(best);
            ItemRouting ir = new ItemRouting();
            ir.itemId = e.getKey();
            ir.itemCode = first.itemCode;
            ir.itemDesc = first.itemDesc;
            ir.uom = first.uom;
            ir.resourceKey = best;
            ir.resourceCode = r.resourceCode;
            ir.departmentCode = r.departmentCode;
            ir.hoursPerUnit = perRes.get(best)[0];
            ir.setupHours = perRes.get(best)[1];
            ir.operationCount = e.getValue().size();
            ir.maxPerDay = r.hoursPerDay / ir.hoursPerUnit;
            out.put(ir.itemId, ir);
        }
        return out;
    }

    public PlanResult plan(PlanRequest req) {
        Org org = repo.org(req.orgId);
        LocalDate today = LocalDate.now();
        LocalDate start = req.start == null || req.start.isEmpty() ? today : LocalDate.parse(req.start);
        int days = Math.max(7, Math.min(req.days, props.getPlanning().getMaxDays()));
        LocalDate end = start.plusDays(days - 1);

        PlanResult r = new PlanResult();
        r.org = org;
        r.start = start;
        r.end = end;

        TreeSet<LocalDate> work = new TreeSet<>(repo.workdays(org, start.minusDays(180), end.plusDays(365)));
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) r.days.add(new Day(d, work.contains(d)));
        if (work.isEmpty()) {
            r.warnings.add("작업 캘린더 " + org.calendarCode + " 에 근무일이 없습니다. 캘린더 빌드 여부를 확인하세요.");
        }

        Map<String, Resource> res = loadResources(org, today);
        Map<String, Map<LocalDate, Double>> used = new HashMap<>();
        for (String k : res.keySet()) used.put(k, new HashMap<>());

        // ---- 1. 기존 공급과 순소요 ----
        List<Supply> supplies = new ArrayList<>(repo.openJobs(org.organizationId, props.getSourceCode()));
        supplies.addAll(repo.pendingInterface(org.organizationId, props.getSourceCode()));
        Map<Long, Double> pool = new HashMap<>(req.netOnhand ? repo.onhand(org.organizationId) : Collections.<Long, Double>emptyMap());
        for (Supply s : supplies) pool.merge(s.itemId, s.remainQty, Double::sum);

        List<Demand> demands = repo.demands(org.organizationId, end);
        // 라우팅은 계획에 나오는 품목(수요 + 기존 공급)만 읽는다
        Set<Long> itemIds = new LinkedHashSet<>();
        for (Demand d : demands) itemIds.add(d.itemId);
        for (Supply s : supplies) itemIds.add(s.itemId);
        Map<Long, ItemRouting> routings = loadRoutings(org.organizationId, today, res, itemIds);
        demands.sort(Comparator.comparing((Demand d) -> d.due).thenComparing(d -> d.orderNumber).thenComparing(d -> d.lineRef));
        List<PlannedJob> planned = new ArrayList<>();
        int uomMismatch = 0;
        for (Demand d : demands) {
            if (d.orderUom != null && !d.orderUom.equals(d.uom)) uomMismatch++;
            double avail = pool.getOrDefault(d.itemId, 0.0);
            d.coveredQty = Math.min(Math.max(avail, 0), d.openQty);
            pool.put(d.itemId, avail - d.coveredQty);
            d.shortQty = d.openQty - d.coveredQty;
            if (d.shortQty <= EPS) continue;

            PlannedJob p = new PlannedJob();
            p.id = "SO" + d.lineId;
            p.soLineId = d.lineId;
            p.orderNumber = d.orderNumber;
            p.lineRef = d.lineRef;
            p.customer = d.customer;
            p.itemId = d.itemId;
            p.itemCode = d.itemCode;
            p.itemDesc = d.itemDesc;
            p.uom = d.uom;
            p.suggestedQty = d.shortQty;
            p.qty = d.shortQty;
            p.due = d.due;
            Adjust ov = req.overrides == null ? null : req.overrides.get(p.id);
            if (ov != null && ov.qty != null && ov.qty > 0) p.qty = ov.qty;
            d.plannedId = p.id;
            planned.add(p);
        }
        if (uomMismatch > 0) {
            r.warnings.add("수주 단위가 기본 단위와 다른 라인 " + uomMismatch + "건은 단위 환산 없이 계산했습니다.");
        }

        // ---- 2. 기존 작업지시 부하 ----
        LocalDate firstWork = work.ceiling(start);
        for (Supply s : supplies) {
            ItemRouting ir = routings.get(s.itemId);
            if (ir == null) { s.note = "라우팅 없음"; continue; }
            Resource rs = res.get(ir.resourceKey);
            s.resourceKey = ir.resourceKey;
            LocalDate from = s.start == null || s.start.isBefore(start) ? start : s.start;
            List<LocalDate> span = new ArrayList<>();
            if (s.completion != null) {
                LocalDate to = s.completion.isBefore(from) ? from : s.completion;
                span.addAll(work.subSet(from, true, to, true));
                if (s.completion.isBefore(start)) s.note = "완료예정일 경과";
            } else {
                int n = (int) Math.max(1, Math.ceil(s.remainQty * ir.hoursPerUnit / Math.max(rs.hoursPerDay, EPS)));
                for (LocalDate d = work.ceiling(from); d != null && span.size() < n; d = work.higher(d)) span.add(d);
            }
            if (span.isEmpty()) {
                LocalDate d = work.ceiling(from);
                span.add(d != null ? d : (firstWork != null ? firstWork : start));
            }
            double qtyPerDay = s.remainQty / span.size();
            for (LocalDate d : span) {
                used.get(ir.resourceKey).merge(d, qtyPerDay * ir.hoursPerUnit, Double::sum);
                s.daily.merge(d.toString(), qtyPerDay, Double::sum);
            }
        }

        // ---- 3. 구매부품 수급 → 자재 가용일 ----
        planned.sort(Comparator.comparing((PlannedJob p) -> p.due).thenComparing(p -> p.id));
        Map<String, Integer> workDays = new HashMap<>();
        for (PlannedJob p : planned) {
            ItemRouting ir = routings.get(p.itemId);
            Resource rs = ir == null ? null : res.get(ir.resourceKey);
            int d = rs == null || rs.hoursPerDay <= 0 ? 1
                    : (int) Math.ceil((ir.setupHours + p.qty * ir.hoursPerUnit) / rs.hoursPerDay);
            workDays.put(p.id, d + 1); // 하루 여유
        }
        MaterialService.Result mat = material.plan(org, start, work, planned, req.overrides, workDays);
        r.materials = mat.rows;
        r.purchases = mat.purchases;
        long latePo = mat.purchases.stream().filter(p -> p.late).count();
        if (latePo > 0) r.warnings.add("리드타임 때문에 필요일까지 입고할 수 없는 구매 제안이 " + latePo + "건 있습니다.");

        // ---- 4. 계획 작업지시 유한능력 일정 ----
        for (PlannedJob p : planned) {
            Adjust ov = req.overrides == null ? null : req.overrides.get(p.id);
            LocalDate earliest = start;
            if (ov != null && ov.start != null && !ov.start.isEmpty()) earliest = LocalDate.parse(ov.start);
            if (p.materialReady != null && p.materialReady.isAfter(earliest)) earliest = p.materialReady;
            ItemRouting ir = routings.get(p.itemId);
            if (ir == null) {
                p.note = "주 라우팅/시간자원 없음 – 일정 계산 불가";
                p.start = work.ceiling(earliest);
                p.releasable = p.start != null;
                continue;
            }
            Resource rs = res.get(ir.resourceKey);
            p.resourceKey = ir.resourceKey;
            p.hours = ir.setupHours + p.qty * ir.hoursPerUnit;
            schedule(p, ir, rs, used.get(ir.resourceKey), work, earliest);
            p.releasable = p.start != null;
        }

        // ---- 5. 결과 정리 ----
        for (Resource rs : res.values()) {
            for (Map.Entry<LocalDate, Double> e : used.get(rs.key).entrySet()) {
                if (!e.getKey().isBefore(start) && !e.getKey().isAfter(end)) rs.load.put(e.getKey().toString(), e.getValue());
            }
            r.resources.add(rs);
        }
        r.existing = supplies;
        r.planned = planned;
        r.demands = demands;
        long late = planned.stream().filter(p -> p.late).count();
        if (late > 0) r.warnings.add("납기 지연이 예상되는 계획 작업지시가 " + late + "건 있습니다.");
        return r;
    }

    private void schedule(PlannedJob p, ItemRouting ir, Resource rs, Map<LocalDate, Double> used,
                          NavigableSet<LocalDate> work, LocalDate earliest) {
        double setupLeft = ir.setupHours;
        double remain = p.qty;
        for (LocalDate d = work.ceiling(earliest); d != null && remain > EPS; d = work.higher(d)) {
            double usedToday = used.getOrDefault(d, 0.0);
            double cap = rs.hoursPerDay - usedToday;
            if (cap <= EPS) continue;
            if (p.start == null) p.start = d;
            if (setupLeft > EPS) {
                double s = Math.min(setupLeft, cap);
                setupLeft -= s;
                cap -= s;
                used.merge(d, s, Double::sum);
                if (setupLeft > EPS || cap <= EPS) continue;
            }
            double q = Math.min(remain, Math.floor(cap / ir.hoursPerUnit + EPS));
            if (q <= 0) {
                // 하루 능력보다 개당 시간이 큰 경우: 빈 날에는 소수 단위로 진행
                if (usedToday > EPS) continue;
                q = Math.min(remain, cap / ir.hoursPerUnit);
            }
            used.merge(d, q * ir.hoursPerUnit, Double::sum);
            p.daily.merge(d.toString(), q, Double::sum);
            remain -= q;
            p.finish = d;
        }
        if (remain > EPS) {
            p.note = "계획 기간(+365일) 안에 완료할 수 없음 – 능력 부족";
            p.finish = null;
        }
        p.late = p.finish == null || p.finish.isAfter(p.due);
    }

    public MasterData master(long orgId) {
        MasterData m = new MasterData();
        m.org = repo.org(orgId);
        LocalDate today = LocalDate.now();
        Map<String, Resource> res = loadResources(m.org, today);
        m.resources.addAll(res.values());
        // 계획 대상 품목(90일 내 미출하 수주 + 미완료 작업지시)의 병목 자원만 표시
        Set<Long> itemIds = new LinkedHashSet<>();
        for (Demand d : repo.demands(orgId, today.plusDays(90))) itemIds.add(d.itemId);
        for (Supply s : repo.openJobs(orgId, props.getSourceCode())) itemIds.add(s.itemId);
        m.routings.addAll(loadRoutings(orgId, today, res, itemIds).values());
        TreeSet<LocalDate> work = new TreeSet<>(repo.workdays(m.org, today, today.plusDays(90)));
        for (LocalDate d = today; !d.isAfter(today.plusDays(90)); d = d.plusDays(1)) {
            if (!work.contains(d)) m.nonWorkDays.add(d);
        }
        return m;
    }
}
